#!/bin/bash
set -ex

echo "=== Aligning APK ==="
/opt/android-sdk/build-tools/34.0.0/zipalign -p -f 4 \
  fdroid-unsigned.apk \
  fdroid-aligned.apk

echo "=== Signing APK with Java apksigner ==="
/opt/android-sdk/build-tools/34.0.0/apksigner sign \
  --ks projectasteriakey.jks \
  --ks-key-alias pak \
  --key-pass 'pass:A^f4bvmc&G^gw!g' \
  --ks-pass 'pass:A^f4bvmc&G^gw!g' \
  --min-sdk-version 1 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled false \
  --v4-signing-enabled false \
  --out /tmp/java-signed.apk \
  fdroid-aligned.apk

echo "=== Patching signatures with apksigcopier ==="
export APKSIGCOPIER_COPY_EXTRA_BYTES=1
mkdir -p /tmp/sigdir
apt-get update && apt-get install -y python3-pip
pip3 install apksigcopier --break-system-packages
apksigcopier extract /tmp/java-signed.apk /tmp/sigdir
apksigcopier patch /tmp/sigdir fdroid-unsigned.apk final-app-release.apk

echo "=== Verifying the Final Repacked APK ==="
/opt/android-sdk/build-tools/34.0.0/apksigner verify --verbose --min-sdk-version 1 final-app-release.apk

echo "=== Fixing ownership ==="
chown $(stat -c "%u:%g" projectasteriakey.jks) final-app-release.apk
echo "DONE"
