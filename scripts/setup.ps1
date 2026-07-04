## KnotNote Local Build Setup Script
## Run in PowerShell:
##   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
##   .\setup.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$PROJECT_ROOT = (Get-Item $PSScriptRoot).Parent.FullName

Write-Host ""
Write-Host "=== KnotNote P0 Setup ===" -ForegroundColor Cyan
Write-Host ""

# ── 1. Check JDK 17 ─────────────────────────────────────────────────────────
Write-Host "[1/5] Checking JDK..." -ForegroundColor Yellow
$javaOk = $false
try {
    $javaVer = (java -version 2>&1) | Select-String "version" | Select-Object -First 1
    Write-Host "  Found: $javaVer" -ForegroundColor Green
    if ($javaVer -notmatch '"17|"21') {
        Write-Warning "JDK 17 or 21 recommended."
    } else {
        $javaOk = $true
    }
} catch {
    Write-Warning "Java not found in PATH."
}

if (-not $javaOk) {
    Write-Host ""
    Write-Host "  JDK 17 is required. Install options:" -ForegroundColor Yellow
    Write-Host "  1) Download Temurin 17: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Cyan
    Write-Host "     -> Windows x64 .msi installer recommended" -ForegroundColor Cyan
    Write-Host "  2) Or use winget (run in a new PowerShell as Admin):" -ForegroundColor Cyan
    Write-Host "       winget install EclipseAdoptium.Temurin.17.JDK" -ForegroundColor White
    Write-Host ""
    Write-Host "  After installing, restart PowerShell and re-run this script." -ForegroundColor Yellow
    Write-Host ""
    $cont = Read-Host "  Continue anyway? (y/N)"
    if ($cont -ne 'y' -and $cont -ne 'Y') { exit 0 }
}

# ── 2. Download gradle-wrapper.jar ──────────────────────────────────────────
Write-Host "[2/5] Checking gradle-wrapper.jar..." -ForegroundColor Yellow
$wrapperDir = Join-Path $PROJECT_ROOT "gradle\wrapper"
$wrapperJar = Join-Path $wrapperDir "gradle-wrapper.jar"

if (Test-Path $wrapperJar) {
    Write-Host "  gradle-wrapper.jar already exists." -ForegroundColor Green
} else {
    Write-Host "  Downloading gradle-wrapper.jar from GitHub..." -ForegroundColor Cyan
    $null = New-Item -ItemType Directory -Force -Path $wrapperDir
    $jarUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $jarUrl -OutFile $wrapperJar -UseBasicParsing
        Write-Host "  Download succeeded." -ForegroundColor Green
    } catch {
        Write-Warning "Download failed. Alternatives:"
        Write-Warning "  A) Open project in IntelliJ IDEA -> it auto-creates the wrapper"
        Write-Warning "  B) Run: gradle wrapper --gradle-version 8.5  (if Gradle is installed)"
    }
}

# ── 3. Generate .env.local ──────────────────────────────────────────────────
Write-Host "[3/5] Generating environment file..." -ForegroundColor Yellow
$envFile = Join-Path $PROJECT_ROOT ".env.local"
if (-not (Test-Path $envFile)) {
    $chars = (65..90) + (97..122) + (48..57)
    $secret = -join ($chars | Get-Random -Count 48 | ForEach-Object { [char]$_ })
    $content = @"
# KnotNote local environment variables
# Copy these into IntelliJ: Run Configuration -> Environment variables

DB_USERNAME=knotnote
DB_PASSWORD=knotnote1234!
JWT_SECRET=$secret
"@
    [System.IO.File]::WriteAllText($envFile, $content, [System.Text.Encoding]::UTF8)
    Write-Host "  .env.local created (JWT_SECRET auto-generated)." -ForegroundColor Green
} else {
    Write-Host "  .env.local already exists." -ForegroundColor Green
}
Write-Host "  >> Register the values in IntelliJ Run Configuration -> Environment variables." -ForegroundColor Magenta

# ── 4. MySQL setup reminder ─────────────────────────────────────────────────
Write-Host "[4/5] MySQL setup..." -ForegroundColor Yellow
Write-Host "  Run this SQL as root if not done yet:" -ForegroundColor Cyan
Write-Host "    mysql -u root -p < db\init.sql" -ForegroundColor White
Write-Host "  (Creates knotnote database + user knotnote / knotnote1234!)" -ForegroundColor Cyan

# ── 5. Gradle build ─────────────────────────────────────────────────────────
Write-Host "[5/5] Running Gradle build..." -ForegroundColor Yellow
if (-not (Test-Path $wrapperJar)) {
    Write-Warning "gradle-wrapper.jar not found. Skipping build. Fix step 2 first."
    exit 0
}

$gradlew = Join-Path $PROJECT_ROOT "gradlew.bat"
Push-Location $PROJECT_ROOT
try {
    Write-Host "  Running: gradlew.bat clean build" -ForegroundColor Cyan
    & $gradlew clean build --stacktrace
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "BUILD SUCCESS!" -ForegroundColor Green
        Write-Host "  Start server : .\gradlew.bat bootRun" -ForegroundColor Cyan
        Write-Host "  Swagger UI   : http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
    } else {
        Write-Host "BUILD FAILED. Check errors above." -ForegroundColor Red
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Cyan
