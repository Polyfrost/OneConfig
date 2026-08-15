# Polyfrost Backend v1

The rust-based backend for polyfrost's software, primarily used for update-checking and downloading

## Usage

```
The main command that starts the backend HTTP server. The server can be configured either with flags or environment variables, listed in the help message

Usage: backend [OPTIONS] --public-maven-url <PUBLIC_MAVEN_URL>

Options:
      --bind <BIND>
          The addresses (IP and port) for the HTTP server to bind to [env: BACKEND_LISTEN_PORT=] [default: 0.0.0.0:8080,[::]:8080]
      --http1
          If passed, the server will be downgraded to HTTP/1.1 rather than HTTP/2 [env: BACKEND_USE_HTTP1=]
      --public-maven-url <PUBLIC_MAVEN_URL>
          Sets the maven root server url that will be advertised for public downloads through the API [env: BACKEND_PUBLIC_MAVEN_URL=]
      --internal-maven-url <INTERNAL_MAVEN_URL>
          If set, the maven root server url that will be used for maven requests (such as checksum requests), but not publicly advertised via the API. If unset, defaults to the public maven url. If maven is running on the same host as this backend, then this can be set to a local IP to greatly speed up requests [env: BACKEND_INTERNAL_MAVEN_URL=]
  -h, --help
          Print help
  -V, --version
          Print version
```
