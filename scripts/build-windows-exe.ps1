param(
    [string]$AppName = "GS1Desk",
    [string]$Version = "1.0.0",
    [string]$JarPath = "target/GS1Desk-1.0.0-shaded.jar",
    [string]$OutputDir = "target/dist",
    [string]$IconPath = "$PSScriptRoot\java.ico"
)

if (-not (Test-Path $JarPath)) {
    Write-Error "Shaded jar not found at '$JarPath'. Run 'mvn clean package' first."
    exit 1
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$jdk = $env:JAVA_HOME
if (-not $jdk) {
    Write-Error "JAVA_HOME is not set. Please point it to a JDK 17+ installation."
    exit 1
}

$jpackage = Join-Path $jdk "bin\jpackage.exe"
if (-not (Test-Path $jpackage)) {
    Write-Error "jpackage was not found under $jdk. Ensure you are using a full JDK 17+."
    exit 1
}

if (-not (Test-Path $IconPath)) {
    # Generate a placeholder icon by copying java.ico from the JDK if available
    $defaultIcon = Join-Path $jdk "lib\images\cursors\java.ico"
    if (Test-Path $defaultIcon) {
        Copy-Item $defaultIcon $IconPath
    }
}

& $jpackage \
    --type exe \
    --name $AppName \
    --app-version $Version \
    --input (Split-Path -Path $JarPath) \
    --main-jar (Split-Path -Leaf $JarPath) \
    --main-class app.Main \
    --dest $OutputDir \
    --icon $IconPath \
    --java-options "-Xmx512m"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Created $OutputDir\$AppName-$Version.exe"
} else {
    Write-Error "jpackage failed with exit code $LASTEXITCODE"
}
