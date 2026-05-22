#!/bin/bash
set -ex

export LC_ALL=C.UTF-8
export TZ=UTC

# Prep fdroidserver
mkdir -p /home/vagrant/fdroidserver
cp -r /tmp/fdroidserver/. /home/vagrant/fdroidserver/
export PATH="/home/vagrant/fdroidserver:$PATH"
export PYTHONPATH="/home/vagrant/fdroidserver:/home/vagrant/fdroidserver/examples"
export PYTHONUNBUFFERED=true

# Create an isolated workspace for fdroid build
mkdir -p /tmp/fdroiddata/metadata
# Copy the modified config without binaries checking so it does not delete the unsigned file
cp /tmp/no-binaries.yml /tmp/fdroiddata/metadata/space.o4bit.projectasteria.foss.yml
cd /tmp/fdroiddata

# Initialize it as a git repo
git config --global user.email "test@test.com"
git config --global user.name "Test User"
git init
git add metadata/
git commit -m "Init metadata"

# Create necessary dirs
for d in logs tmp unsigned /home/vagrant/.android /home/vagrant/.gradle; do 
  mkdir -p $d
done

# Run fdroid build to produce the unsigned APK exactly like CI does
HOME=/home/vagrant fdroid build --verbose --on-server space.o4bit.projectasteria.foss:40

# Now copy the exactly generated unsigned APK back
cp unsigned/space.o4bit.projectasteria.foss_40.apk /home/vagrant/build/space.o4bit.projectasteria.foss/fdroid-unsigned.apk
