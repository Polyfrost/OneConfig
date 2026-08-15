mod cover;
mod get_player;
mod grant;
mod list;
mod list_capes;
mod manage;
mod put_player;
mod search;
mod view;

use std::{
	collections::HashMap,
	io::{Cursor, Read, Write},
	sync::Arc,
};

use aide::axum::ApiRouter;
use entities::sea_orm_active_enums::{BodySlot, CosmeticType};
use moka::future::Cache;
use s3::Bucket;
use schemars::JsonSchema;
use sea_orm::{DatabaseConnection, DbErr, EntityTrait};
use serde::{Deserialize, Serialize};

use crate::api::ApiState;

/// How long a presigned object url stays valid. Kept above the asset cache's own
/// time-to-live so a cached url never outlives the signature inside it.
const PRESIGN_EXPIRY_SECS: u32 = 86_400;

/// The parts of an [`entities::asset::Model`] that responses need on every
/// request, resolved once and then kept in the api state's asset cache.
#[derive(Clone, Debug)]
pub(super) struct CachedAssetInfo {
	/// The direct url the asset can be fetched from: the asset's own url when it
	/// has one, a presigned object url when it lives in S3.
	pub(super) url: Option<String>,
	/// The content hash recorded when the asset was uploaded, if any.
	pub(super) hash: Option<String>,
}

impl CachedAssetInfo {
	/// Resolves an asset's url and hash. Presigning is a local signature over the
	/// object path, so this makes no network request.
	pub(super) async fn from_db_model(
		asset: &entities::asset::Model,
		s3_bucket: Arc<Bucket>,
	) -> Result<Self, s3::error::S3Error> {
		let url = match (&asset.url, &asset.storage_path) {
			(Some(url), _) => Some(url.clone()),
			(None, Some(path)) => Some(
				s3_bucket
					.presign_get(path, PRESIGN_EXPIRY_SECS, None)
					.await?,
			),
			(None, None) => None,
		};

		Ok(Self {
			url,
			hash: asset.hash.clone(),
		})
	}
}

/// Reads an asset's info from the cache, resolving and caching it on a miss.
async fn cached_asset_info(
	asset: &entities::asset::Model,
	asset_cache: &Cache<i32, CachedAssetInfo>,
	s3_bucket: &Arc<Bucket>,
) -> Result<CachedAssetInfo, s3::error::S3Error> {
	if let Some(info) = asset_cache.get(&asset.id).await {
		return Ok(info);
	}

	let info = CachedAssetInfo::from_db_model(asset, s3_bucket.clone()).await?;
	asset_cache.insert(asset.id, info.clone()).await;

	Ok(info)
}

/// One selectable variant of a cosmetic.
#[derive(Debug, Serialize, JsonSchema)]
pub(super) struct VariantInfo {
	pub(super) id: i32,
	/// The variant's own label within its group, falling back to the cosmetic's
	/// name and then to `Variant {id}`.
	pub(super) name: String,
	/// The skin model this variant targets (`slim`/`wide`), when it only applies
	/// to one.
	pub(super) model: Option<String>,
	/// A direct url for the variant's asset. Only filled in by endpoints that
	/// presign; resolve `asset_id` through `/asset/{id}` otherwise.
	pub(super) url: Option<String>,
	pub(super) asset_id: Option<i32>,
	pub(super) cover_asset_id: Option<i32>,
	/// The content hash of the variant's asset, empty when it has none.
	pub(super) hash: String,
}

/// A buyable cosmetic. A grouped cosmetic collapses into a single entry whose
/// `variants` are the swatches the player picks between; an ungrouped one has
/// exactly one variant.
#[derive(Debug, Serialize, JsonSchema)]
pub(super) struct CosmeticInfo {
	/// The representative variant's id, not the group's. Pass this to
	/// `/cosmetics/view/{id}`.
	pub(super) id: i32,
	pub(super) r#type: CosmeticType,
	/// The group's name for a grouped cosmetic, the cosmetic's own name
	/// otherwise.
	pub(super) name: String,
	pub(super) allowed_slots: Vec<BodySlot>,
	/// Every variant of this cosmetic, ordered by `variant_order` then id.
	pub(super) variants: Vec<VariantInfo>,
}

/// An emote a player owns. Emotes are bundles rather than images, so they carry
/// no body slots and never group into variants.
#[derive(Debug, Serialize, JsonSchema)]
pub(super) struct EmoteInfo {
	pub(super) id: i32,
	pub(super) name: String,
	pub(super) asset_id: Option<i32>,
	/// A direct url for the emote bundle, when its asset resolves to one.
	pub(super) url: Option<String>,
	/// The content hash of the emote bundle, empty when it has none.
	pub(super) hash: String,
}

impl EmoteInfo {
	pub(super) async fn from_db_model(
		cosmetic: &entities::cosmetic::Model,
		asset: Option<&entities::asset::Model>,
		asset_cache: Cache<i32, CachedAssetInfo>,
		s3_bucket: Arc<Bucket>,
	) -> Result<Self, s3::error::S3Error> {
		let info = match asset {
			Some(asset) => Some(cached_asset_info(asset, &asset_cache, &s3_bucket).await?),
			None => None,
		};

		Ok(Self {
			id: cosmetic.id,
			name: cosmetic
				.name
				.clone()
				.unwrap_or_else(|| format!("Emote {}", cosmetic.id)),
			asset_id: cosmetic.asset_id,
			url: info.as_ref().and_then(|info| info.url.clone()),
			hash: info.and_then(|info| info.hash).unwrap_or_default(),
		})
	}
}

/// The cosmetic equipped in each body slot, serialized as a flat slot-keyed
/// object. Slots with nothing equipped are absent.
#[derive(Debug, Default, Serialize, JsonSchema)]
pub(super) struct EquippedCosmetics {
	#[serde(flatten)]
	pub(super) equipped: HashMap<BodySlot, i32>,
}

impl Extend<(BodySlot, i32)> for EquippedCosmetics {
	fn extend<T: IntoIterator<Item = (BodySlot, i32)>>(&mut self, iter: T) {
		self.equipped.extend(iter);
	}
}

/// Slot-keyed equipment updates. A null value clears that slot; slots left out
/// of the object are untouched.
#[derive(Debug, Deserialize, JsonSchema)]
pub(super) struct PartialEquippedCosmetics {
	#[serde(flatten)]
	pub(super) equipped: HashMap<BodySlot, Option<i32>>,
}

/// Every enabled cosmetic group, keyed by id, with the body slots it allows.
pub(super) type CosmeticGroups =
	HashMap<i32, (entities::cosmetic_group::Model, Vec<BodySlot>)>;

/// Loads the groups needed to collapse a set of cosmetics into grouped entries.
pub(super) async fn load_groups(db: &DatabaseConnection) -> Result<CosmeticGroups, DbErr> {
	use entities::{cosmetic_group, prelude::*};
	use sea_orm::{ColumnTrait, QueryFilter};

	let mut groups = CosmeticGroups::new();
	for (group, slots) in CosmeticGroup::find()
		.filter(cosmetic_group::Column::Enabled.eq(true))
		.find_with_related(CosmeticGroupAllowedSlot)
		.all(db)
		.await?
	{
		let slots = slots.into_iter().map(|allowed| allowed.slot).collect();
		groups.insert(group.id, (group, slots));
	}

	Ok(groups)
}

/// Whether a variant label carries no choice for the player, and so should not
/// be listed as its own swatch.
///
/// A *missing* label is not redundant: variants that differ only by skin model
/// carry none, and dropping those would empty their group entirely.
pub(super) fn is_redundant_variant(variant_name: Option<&str>) -> bool {
	variant_name.is_some_and(|name| {
		let name = name.trim();
		name.is_empty()
			|| name.eq_ignore_ascii_case("default")
			|| name.eq_ignore_ascii_case("base")
			|| name.eq_ignore_ascii_case("none")
	})
}

/// A cosmetic row as the listing endpoints load it: the cosmetic, its asset, its
/// cover asset, and the body slots it may be equipped in.
type CosmeticRow = (
	entities::cosmetic::Model,
	Option<entities::asset::Model>,
	Option<entities::asset::Model>,
	Vec<BodySlot>,
);

/// Identifies the entry a cosmetic row collapses into: its group, or itself when
/// it has none.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
enum BucketKey {
	Group(i32),
	Solo(i32),
}

/// Collapses cosmetic rows into buyable entries: every variant of a group folds
/// into one [`CosmeticInfo`], and an ungrouped cosmetic stands alone.
///
/// Entries come back in the order their first row appeared, so callers keep
/// whatever ordering they queried with. `presign` fills each variant's `url`
/// with a direct object url; leave it off for endpoints whose clients resolve
/// assets through `/asset/{id}` instead.
pub(super) async fn group_cosmetics(
	rows: Vec<CosmeticRow>,
	groups: CosmeticGroups,
	asset_cache: Cache<i32, CachedAssetInfo>,
	s3_bucket: Arc<Bucket>,
	presign: bool,
) -> Result<Vec<CosmeticInfo>, s3::error::S3Error> {
	let mut order: Vec<BucketKey> = Vec::new();
	let mut buckets: HashMap<BucketKey, Vec<CosmeticRow>> = HashMap::new();

	for row in rows {
		let key = match row.0.group_id {
			Some(group_id) => BucketKey::Group(group_id),
			None => BucketKey::Solo(row.0.id),
		};

		buckets
			.entry(key)
			.or_insert_with(|| {
				order.push(key);
				Vec::new()
			})
			.push(row);
	}

	let mut infos = Vec::with_capacity(order.len());
	for key in order {
		let Some(mut members) = buckets.remove(&key) else {
			continue;
		};

		// The representative is the lowest `variant_order`, ties broken by id, so
		// the same variant stands for the group however the rows were queried.
		members.sort_by_key(|(cosmetic, ..)| (cosmetic.variant_order, cosmetic.id));

		let Some((representative, _, _, representative_slots)) = members.first() else {
			continue;
		};

		let group = match key {
			BucketKey::Group(group_id) => groups.get(&group_id),
			BucketKey::Solo(_) => None,
		};

		let name = match group {
			Some((group, _)) => group.name.clone(),
			None => representative
				.name
				.clone()
				.unwrap_or_else(|| format!("Cosmetic {}", representative.id)),
		};

		// A group declares its own slots; fall back to the representative's when
		// it declares none, so a half-configured group still equips somewhere.
		let allowed_slots = match group {
			Some((_, slots)) if !slots.is_empty() => slots.clone(),
			_ => representative_slots.clone(),
		};

		let id = representative.id;
		let cosmetic_type = representative.r#type.clone();

		let mut variants = Vec::with_capacity(members.len());
		for (cosmetic, asset, cover_asset, _) in &members {
			let info = match asset {
				Some(asset) => {
					Some(cached_asset_info(asset, &asset_cache, &s3_bucket).await?)
				}
				None => None,
			};

			variants.push(VariantInfo {
				id: cosmetic.id,
				name: cosmetic
					.variant_name
					.clone()
					.or_else(|| cosmetic.name.clone())
					.unwrap_or_else(|| format!("Variant {}", cosmetic.id)),
				model: cosmetic.model_variant.clone(),
				url: presign
					.then(|| info.as_ref().and_then(|info| info.url.clone()))
					.flatten(),
				asset_id: cosmetic.asset_id,
				cover_asset_id: cover_asset
					.as_ref()
					.map(|cover| cover.id)
					.or(cosmetic.cover_asset_id),
				hash: info.and_then(|info| info.hash).unwrap_or_default(),
			});
		}

		infos.push(CosmeticInfo {
			id,
			r#type: cosmetic_type,
			name,
			allowed_slots,
			variants,
		});
	}

	Ok(infos)
}

/// Whether the uploaded bytes are a ZIP archive, which is how multi-file
/// cosmetics and every emote arrive.
pub(super) fn is_zip(data: &[u8]) -> bool {
	// Local file header, end of central directory, and the spanned-archive
	// marker a few zip writers emit first.
	data.starts_with(b"PK\x03\x04")
		|| data.starts_with(b"PK\x05\x06")
		|| data.starts_with(b"PK\x07\x08")
}

/// Whether a ZIP entry is macOS resource-fork noise rather than cosmetic
/// content.
fn is_macos_junk(path: &str) -> bool {
	path.split('/').any(|segment| segment == "__MACOSX")
		|| path.rsplit('/').next() == Some(".DS_Store")
}

/// Rewrites a ZIP bundle without the `__MACOSX` entries and `.DS_Store` files
/// that macOS adds when an admin zips a folder by hand.
pub(super) fn strip_macos_junk(data: &[u8]) -> Result<Vec<u8>, zip::result::ZipError> {
	use zip::{result::ZipError, write::SimpleFileOptions};

	let mut archive = zip::ZipArchive::new(Cursor::new(data))?;
	let mut output = Cursor::new(Vec::new());

	{
		let mut writer = zip::ZipWriter::new(&mut output);
		let options = SimpleFileOptions::default()
			.compression_method(zip::CompressionMethod::Deflated);

		for index in 0..archive.len() {
			let mut entry = archive.by_index(index)?;

			// Entries with a traversing or absolute path are dropped rather than
			// rewritten: nothing legitimate in a cosmetic bundle needs one.
			let Some(path) = entry.enclosed_name() else {
				continue;
			};
			let name = path.to_string_lossy().replace('\\', "/");

			if is_macos_junk(&name) {
				continue;
			}

			if entry.is_dir() {
				writer.add_directory(name, options)?;
				continue;
			}

			let mut contents = Vec::with_capacity(entry.size() as usize);
			entry.read_to_end(&mut contents).map_err(ZipError::Io)?;

			writer.start_file(name, options)?;
			writer.write_all(&contents).map_err(ZipError::Io)?;
		}

		writer.finish()?;
	}

	Ok(output.into_inner())
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new()
		// These carry their own absolute paths.
		.merge(list::router())
		.merge(list_capes::router())
		.merge(search::router())
		.merge(view::router())
		// These are relative, and would collide with `/player` on transactions
		// and `/create` on stripe if merged at the root.
		.nest(
			"/cosmetics",
			ApiRouter::new()
				.merge(get_player::router())
				.merge(put_player::router())
				.merge(grant::router())
				.merge(manage::router()),
		)
}

#[cfg(test)]
mod tests {
	use super::*;

	#[test]
	fn only_placeholder_variant_labels_are_redundant() {
		assert!(is_redundant_variant(Some("")));
		assert!(is_redundant_variant(Some("  ")));
		assert!(is_redundant_variant(Some("Default")));
		assert!(!is_redundant_variant(Some("Blue")));
		// A model-only variant has no label, and must survive to keep its group.
		assert!(!is_redundant_variant(None));
	}

	#[test]
	fn macos_junk_is_recognised_at_any_depth() {
		assert!(is_macos_junk("__MACOSX/textures/cape.png"));
		assert!(is_macos_junk("textures/__MACOSX/cape.png"));
		assert!(is_macos_junk("textures/.DS_Store"));
		assert!(!is_macos_junk("textures/cape.png"));
		assert!(!is_macos_junk("textures/DS_Store.png"));
	}

	#[test]
	fn zip_magic_is_detected() {
		assert!(is_zip(b"PK\x03\x04rest"));
		assert!(!is_zip(b"\x89PNG\r\n\x1a\n"));
		assert!(!is_zip(b"PK"));
	}
}
