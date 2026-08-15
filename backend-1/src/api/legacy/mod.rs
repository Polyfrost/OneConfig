use actix_web::web::ServiceConfig;

pub mod endpoint;
pub mod metrics;
pub mod types;
pub mod utils;

pub fn configure(config: &mut ServiceConfig) {
	config.service(endpoint::oneconfig);
	// Metrics middleware in main.rs due to github.com/actix/actix-web#414
}
