param(
    [string]$PublishVersion = $(if ($env:PUBLISH_VERSION) { $env:PUBLISH_VERSION } else { $env:VERSION })
)

# Change to the directory where the script is located
Set-Location -Path $PSScriptRoot

$ArtifactId = "bukkit-smart-invs"
$GradleTask = "createBukkitJar"
$JarPrefix = "SmartInvs-Bukkit"

if ($env:MASTER_BRANCH) {
    $MasterBranch = $env:MASTER_BRANCH
}
else {
    $MasterBranch = "master"
}

if ($env:MASTER_WORKTREE_DIR) {
    $MasterWorktreeDir = $env:MASTER_WORKTREE_DIR
}
else {
    $MasterWorktreeDir = Join-Path $PSScriptRoot "..\SmartInvs-master"
}

if (-not [System.IO.Path]::IsPathRooted($MasterWorktreeDir)) {
    $MasterWorktreeDir = Join-Path $PSScriptRoot $MasterWorktreeDir
}
$MasterWorktreeDir = [System.IO.Path]::GetFullPath($MasterWorktreeDir)

function Test-Command($cmdname) {
    return [bool](Get-Command -Name $cmdname -ErrorAction SilentlyContinue)
}

function Ensure-Command {
    param(
        [string]$Command,
        [string]$Package,
        [bool]$CanInstall = $true
    )

    if (Test-Command $Command) {
        return
    }

    if (-not $CanInstall) {
        Write-Host "$Command is not installed. Please install it manually."
        exit 1
    }

    if (-not $Package) {
        $Package = $Command
    }

    if (-not (Test-Command choco)) {
        Write-Host "$Command is not installed, and Chocolatey is not available to install $Package."
        exit 1
    }

    Write-Host "$Command is not installed. Attempting to install via Chocolatey..."
    choco install $Package -y
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to install $Command. Please install it manually."
        exit 1
    }
}

function Invoke-CheckedCommand {
    param(
        [string]$Command,
        [string[]]$Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        Write-Host "$Command failed. Exiting."
        exit 1
    }
}

function Prepare-MasterWorktree {
    if (-not (Test-Path $MasterWorktreeDir)) {
        Write-Host "Creating $MasterBranch worktree at $MasterWorktreeDir..."
        Invoke-CheckedCommand "git" @("fetch", "origin", $MasterBranch)

        git show-ref --verify --quiet "refs/heads/$MasterBranch"
        if ($LASTEXITCODE -eq 0) {
            Invoke-CheckedCommand "git" @("worktree", "add", $MasterWorktreeDir, $MasterBranch)
        }
        else {
            Invoke-CheckedCommand "git" @("worktree", "add", "-b", $MasterBranch, $MasterWorktreeDir, "origin/$MasterBranch")
        }
    }
    elseif (-not (Test-Path (Join-Path $MasterWorktreeDir ".git"))) {
        Write-Host "Error: $MasterWorktreeDir exists but is not a git worktree."
        exit 1
    }

    Push-Location $MasterWorktreeDir

    $currentBranch = (git branch --show-current).Trim()
    if ($currentBranch -ne $MasterBranch) {
        Write-Host "Error: $MasterWorktreeDir is on '$currentBranch', not '$MasterBranch'."
        exit 1
    }

    Write-Host "Updating $MasterBranch..."
    Invoke-CheckedCommand "git" @("fetch", "origin", $MasterBranch)
    Invoke-CheckedCommand "git" @("pull", "--ff-only", "origin", $MasterBranch)

    $gradlew = Join-Path $MasterWorktreeDir "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        $gradlew = Join-Path $MasterWorktreeDir "gradlew"
    }

    if (-not (Test-Path $gradlew)) {
        Write-Host "Error: Could not find Gradle wrapper in $MasterWorktreeDir."
        exit 1
    }

    Write-Host "Building $GradleTask from $MasterBranch..."
    Invoke-CheckedCommand $gradlew @("clean", $GradleTask)

    Pop-Location
}

function Write-CertutilHash {
    param(
        [string]$File,
        [string]$Algorithm,
        [string]$OutputPath
    )

    $hashLine = certutil -hashfile $File $Algorithm |
        Where-Object { $_ -match "^[0-9a-fA-F ]+$" } |
        Select-Object -First 1

    if (-not $hashLine) {
        Write-Host "Failed to generate $Algorithm hash for $File."
        exit 1
    }

    $hash = ($hashLine -replace " ", "").ToLowerInvariant()
    Set-Content -Path $OutputPath -Value $hash
}

Ensure-Command -Command "git"
Ensure-Command -Command "mvn" -Package "maven"
Ensure-Command -Command "certutil" -CanInstall $false

if (-not (Test-Command java)) {
    Write-Host "java is not installed. Please install JDK 21 or newer."
    exit 1
}

Prepare-MasterWorktree

$libsDir = Join-Path $MasterWorktreeDir "build\libs"
$jar = Get-ChildItem -Path $libsDir -Filter "$JarPrefix-*.jar" -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    Write-Host "Error: Could not find built jar at $libsDir\$JarPrefix-*.jar"
    exit 1
}

$jarPath = $jar.FullName
$inferredVersion = $jar.BaseName.Substring($JarPrefix.Length + 1)

if ($PublishVersion) {
    $version = $PublishVersion
    if ($version -ne $inferredVersion) {
        Write-Host "Using publish version $version; built jar version is $inferredVersion."
    }
}
else {
    $version = $inferredVersion
}

Write-Host "Publishing $ArtifactId $version from $jarPath..."

mvn install:install-file `
    "-DgroupId=fr.minuskube.inv" `
    "-DartifactId=$ArtifactId" `
    "-Dversion=$version" `
    "-Dfile=$jarPath" `
    "-Dpackaging=jar" `
    "-DlocalRepositoryPath=." `
    "-DcreateChecksum=true" `
    "-DgeneratePom=true"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven install failed. Exiting."
    exit 1
}

Write-Host "Generating checksums..."
Set-Location -Path "fr\minuskube\inv\$ArtifactId\$version"

$files = @("$ArtifactId-$version.jar", "$ArtifactId-$version.pom")
foreach ($file in $files) {
    Write-CertutilHash -File $file -Algorithm "SHA1" -OutputPath "$file.sha1"
    Write-CertutilHash -File $file -Algorithm "MD5" -OutputPath "$file.md5"
}

Write-Host "Deployment complete. Files are ready to be committed and pushed to the repository."
