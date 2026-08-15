use std::{net::SocketAddr, str::FromStr};

use axum_client_ip::ClientIpSource;
use bpaf::Bpaf;
use http::{HeaderValue, header::InvalidHeaderValue};

#[derive(Clone, Debug, Bpaf)]
#[bpaf(options, version)]
pub(crate) struct BackendArgs {
	#[bpaf(external(self::subcommand))]
	pub(crate) command: Subcommand,
}

#[derive(Clone, Debug, Bpaf)]
pub(crate) enum Subcommand {
	#[bpaf(command("serve"))]
	Serve(#[bpaf(external(serve_args))] ServeArgs),
}

#[derive(Clone, Debug, Bpaf)]
pub(crate) struct ServeArgs {
	/// The socket addresses to bind the HTTP server to, comma seperated.
	/// If specified on the command line, multiple flags can be provided instead
	/// of passing a comma-delimited value.
	#[bpaf(
		long("bind-addr"),
		long("bind-address"),
		env("BIND_ADDR"),
		argument::<String>("ADDR"),
		parse(parse_bind_addrs),
		fallback_with(default_bind_addrs)
	)]
	pub(crate) bind_addr: Vec<SocketAddr>,
	/// The Stripe secret API key used to create checkout sessions
	#[bpaf(long("stripe-secret"), env("STRIPE_SECRET"))]
	pub(crate) stripe_secret: String,
	/// The Stripe webhook signing secret used to validate webhook signatures
	#[bpaf(long("stripe-webhook-secret"), env("STRIPE_WEBHOOK_SECRET"))]
	pub(crate) stripe_webhook_secret: String,
	/// The URL Stripe redirects the buyer to after a successful checkout
	#[bpaf(long("stripe-success-url"), env("STRIPE_SUCCESS_URL"))]
	pub(crate) stripe_success_url: String,
	/// The URL Stripe redirects the buyer to if they cancel checkout
	#[bpaf(long("stripe-cancel-url"), env("STRIPE_CANCEL_URL"))]
	pub(crate) stripe_cancel_url: String,
	/// The URL to use for connecting to the database
	#[bpaf(long("database-url"), env("DATABASE_URL"))]
	pub(crate) database_url: String,
	/// Where to source client IPs from. By default, parsed IPs will simply be
	/// the connecting remote IP address. However, other options like
	/// RightmostXForwardedFor can be passed to change this behavior. When set
	/// to anything except ConnectInfo, make sure that the API is run behind a
	/// TRUSTED reverse proxy, and is not exposed to the internet otherwise.
	/// See https://docs.rs/axum-client-ip/latest/axum_client_ip/enum.ClientIpSource.html for availible choices.
	#[bpaf(
		long("client-ip-source"),
		env("CLIENT_IP_SOURCE"),
		fallback(ClientIpSource::ConnectInfo)
	)]
	pub(crate) client_ip_source: ClientIpSource,
	/// The name of the s3 bucket to use
	#[bpaf(long("s3-bucket-name"), env("S3_BUCKET_NAME"))]
	pub(crate) s3_bucket_name: String,
	/// The region of the s3 bucket to use
	#[bpaf(long("s3-bucket-region"), env("S3_BUCKET_REGION"))]
	pub(crate) s3_bucket_region: String,
	/// The endpoint of the s3 bucket to use
	#[bpaf(long("s3-bucket-endpoint"), env("S3_BUCKET_ENDPOINT"))]
	pub(crate) s3_bucket_endpoint: String,
	/// Password for admin operations
	#[bpaf(long("admin-password"), env("ADMIN_PASSWORD"))]
	pub(crate) admin_password: String,
	#[bpaf(
		long("render-service-url"),
		env("RENDER_SERVICE_URL"),
		fallback(String::new())
	)]
	pub(crate) render_service_url: String,
	/// The origins allowed to make cross-origin requests to the API, comma
	/// seperated.
	#[bpaf(
		long("cors-origins"),
		env("CORS_ORIGINS"),
		argument::<String>("ORIGINS"),
		parse(parse_cors_origins),
		fallback_with(default_cors_origins)
	)]
	pub(crate) cors_origins: Vec<HeaderValue>,
}

fn parse_bind_addrs(value: String) -> Result<Vec<SocketAddr>, std::net::AddrParseError> {
	value
		.split(',')
		.map(str::trim)
		.filter(|addr| !addr.is_empty())
		.map(SocketAddr::from_str)
		.collect()
}

fn default_bind_addrs() -> Result<Vec<SocketAddr>, std::net::AddrParseError> {
	parse_bind_addrs("[::]:8080,0.0.0.0:8080".to_owned())
}

fn parse_cors_origins(value: String) -> Result<Vec<HeaderValue>, InvalidHeaderValue> {
	value
		.split(',')
		.map(str::trim)
		.filter(|origin| !origin.is_empty())
		.map(HeaderValue::from_str)
		.collect()
}

fn default_cors_origins() -> Result<Vec<HeaderValue>, InvalidHeaderValue> {
	parse_cors_origins(
		"https://plus-admin.polyfrost.org,http://localhost:3000".to_owned(),
	)
}
