mod api;

use std::net::SocketAddr;

use actix_web::{App, HttpServer, web};
use clap::Parser;
use url::Url;

use crate::api::common::data::ApiData;

/// The main command that starts the backend HTTP server. The server can be
/// configured either with flags or environment variables, listed in the help
/// message.
#[derive(Parser, Clone)]
#[clap(version, about, long_about = None)]
pub struct AppCommand {
	/// The addresses (IP and port) for the HTTP server to bind to.
	///
	/// Multiple bind addresses can be either split by comma, or passed as
	/// multiple flags.
	#[clap(
		long,
		env = "BACKEND_BIND_ADDRS",
		value_delimiter = ',',
		default_value = "[::]:8080"
	)]
	pub bind: Vec<SocketAddr>,
	/// If passed, the server will be downgraded to HTTP/1.1 rather than HTTP/2
	#[clap(long, env = "BACKEND_USE_HTTP1", default_value_t = false)]
	pub http1: bool,
	/// Sets the maven root server url that will be advertised for public
	/// downloads through the API.
	#[clap(long, env = "BACKEND_PUBLIC_MAVEN_URL")]
	pub public_maven_url: Url,
	/// If set, the maven root server url that will be used for maven requests
	/// (such as checksum requests), but not publicly advertised via the API. If
	/// unset, defaults to the public maven url. If maven is running on the
	/// same host as this backend, then this can be set to a local IP to
	/// greatly speed up requests.
	#[clap(long, env = "BACKEND_INTERNAL_MAVEN_URL")]
	pub internal_maven_url: Option<Url>,
}

#[tokio::main]
async fn main() {
	env_logger::init();
	rustls::crypto::ring::default_provider()
		.install_default()
		.expect("Failed to install rustls crypto provider");

	let args = AppCommand::parse();
	let data = web::Data::new(ApiData::new(&args));

	let mut server = HttpServer::new(move || {
		App::new()
			// Register app state
			.app_data(data.clone())
			// Register legacy routes and middleware
			.configure(api::legacy::configure)
			.wrap(actix_web::middleware::from_fn(api::legacy::metrics::middleware))
			// Register v1 routes and middleware
			.configure(api::v1::configure)
			// Register caching middleware
			.wrap(actix_web::middleware::from_fn(
				api::common::caching::middleware
			))
			// Register metrics routes and middleware
			.configure(api::common::metrics::configure)
			.wrap(actix_web::middleware::from_fn(
				api::common::metrics::middleware
			))
	});

	// Call .bind for each address, as using multiple in the same call can silently
	// fail
	for bind_addr in args.bind {
		server = server
			.bind_auto_h2c(bind_addr)
			.expect("Unable to bind on specified address");
	}

	server.run().await.expect("Unable to start HTTP server");
}
