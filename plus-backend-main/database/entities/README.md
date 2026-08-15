# Database entities

This crate contains the generated database entity structs for SeaORM.

## How to generate

1. Ensure you have a PostgreSQL server running (preferably with an empty database)
1. Set the `DATABASE_URL` env variable to the database to use (it will be wiped!)
1. Use `sea-orm-cli migrate fresh` to initialize the database schema
1. Delete the existing generated code (`rm -r ./database/entities/src/entities`)
1. Use `sea-orm-cli generate entity -o ./database/entities/src/entities` to generate the code
