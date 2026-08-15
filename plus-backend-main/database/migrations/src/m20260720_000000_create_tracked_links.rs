use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum TrackedLinks {
	Table,
	Slug,
	TargetUrl,
	/// Count of unique visitors (see [`TrackedLinkHits`]), not raw hits.
	Clicks,
	CreatedAt,
}

/// One row per (link, unique visitor). The visitor is identified by a salted
/// hash of ip + user-agent, so no raw IP is ever stored. Presence of a row is
/// what makes a subsequent visit non-unique.
#[derive(DeriveIden)]
pub enum TrackedLinkHits {
	Table,
	Slug,
	VisitorHash,
	CreatedAt,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(TrackedLinks::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(TrackedLinks::Slug)
							.text()
							.not_null()
							.primary_key(),
					)
					.col(ColumnDef::new(TrackedLinks::TargetUrl).text().not_null())
					.col(
						ColumnDef::new(TrackedLinks::Clicks)
							.big_integer()
							.not_null()
							.default(0),
					)
					.col(
						ColumnDef::new(TrackedLinks::CreatedAt)
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
					.table(TrackedLinkHits::Table)
					.if_not_exists()
					.col(ColumnDef::new(TrackedLinkHits::Slug).text().not_null())
					.col(
						ColumnDef::new(TrackedLinkHits::VisitorHash)
							.text()
							.not_null(),
					)
					.col(
						ColumnDef::new(TrackedLinkHits::CreatedAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.primary_key(
						Index::create()
							.col(TrackedLinkHits::Slug)
							.col(TrackedLinkHits::VisitorHash),
					)
					.foreign_key(
						ForeignKey::create()
							.from(TrackedLinkHits::Table, TrackedLinkHits::Slug)
							.to(TrackedLinks::Table, TrackedLinks::Slug)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(TrackedLinkHits::Table).to_owned())
			.await?;
		manager
			.drop_table(Table::drop().table(TrackedLinks::Table).to_owned())
			.await
	}
}
