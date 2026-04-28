## KnotNote - Start React frontend
## Usage: .\run-frontend.ps1

$frontendDir = Join-Path $PSScriptRoot "frontend"

if (-not (Test-Path $frontendDir)) {
    Write-Error "frontend/ directory not found."
    exit 1
}

# node_modules 없으면 install 먼저
if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
    Write-Host "Installing dependencies..." -ForegroundColor Cyan
    Push-Location $frontendDir
    npm install
    Pop-Location
}

Write-Host ""
Write-Host "Starting KnotNote Frontend..." -ForegroundColor Cyan
Write-Host "  App : http://localhost:3000" -ForegroundColor Cyan
Write-Host "  API proxy -> http://localhost:8080" -ForegroundColor DarkGray
Write-Host ""

Push-Location $frontendDir
npm run dev
Pop-Location
