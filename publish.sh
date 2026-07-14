#!/bin/bash
# DrishtiSDK Maven Central Publish Script
# ========================================
# Prerequisites:
#   1. GPG key generated and uploaded to keyserver
#   2. Sonatype OSSRH account created, namespace io.drishti verified
#   3. API token generated at https://central.sonatype.com → Settings → API Keys
#   4. Environment variables set (see below)
#
# Usage:
#   export OSSRH_USERNAME="your-sonatype-username"
#   export OSSRH_TOKEN="your-api-token"
#   export GPG_PRIVATE_KEY="$(cat ~/gpg-private-key.asc)"
#   export GPG_PASSPHRASE="your-gpg-passphrase"
#   ./publish.sh
#
# For CI (GitHub Actions), add these as repository secrets.

set -euo pipefail

# Validate environment
for var in OSSRH_USERNAME OSSRH_TOKEN GPG_PRIVATE_KEY GPG_PASSPHRASE; do
    if [ -z "${!var:-}" ]; then
        echo "ERROR: $var is not set. Export it before running this script."
        exit 1
    fi
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== DrishtiSDK Maven Central Publish ==="
echo "Group: io.drishti"
echo "Version: 1.0.0"
echo ""

# Step 1: Clean build
echo "[1/4] Building..."
cd "$SCRIPT_DIR"
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew assembleDebug testDebugUnitTest --offline \
  -Dorg.gradle.jvmargs="-Xmx1g" \
  -Dorg.gradle.workers.max=1

echo ""

# Step 2: API dump (ensure BCV is current)
echo "[2/4] Updating API dump..."
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew apiDump --offline

echo ""

# Step 3: Publish to OSSRH staging
echo "[3/4] Publishing to OSSRH staging..."
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew publishAllPublicationsToOSSRHRepository \
  -Dorg.gradle.jvmargs="-Xmx1g" \
  -Dorg.gradle.workers.max=1

echo ""

# Step 4: Instructions
echo "[4/4] Published to OSSRH staging!"
echo ""
echo "Next steps (manual):"
echo "  1. Go to https://central.sonatype.com"
echo "  2. Publishing → Staging Repositories"
echo "  3. Find your staging repo (io.drishti-XXXX)"
echo "  4. Click 'Close' (runs automated validation)"
echo "  5. If validation passes, click 'Release'"
echo "  6. Wait ~30 minutes for sync to Maven Central"
echo ""
echo "Verify publication:"
echo "  https://repo1.maven.org/maven2/io/drishti/"
