#!/bin/bash

# Change to the directory where the script is located
cd "$(dirname "$0")" || exit

ARTIFACT_ID="bukkit-smart-invs"
GRADLE_TASK="createBukkitJar"
JAR_PREFIX="SmartInvs-Bukkit"
MASTER_BRANCH="${MASTER_BRANCH:-master}"
MASTER_WORKTREE_DIR="${MASTER_WORKTREE_DIR:-../SmartInvs-master}"

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

install_missing_command() {
    local cmd="$1"
    local package="${2:-$1}"

    if command_exists "$cmd"; then
        return
    fi

    echo "$cmd is not installed. Attempting to install..."
    if [ "$OS" == "macOS" ]; then
        $INSTALL_CMD "$package"
    elif [ "$OS" == "Linux" ]; then
        $INSTALL_CMD "$package"
    fi

    if [ $? -ne 0 ]; then
        echo "Failed to install $cmd. Please install it manually."
        exit 1
    fi
}

prepare_master_worktree() {
    if [ -e "$MASTER_WORKTREE_DIR" ]; then
        if [ ! -d "$MASTER_WORKTREE_DIR/.git" ] && [ ! -f "$MASTER_WORKTREE_DIR/.git" ]; then
            echo "Error: $MASTER_WORKTREE_DIR exists but is not a git worktree."
            exit 1
        fi
    else
        echo "Creating $MASTER_BRANCH worktree at $MASTER_WORKTREE_DIR..."
        git fetch origin "$MASTER_BRANCH"

        if git show-ref --verify --quiet "refs/heads/$MASTER_BRANCH"; then
            git worktree add "$MASTER_WORKTREE_DIR" "$MASTER_BRANCH"
        else
            git worktree add -b "$MASTER_BRANCH" "$MASTER_WORKTREE_DIR" "origin/$MASTER_BRANCH"
        fi
    fi

    (
        cd "$MASTER_WORKTREE_DIR" || exit 1

        current_branch=$(git branch --show-current)
        if [ "$current_branch" != "$MASTER_BRANCH" ]; then
            echo "Error: $MASTER_WORKTREE_DIR is on '$current_branch', not '$MASTER_BRANCH'."
            exit 1
        fi

        echo "Updating $MASTER_BRANCH..."
        git fetch origin "$MASTER_BRANCH"
        git pull --ff-only origin "$MASTER_BRANCH"

        if [ ! -x ./gradlew ]; then
            chmod +x ./gradlew
        fi

        echo "Building $GRADLE_TASK from $MASTER_BRANCH..."
        ./gradlew clean "$GRADLE_TASK"
    )

    if [ $? -ne 0 ]; then
        echo "Build from $MASTER_BRANCH failed. Exiting."
        exit 1
    fi
}

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macOS"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="Linux"
else
    echo "Unsupported operating system. This script only works on macOS and Linux."
    exit 1
fi

# OS-specific package manager and installation commands
if [ "$OS" == "macOS" ]; then
    PKG_MANAGER="brew"
    INSTALL_CMD="brew install"
    if ! command_exists brew; then
        echo "Homebrew is not installed. Please install it first."
        echo "You can install Homebrew by running:"
        echo '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
        exit 1
    fi
elif [ "$OS" == "Linux" ]; then
    if command_exists apt-get; then
        PKG_MANAGER="apt-get"
        INSTALL_CMD="sudo apt-get install -y"
    elif command_exists yum; then
        PKG_MANAGER="yum"
        INSTALL_CMD="sudo yum install -y"
    else
        echo "Unsupported Linux distribution. Please install the required packages manually."
        exit 1
    fi
fi

# Check for required commands
install_missing_command git git
install_missing_command mvn maven

if ! command_exists java; then
    echo "java is not installed. Please install JDK 21 or newer."
    exit 1
fi

if [ "$OS" == "macOS" ]; then
    install_missing_command shasum perl
    install_missing_command md5
elif [ "$OS" == "Linux" ]; then
    install_missing_command shasum perl
    install_missing_command md5sum coreutils
fi

prepare_master_worktree

jar_files=("$MASTER_WORKTREE_DIR"/build/libs/"$JAR_PREFIX"-*.jar)
jar_path="${jar_files[0]}"

if [ ! -f "$jar_path" ]; then
    echo "Error: Could not find built jar at $MASTER_WORKTREE_DIR/build/libs/$JAR_PREFIX-*.jar"
    exit 1
fi

jar_name=$(basename "$jar_path")
version="${jar_name#$JAR_PREFIX-}"
version="${version%.jar}"

echo "Publishing $ARTIFACT_ID $version from $jar_path..."

# Run Maven install command
mvn install:install-file \
    -DgroupId=fr.minuskube.inv \
    -DartifactId="$ARTIFACT_ID" \
    -Dversion="$version" \
    -Dfile="$jar_path" \
    -Dpackaging=jar \
    -DlocalRepositoryPath=. \
    -DcreateChecksum=true \
    -DgeneratePom=true

if [ $? -ne 0 ]; then
    echo "Maven install failed. Exiting."
    exit 1
fi

# Generate SHA1 and MD5 checksums
echo "Generating checksums..."
cd fr/minuskube/inv/"$ARTIFACT_ID"/"$version" || exit

for file in "$ARTIFACT_ID"-"$version".{jar,pom}; do
    if [ "$OS" == "macOS" ]; then
        shasum -a 1 "$file" > "$file.sha1"
        md5 "$file" > "$file.md5"
    elif [ "$OS" == "Linux" ]; then
        sha1sum "$file" > "$file.sha1"
        md5sum "$file" > "$file.md5"
    fi
done

echo "Deployment complete. Files are ready to be committed and pushed to the repository."
