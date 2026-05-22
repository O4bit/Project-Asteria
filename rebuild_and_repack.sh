#!/bin/bash
set -e

echo "=== Building Unsigned APK ==="
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export SOURCE_DATE_EPOCH=1776710123
export TZ=UTC
export LC_ALL=C.UTF-8

# F-Droid runs this before build:
find . -name "google-services.json" -delete

./gradlew clean assembleRelease --no-daemon --no-build-cache

echo "=== Aligning APK ==="
/opt/android-sdk/build-tools/34.0.0/zipalign -p -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release-aligned.apk

echo "=== Signing APK with Java apksigner ==="
/opt/android-sdk/build-tools/34.0.0/apksigner sign \
  --ks /home/vagrant/build/space.o4bit.projectasteria.foss/projectasteriakey.jks \
  --ks-key-alias pak \
  --key-pass 'pass:A^f4bvmc&G^gw!g' \
  --ks-pass 'pass:A^f4bvmc&G^gw!g' \
  --min-sdk-version 1 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled false \
  --v4-signing-enabled false \
  --out /tmp/java-signed.apk \
  app/build/outputs/apk/release/app-release-aligned.apk

echo "=== Repacking signatures using Python apksigcopier ==="
export APKSIGCOPIER_COPY_EXTRA_BYTES=1
mkdir -p /tmp/sigdir
apt-get update && apt-get install -y python3-pip
pip3 install apksigcopier --break-system-packages
apksigcopier extract /tmp/java-signed.apk /tmp/sigdir
apksigcopier patch /tmp/sigdir app/build/outputs/apk/release/app-release-unsigned.apk release/app-release.apk

echo "=== Fixing ownership ==="
chown -R $(stat -c "%u:%g" projectasteriakey.jks) app/build release rebuild_and_repack.sh
echo "DONE"
