{
  inputs = {
    uninix.url = "github:pauliesnug/uninix";
    flake-utils.follows = "uninix/flake-utils";
    nixpkgs.follows = "uninix/nixpkgs";
  };

  outputs = inputs: with inputs;
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ uninix.overlays.gradle ];
        };

        pkgSystem = pkgs.builder.gradle.makePackageSet {
          packageFn = import ./uni.demo.nix;
          targets = "auto";
        };
      in rec {
        packages = {
          oneconfig = (pkgSystem.workspace.minecraft.eachDefaultVersion {}).bin;
          default = packages.oneconfig;
        };
      }
    );
}