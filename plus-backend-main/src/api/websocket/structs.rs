use std::{borrow::Cow, collections::HashMap};

use entities::sea_orm_active_enums::BodySlot;
use schemars::{JsonSchema, json_schema};
use serde::{Deserialize, Serialize, ser::SerializeStruct as _};
use uuid::Uuid;

#[derive(Debug, thiserror::Error)]
pub enum WebsocketError {
	#[error("A fatal websocket connection error")]
	Fatal(#[from] axum::Error),
	#[error("Unable to query database: {0}")]
	DatabaseQuery(#[from] sea_orm::error::DbErr),
	#[error("Unable to serialize response: {0}")]
	Serialization(#[from] serde_json::Error),
	#[error("Unable to parse request: {0}")]
	Deserialization(serde_json::Error),
	#[error("Player does not own cosmetic {0}")]
	UnownedCosmetic(i32),
	#[error("Cosmetic {cosmetic_id} is not allowed in slot {slot:?}")]
	InvalidSlot { slot: BodySlot, cosmetic_id: i32 },
	#[error("Player does not own emote {0}")]
	UnownedEmote(i32),
	#[error("Too many players in one request (max {limit})")]
	TooManyPlayersInRequest { limit: usize },
}

impl WebsocketError {
	const ERROR_CODES: &[&str] =
		&["fatal", "internal_server_error", "bad_request", "not_owned"];

	pub fn error_code(&self) -> &'static str {
		match self {
			Self::Fatal(_) => Self::ERROR_CODES[0],
			Self::DatabaseQuery(_) | Self::Serialization(_) => Self::ERROR_CODES[1],
			Self::Deserialization(_)
			| Self::InvalidSlot { .. }
			| Self::TooManyPlayersInRequest { .. } => Self::ERROR_CODES[2],
			Self::UnownedCosmetic(_) | Self::UnownedEmote(_) => Self::ERROR_CODES[3],
		}
	}
}

impl Serialize for WebsocketError {
	fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
	where
		S: serde::Serializer,
	{
		let mut state = serializer.serialize_struct("WebsocketError", 2)?;
		state.serialize_field("error_code", self.error_code())?;
		state.serialize_field("message", &self.to_string())?;
		state.end()
	}
}

impl JsonSchema for WebsocketError {
	fn schema_name() -> Cow<'static, str> {
		Cow::Borrowed("WebsocketError")
	}

	fn json_schema(_: &mut schemars::SchemaGenerator) -> schemars::Schema {
		json_schema!({
			"type": "object",
			"properties": {
				"error_code": {
					"enum": WebsocketError::ERROR_CODES,
					"description": "The machine-readable unique error code",
					"example": "internal_server_error"
				},
				"message": {
					"type": "string",
					"description": "The human-readable error message"
				}
			}
		})
	}
}

/// A JSON object that a client can send in the websocket connection
#[derive(Debug, Deserialize, JsonSchema)]
#[serde(rename_all = "PascalCase", tag = "type")]
pub enum ServerBoundPacket {
	/// Fetches active cosmetics for a list of players in bulk
	GetActiveCosmetics {
		/// An array of player UUIDs to include in the bulk lookup
		players: Vec<Uuid>,
	},
	/// Subscribe to cosmetic/emote updates for nearby players (render distance).
	SubscribePlayers {
		/// Player UUIDs to watch. Capped per request and per connection total.
		players: Vec<Uuid>,
		#[serde(default)]
		request_id: Option<u64>,
	},
	UnsubscribePlayers {
		players: Vec<Uuid>,
	},
	SetEquippedCosmetic {
		slot: BodySlot,
		cosmetic_id: Option<i32>,
	},
	SetParticleColor {
		color: Option<i32>,
	},
	PlayEmote {
		emote_id: i32,
	},
	StopEmote,
}

/// A JSON object that the server will send to the client in the websocket
/// connection
#[derive(Debug, Serialize, JsonSchema)]
#[serde(rename_all = "PascalCase", tag = "type")]
pub enum ClientBoundPacket {
	/// Information on player UUIDs and what cosmetics they own, sent in
	/// response to [ServerBoundPacket::GetActiveCosmetics]
	CosmeticsInfo {
		/// An object mapping player UUIDs to a list of their active cosmetic
		/// IDs. These cosmetic IDs should be resolved seperately, as no other
		/// information is given in this response.
		///
		/// Any players without active cosmetics are not returned in this
		/// object.
		#[schemars(example = HashMap::from([("424ef6d0-4774-4f8c-8bef-8f62ebdac9c0", [1,2])]))]
		cosmetics: HashMap<Uuid, Vec<i32>>,
	},
	SubscriptionSnapshot {
		equipped: HashMap<Uuid, HashMap<BodySlot, i32>>,
		active_emotes: HashMap<Uuid, i32>,
		particle_colors: HashMap<Uuid, i32>,
		/// The subset of subscribed players that currently have a live PolyPlus
		/// session connected. Used to render a "uses PolyPlus" indicator.
		users: Vec<Uuid>,
		rejected: Vec<Uuid>,
		request_id: Option<u64>,
	},
	/// A subscribed player's PolyPlus session came online or went offline.
	PlayerPresence {
		player: Uuid,
		online: bool,
	},
	PlayerCosmeticEquipped {
		player: Uuid,
		slot: BodySlot,
		cosmetic_id: Option<i32>,
	},
	PlayerParticleColorChanged {
		player: Uuid,
		color: Option<i32>,
	},
	PlayerEmoteStarted {
		player: Uuid,
		emote_id: i32,
	},
	PlayerEmoteStopped {
		player: Uuid,
	},
	OwnershipUpdated {
		player: Uuid,
		cosmetic_ids: Vec<i32>,
		emote_ids: Vec<i32>,
		/// are we revoking stuff
		revoked: bool,
	},
	/// An error response from the server
	Error {
		#[serde(flatten)]
		error: WebsocketError,
		request_id: Option<u64>,
	},
}

impl ServerBoundPacket {
	pub fn request_id(&self) -> Option<u64> {
		match self {
			Self::SubscribePlayers { request_id, .. } => *request_id,
			_ => None,
		}
	}
}

#[cfg(test)]
mod tests {
	use std::collections::HashMap;

	use entities::sea_orm_active_enums::BodySlot;
	use uuid::Uuid;

	use super::{ClientBoundPacket, ServerBoundPacket};

	#[test]
	fn parses_slot_based_equipment_update() {
		let packet: ServerBoundPacket = serde_json::from_str(
			r#"{"type":"SetEquippedCosmetic","slot":"cape","cosmetic_id":42}"#,
		)
		.expect("packet should parse");

		match packet {
			ServerBoundPacket::SetEquippedCosmetic { slot, cosmetic_id } => {
				assert_eq!(slot, BodySlot::Cape);
				assert_eq!(cosmetic_id, Some(42));
			}
			_ => panic!("unexpected packet variant"),
		}
	}

	#[test]
	fn parses_player_subscription() {
		let player = Uuid::nil();
		let packet: ServerBoundPacket = serde_json::from_str(&format!(
			r#"{{"type":"SubscribePlayers","players":["{player}"]}}"#
		))
		.expect("packet should parse");

		match packet {
			ServerBoundPacket::SubscribePlayers {
				players,
				request_id,
			} => {
				assert_eq!(players, vec![player]);
				assert_eq!(request_id, None);
			}
			_ => panic!("unexpected packet variant"),
		}
	}

	#[test]
	fn parses_player_subscription_with_request_id() {
		let player = Uuid::nil();
		let packet: ServerBoundPacket = serde_json::from_str(&format!(
			r#"{{"type":"SubscribePlayers","players":["{player}"],"request_id":7}}"#
		))
		.expect("packet should parse");

		assert_eq!(packet.request_id(), Some(7));
	}

	#[test]
	fn serializes_subscription_snapshot_packet() {
		let player = Uuid::nil();
		let packet = ClientBoundPacket::SubscriptionSnapshot {
			equipped: HashMap::from([(player, HashMap::from([(BodySlot::Cape, 1)]))]),
			active_emotes: HashMap::from([(player, 6)]),
			particle_colors: HashMap::from([(player, 0xFF_0000)]),
			users: vec![player],
			rejected: Vec::new(),
			request_id: Some(3),
		};

		let serialized = serde_json::to_value(packet).expect("packet should serialize");
		assert_eq!(serialized["type"], "SubscriptionSnapshot");
		assert_eq!(serialized["equipped"][player.to_string()]["cape"], 1);
		assert_eq!(serialized["users"][0], player.to_string());
		assert_eq!(serialized["rejected"].as_array().map(Vec::len), Some(0));
		assert_eq!(serialized["request_id"], 3);
	}

	#[test]
	fn serializes_error_packet_with_request_id() {
		let packet = ClientBoundPacket::Error {
			error: super::WebsocketError::TooManyPlayersInRequest { limit: 64 },
			request_id: Some(9),
		};

		let serialized = serde_json::to_value(packet).expect("packet should serialize");
		assert_eq!(serialized["type"], "Error");
		assert_eq!(serialized["error_code"], "bad_request");
		assert_eq!(serialized["request_id"], 9);
	}

	#[test]
	fn serializes_player_presence_packet() {
		let player = Uuid::nil();
		let packet = ClientBoundPacket::PlayerPresence {
			player,
			online: true,
		};

		let serialized = serde_json::to_value(packet).expect("packet should serialize");
		assert_eq!(serialized["type"], "PlayerPresence");
		assert_eq!(serialized["player"], player.to_string());
		assert_eq!(serialized["online"], true);
	}
}
