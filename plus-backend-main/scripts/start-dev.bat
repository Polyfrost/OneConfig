@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0.."
pushd "%ROOT%" || (echo Could not cd to repo root & exit /b 1)

if not exist ".env" (
    echo No .env found in %ROOT% - copy .env.example to .env and fill it in first.
    popd
    exit /b 1
)

where rclone >nul 2>nul
if %errorlevel%==0 (
    set "RCLONE=rclone"
) else (
    set "RCLONE=%LOCALAPPDATA%\Microsoft\WinGet\Packages\Rclone.Rclone_Microsoft.Winget.Source_8wekyb3d8bbwe\rclone-v1.74.4-windows-amd64\rclone.exe"
)

if not exist "!RCLONE!" if not "!RCLONE!"=="rclone" (
    echo rclone not found. Install it with: winget install --id Rclone.Rclone -e
    popd
    exit /b 1
)

tasklist /fi "imagename eq rclone.exe" | findstr /i "rclone.exe" >nul
if %errorlevel%==0 (
    echo rclone S3 server already running, leaving it be.
) else (
    echo Starting local S3 server via rclone on 127.0.0.1:8081 ...
    if not exist ".local\s3\local" mkdir ".local\s3\local"
    start "plus-backend rclone s3" /min "!RCLONE!" serve s3 --addr 127.0.0.1:8081 --auth-key local,local "%ROOT%\.local\s3"
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" set "%%A=%%B"
)

echo Starting plus-backend...
cargo run serve

popd
endlocal
