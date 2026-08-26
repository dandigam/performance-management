$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$secretFile = Join-Path $projectRoot ".local\application-secrets.properties"

if (-not (Test-Path -LiteralPath $secretFile)) {
    throw "Missing local secrets file: $secretFile"
}

$deploymentSecrets = ConvertFrom-StringData (Get-Content -LiteralPath $secretFile -Raw)
$env:DB_PASSWORD = $deploymentSecrets["spring.datasource.password"]
$env:JWT_SECRET = $deploymentSecrets["jwt.secret"]

if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "spring.datasource.password is missing from the local secrets file"
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET) -or $env:JWT_SECRET.Length -lt 32) {
    throw "jwt.secret must contain at least 32 characters"
}

$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenPath = if ($null -eq $mavenCommand) { $null } else { $mavenCommand.Source }
if ([string]::IsNullOrWhiteSpace($mavenPath)) {
    $mavenPath = Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists" -Recurse -Filter mvn.cmd |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if ([string]::IsNullOrWhiteSpace($mavenPath)) {
    throw "Maven was not found. Run a Maven build once or install Maven."
}

Push-Location $projectRoot
try {
    & $mavenPath clean package azure-webapp:deploy -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Azure deployment failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
}
