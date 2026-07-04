## KnotNote - Start Spring Boot server
## Usage: .\run.ps1

# Load .env.local
$PROJECT_ROOT = (Get-Item $PSScriptRoot).Parent.FullName
$envFile = Join-Path $PROJECT_ROOT ".env.local"
if (-not (Test-Path $envFile)) {
    Write-Error ".env.local not found. Run .\setup.ps1 first."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.+)$') {
        $key   = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
        Write-Host "  SET $key" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "Starting KnotNote..." -ForegroundColor Cyan
Write-Host "  Swagger : http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host ""

& "$PROJECT_ROOT\gradlew.bat" bootRun
