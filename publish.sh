#!/bin/bash
# DrishtiSDK Maven Central Publish Script (Central Portal)
# =========================================================
# Prerequisites:
#   1. GPG key generated and uploaded to keyserver
#   2. Namespace io.github.LathissKhumar verified on central.sonatype.com
#   3. Portal user token generated at https://central.sonatype.com → Settings → API Keys
#   4. Environment variables set (see below)
#
# Usage:
#   export MAVEN_CENTRAL_USERNAME="your-portal-token-username"
#   export MAVEN_CENTRAL_PASSWORD="your-portal-token-password"
#   export SIGNING_KEY="$(cat ~/gpg-private-key.asc)"
#   export SIGNING_PASSWORD="your-gpg-passphrase"
#   ./publish.sh

set -euo pipefail

for var in MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_PASSWORD SIGNING_KEY SIGNING_PASSWORD; do
    if [ -z "${!var:-}" ]; then
        echo "ERROR: $var is not set. Export it before running this script."
        exit 1
    fi
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== DrishtiSDK Maven Central Publish (Central Portal) ==="
echo "Group: io.github.LathissKhumar"
echo "Version: 1.0.0"
echo ""

echo "[1/3] Building..."
cd "$SCRIPT_DIR"
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew assembleDebug testDebugUnitTest --offline \
  -Dorg.gradle.jvmargs="-Xmx1g" \
  -Dorg.gradle.workers.max=1

echo ""

echo "[2/3] Updating API dump..."
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew apiDump --offline

echo ""

echo "[3/3] Publishing to Maven Central Portal..."
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
ORG_GRADLE_PROJECT_mavenCentralUsername="$MAVEN_CENTRAL_USERNAME" \
ORG_GRADLE_PROJECT_mavenCentralPassword="$MAVEN_CENTRAL_PASSWORD" \
ORG_GRADLE_PROJECT_signingInMemoryKey="$SIGNING_KEY" \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$SIGNING_PASSWORD" \
./gradlew publishAndReleaseToMavenCentral \
  -Dorg.gradle.jvmargs="-Xmx1g" \
  -Dorg.gradle.workers.max=1

echo ""
echo "Published! Artifacts will appear on Maven Central within ~30 minutes."
echo "Check: https://central.sonatype.com/publishing/deployments"
