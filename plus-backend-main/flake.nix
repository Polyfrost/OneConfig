{
    inputs = {
        nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
        flake-utils.url = "github:numtide/flake-utils";
        rust-overlay = {
            url = "github:oxalica/rust-overlay";
            inputs.nixpkgs.follows = "nixpkgs";
        };
        crane.url = "github:ipetkov/crane";
        treefmt-nix = {
            url = "github:numtide/treefmt-nix";
            inputs.nixpkgs.follows = "nixpkgs";
        };
    };

    outputs =
        {
            self,
            nixpkgs,
            flake-utils,
            rust-overlay,
            crane,
            treefmt-nix,
            ...
        }:
        flake-utils.lib.eachDefaultSystem (
            system:
            let
                # Initialize nixpkgs
                pkgs = nixpkgs.legacyPackages.${system};
                inherit (pkgs) lib;
                # Setup the rust toolchain
                rust-bin = rust-overlay.lib.mkRustBin { } pkgs;
                rust' = (rust-bin.fromRustupToolchainFile ./rust-toolchain.toml);
                # Setup rust nix packaging
                craneLib = (crane.mkLib pkgs).overrideToolchain (_: rust');
                stdenvSelector =
                    p: if p.stdenv.hostPlatform.isElf then p.stdenvAdapters.useMoldLinker p.stdenv else p.stdenv;
                commonArgs = {
                    src = craneLib.cleanCargoSource ./.;
                    strictDeps = true;

                    # buildInputs = with pkgs; [
                    #     libpq
                    #     openssl
                    # ];

                    # nativeBuildInputs = with pkgs; [ pkg-config ];

                    # Use mold linker for faster builds on ELF platforms
                    stdenv = stdenvSelector;
                };
                cargoArtifacts = craneLib.buildDepsOnly commonArgs;
                commonArgsWithDeps = commonArgs // {
                    inherit cargoArtifacts;
                };
                cranePackage = craneLib.buildPackage (
                    commonArgsWithDeps
                    // {
                        meta = {
                            mainProgram = "plus-backend";
                            license = lib.licenses.gpl3Plus;
                        };
                    }
                );
                # The skinview3d cover render sidecar (render-service/). Pure-JS
                # runtime deps (puppeteer + fflate) installed from the committed
                # lockfile via importNpmLock (no vendored hash to maintain). The
                # skinview3d fork is pre-bundled into render-service/vendor, so the
                # Nix build never touches the git dependency or a JS build step.
                # Puppeteer's Chromium download is skipped; the nixpkgs chromium is
                # wired in at runtime instead.
                render-service = pkgs.buildNpmPackage {
                    pname = "plus-render-service";
                    version = "0.1.0";
                    src = lib.cleanSourceWith {
                        src = ./render-service;
                        # Keep the local node_modules out of the store; npm ci
                        # reinstalls from the lockfile in the build sandbox.
                        filter = path: _type: baseNameOf path != "node_modules";
                    };
                    npmDeps = pkgs.importNpmLock { npmRoot = ./render-service; };
                    inherit (pkgs.importNpmLock) npmConfigHook;
                    dontNpmBuild = true;
                    # Skip Puppeteer's Chromium download during the build; the
                    # nixpkgs chromium is wired in at runtime below.
                    PUPPETEER_SKIP_DOWNLOAD = "1";
                    nativeBuildInputs = [ pkgs.makeWrapper ];
                    postInstall = ''
                        makeWrapper ${pkgs.nodejs}/bin/node $out/bin/plus-render-service \
                            --add-flags $out/lib/node_modules/plus-render-service/src/server.js \
                            --set PUPPETEER_EXECUTABLE_PATH ${pkgs.chromium}/bin/chromium
                    '';
                    meta.mainProgram = "plus-render-service";
                };
                # Setup treefmt-nix
                treefmtModule = import ./treefmt.nix { inherit rust'; };
                treefmtEval = treefmt-nix.lib.evalModule pkgs treefmtModule;
                # Utilities for testing locally
                start-dev-env = pkgs.writeShellApplication {
                    name = "start-dev-env";
                    text = builtins.readFile ./scripts/start-dev-env.sh;
                    runtimeInputs = with pkgs; [
                        rclone
                        curl
                        jq
                        postgresql
                    ];
                };
            in
            {
                packages = {
                    default = self.packages.${system}.plus-backend;
                    plus-backend = cranePackage;
                    inherit render-service;
                };
                apps.render-service = {
                    type = "app";
                    program = "${render-service}/bin/plus-render-service";
                };
                formatter = treefmtEval.config.build.wrapper;
                devShells.default =
                    craneLib.devShell.override { mkShell = pkgs.mkShell.override { stdenv = stdenvSelector pkgs; }; }
                        {
                            # Add all build-time dependencies to the environment
                            packages =
                                cranePackage.buildInputs
                                ++ cranePackage.nativeBuildInputs
                                ++ [
                                    # Conveinience scripts for testing
                                    start-dev-env
                                ]
                                ++ (with pkgs; [
                                    # External rust dev utilities
                                    sea-orm-cli
                                    cargo-deny
                                    cargo-udeps
                                    cargo-nextest
                                    # Rust repl for testing
                                    evcxr
                                    # Debugging
                                    lldb
                                    postgresql
                                    # Add treefmt wrapper to the PATH for ease of use
                                    self.formatter.${system}
                                    # s3
                                    s3cmd
                                    # Node runtime for the skinview3d cover render sidecar
                                    # (render-service/). It is installed and run via npm;
                                    # Puppeteer provides its own headless Chromium.
                                    nodejs
                                ]);

                            env = {
                                MIGRATION_DIR = "./database/migrations";
                            };
                        };
            }
        );
}
