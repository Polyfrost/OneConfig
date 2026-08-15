{ rust' }:
{ lib, ... }:
{
    projectRootFile = "flake.nix";

    programs = {
        rustfmt = {
            enable = true;
            edition = (builtins.fromTOML (builtins.readFile ./Cargo.toml)).workspace.package.edition;
        };
        nixfmt = {
            enable = true;
            strict = true;
            width = 100;
        };
        mdformat = {
            enable = true;
            settings = {
                end-of-line = "lf";
                number = false;
                wrap = 100;
            };
        };
    };

    settings = {
        global.excludes = [ ".jj" ];
        settings.formatter.mdformat.excludes = [ "README.md" ];
        formatter = {
            rustfmt = {
                command = lib.getExe' rust' "rustfmt";
                excludes = [ "database/entities/src/entities" ];
            };
            nixfmt.options = [
                "--indent"
                "4"
            ];
        };
    };
}
