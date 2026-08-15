mod account;
mod admin_auth;
mod analytics;
mod assets;
mod bundles;
mod category;
mod collections;
mod cosmetics;
mod links;
mod players;
mod state;
mod stripe;
mod tags;
mod transactions;
mod websocket;

use std::{net::SocketAddr, sync::Arc};

use aide::{
	axum::routing::get,
	openapi::{OpenApi, SecurityScheme},
	scalar::Scalar,
	transform::TransformOpenApi,
};
use axum::{Extension, Json};

pub(crate) use state::ApiState;

use crate::api::account::OPENAPI_SECURITY_NAME;

pub(crate) async fn start(args: crate::commands::ServeArgs) {
	tracing::info!("Starting plus-backend server");
	let state = ApiState::new(&args).await;

	let cors = tower_http::cors::CorsLayer::new()
		.allow_origin(args.cors_origins)
		.allow_methods([
			http::Method::GET,
			http::Method::POST,
			http::Method::PUT,
			http::Method::PATCH,
			http::Method::DELETE,
		])
		// The headers have to be listed rather than wildcarded: the CORS spec
		// forbids `Access-Control-Allow-Headers: *` alongside
		// `Allow-Credentials: true`, and tower-http panics on the combination.
		// These are the ones clients actually send — the admin password and
		// bearer tokens, JSON and multipart uploads.
		.allow_headers([
			http::header::AUTHORIZATION,
			http::header::CONTENT_TYPE,
			http::header::ACCEPT,
		])
		.allow_credentials(true);

	let mut api = OpenApi::default();

	let app = setup_router()
		.await
		// Documentation, deliberately outside the documented surface itself.
		.route("/scalar", Scalar::new("/openapi.json").axum_route())
		.route("/openapi.json", get(serve_openapi))
		.finish_api_with(&mut api, api_docs)
		.layer(Extension(Arc::new(api)))
		// `ClientIp` reads this to decide which address to trust; without it
		// every extraction in `links` fails at runtime.
		.layer(args.client_ip_source.into_extension())
		.layer(cors)
		.with_state(state);

	// Serve on every address rather than only the first that binds. `localhost`
	// resolves to ::1 before 127.0.0.1 on Windows, so a client that asks for it
	// by name reaches an IPv4-only listener only by accident.
	let mut servers = Vec::new();
	for addr in &args.bind_addr {
		match tokio::net::TcpListener::bind(addr).await {
			Ok(listener) => {
				tracing::info!("Server listening on {addr}");
				// `ConnectInfo` is what `ClientIpSource::ConnectInfo` falls back
				// on, so each service has to carry the peer address.
				let service =
					app.clone().into_make_service_with_connect_info::<SocketAddr>();
				servers.push(tokio::spawn(async move {
					axum::serve(listener, service).await
				}));
			}
			// One address being taken is common and survivable; refusing to
			// start because the IPv6 half is unavailable would not be.
			Err(error) => tracing::warn!("Unable to bind {addr}: {error}"),
		}
	}

	assert!(
		!servers.is_empty(),
		"Failed to bind any of the requested addresses"
	);

	for server in servers {
		match server.await {
			Ok(Ok(())) => {}
			Ok(Err(error)) => tracing::error!("Server error: {error}"),
			Err(error) => tracing::error!("Server task panicked: {error}"),
		}
	}
}

/// Serves the generated OpenAPI document that `/scalar` renders.
async fn serve_openapi(Extension(api): Extension<Arc<OpenApi>>) -> Json<Arc<OpenApi>> {
	Json(api)
}

fn api_docs(api: TransformOpenApi) -> TransformOpenApi {
	api.title("PolyPlus Backend")
		.version(env!("CARGO_PKG_VERSION"))
		.summary("Cosmetics, bundles and playtime for Poly+.")
		.security_scheme(
			OPENAPI_SECURITY_NAME,
			SecurityScheme::Http {
				scheme: "bearer".into(),
				bearer_format: Some("PASETO".into()),
				description: Some(
					"A PASETO v4.local token, as returned by `/account/login`.".into(),
				),
				extensions: Default::default(),
			},
		)
}

pub(super) async fn setup_router() -> aide::axum::ApiRouter<ApiState> {
	aide::axum::ApiRouter::new()
		.merge(account::setup_router().await)
		.merge(analytics::setup_router().await)
		.merge(assets::setup_router().await)
		.merge(bundles::router())
		.merge(category::setup_router().await)
		.merge(collections::setup_router().await)
		.merge(cosmetics::router())
		.merge(links::setup_router().await)
		.merge(players::setup_router().await)
		.merge(stripe::setup_router().await)
		.merge(tags::setup_router().await)
		.merge(transactions::setup_router().await)
		.merge(websocket::setup_router().await)
}
