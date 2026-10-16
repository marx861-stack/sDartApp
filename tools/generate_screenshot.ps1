param(
    [string]$Type = "bracket",
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\tournament_preview.png")
)

if (-not $env:JAVA_HOME -and (Test-Path "C:\Program Files\Android\Android Studio\jbr")) {
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
}
if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

Write-Host "Erzeuge Screenshot für Typ: $Type -> $OutputPath"
./gradlew :composeApp:run --args="--screenshot=$Type --output=$OutputPath"
