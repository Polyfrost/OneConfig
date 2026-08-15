use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

/// Existing `cosmetic` table (only the columns this migration touches).
#[derive(DeriveIden)]
enum Cosmetic {
	Table,
	GroupId,
	VariantName,
	ModelVariant,
	VariantOrder,
}

#[derive(DeriveIden)]
enum CosmeticGroup {
	Table,
	Id,
	Type,
	Name,
	Enabled,
	CreatedAt,
	UpdatedAt,
}

#[derive(DeriveIden)]
enum CosmeticGroupAllowedSlot {
	Table,
	GroupId,
	Slot,
}

const FK_COSMETIC_GROUP: &str = "fk_cosmetic_group_id";
const FK_GROUP_ALLOWED_SLOT: &str = "fk_cosmetic_group_allowed_slot_group_id";
const IDX_COSMETIC_GROUP_ID: &str = "cosmetic_group_id_idx";

const COSMETIC_TYPE_ENUM: &str = "cosmetic_type";
const BODY_SLOT_ENUM: &str = "body_slot";
const SHARED_ID_SEQ: &str = "cosmetic_entity_id_seq";

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(CosmeticGroup::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(CosmeticGroup::Id)
							.integer()
							.not_null()
							.primary_key()
							.extra(format!("DEFAULT nextval('{SHARED_ID_SEQ}')")),
					)
					.col(
						ColumnDef::new(CosmeticGroup::Type)
							.custom(Alias::new(COSMETIC_TYPE_ENUM))
							.not_null(),
					)
					.col(ColumnDef::new(CosmeticGroup::Name).text().not_null())
					.col(
						ColumnDef::new(CosmeticGroup::Enabled)
							.boolean()
							.not_null()
							.default(true),
					)
					.col(
						ColumnDef::new(CosmeticGroup::CreatedAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.col(
						ColumnDef::new(CosmeticGroup::UpdatedAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.to_owned(),
			)
			.await?;

		manager
			.create_table(
				Table::create()
					.table(CosmeticGroupAllowedSlot::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(CosmeticGroupAllowedSlot::GroupId)
							.integer()
							.not_null(),
					)
					.col(
						ColumnDef::new(CosmeticGroupAllowedSlot::Slot)
							.custom(Alias::new(BODY_SLOT_ENUM))
							.not_null(),
					)
					.primary_key(
						Index::create()
							.col(CosmeticGroupAllowedSlot::GroupId)
							.col(CosmeticGroupAllowedSlot::Slot),
					)
					.foreign_key(
						ForeignKey::create()
							.name(FK_GROUP_ALLOWED_SLOT)
							.from(
								CosmeticGroupAllowedSlot::Table,
								CosmeticGroupAllowedSlot::GroupId,
							)
							.to(CosmeticGroup::Table, CosmeticGroup::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.to_owned(),
			)
			.await?;

		manager
			.alter_table(
				Table::alter()
					.table(Cosmetic::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::GroupId).integer().null(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::VariantName).text().null(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::ModelVariant).text().null(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::VariantOrder)
							.integer()
							.not_null()
							.default(0),
					)
					.to_owned(),
			)
			.await?;

		manager
			.create_foreign_key(
				ForeignKey::create()
					.name(FK_COSMETIC_GROUP)
					.from(Cosmetic::Table, Cosmetic::GroupId)
					.to(CosmeticGroup::Table, CosmeticGroup::Id)
					.on_delete(ForeignKeyAction::SetNull)
					.to_owned(),
			)
			.await?;

		manager
			.create_index(
				Index::create()
					.if_not_exists()
					.name(IDX_COSMETIC_GROUP_ID)
					.table(Cosmetic::Table)
					.col(Cosmetic::GroupId)
					.to_owned(),
			)
			.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_index(
				Index::drop()
					.if_exists()
					.name(IDX_COSMETIC_GROUP_ID)
					.table(Cosmetic::Table)
					.to_owned(),
			)
			.await?;

		manager
			.alter_table(
				Table::alter()
					.table(Cosmetic::Table)
					.drop_foreign_key(Alias::new(FK_COSMETIC_GROUP))
					.drop_column(Cosmetic::GroupId)
					.drop_column(Cosmetic::VariantName)
					.drop_column(Cosmetic::ModelVariant)
					.drop_column(Cosmetic::VariantOrder)
					.to_owned(),
			)
			.await?;

		manager
			.drop_table(
				Table::drop()
					.table(CosmeticGroupAllowedSlot::Table)
					.to_owned(),
			)
			.await?;

		manager
			.drop_table(Table::drop().table(CosmeticGroup::Table).to_owned())
			.await?;

		Ok(())
	}
}
