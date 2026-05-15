param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$mavenVersion = "3.9.9"
$mavenBaseDir = Join-Path $scriptDir "apache-maven-$mavenVersion"
$zipPath = Join-Path $scriptDir "apache-maven-$mavenVersion-bin.zip"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$mvnCmd = Join-Path $mavenBaseDir "bin\mvn.cmd"

if (!(Test-Path $mvnCmd)) {
    if (!(Test-Path $zipPath)) {
        Write-Host "Downloading Maven $mavenVersion..."
        Invoke-WebRequest -Uri $mavenUrl -OutFile $zipPath
    }

    Write-Host "Extracting Maven $mavenVersion..."
    Expand-Archive -Path $zipPath -DestinationPath $scriptDir -Force
}

& $mvnCmd "-f" (Join-Path $projectRoot "pom.xml") @MavenArgs
exit $LASTEXITCODE
