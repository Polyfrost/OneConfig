set -euxo pipefail

sea-orm-cli migrate up -d "./database/migrations"
rm -r ./database/entities/src/entities
sea-orm-cli generate entity -o ./database/entities/src/entities \
    --enum-extra-derives schemars::JsonSchema,serde::Deserialize,serde::Serialize,Hash \
    --enum-extra-attributes 'serde(rename_all = "snake_case")'
