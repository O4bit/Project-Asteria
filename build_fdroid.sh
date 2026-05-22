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
cp space.o4bit.projectasteria.foss.yml /tmp/fdroiddata/metadata/
cd /tmp/fdroiddata

# Initialize it as a git repo (fdroid often expects metadata to be tracked)
git config --global user.email aaa@aaa.com && git config --global user.name A && git init
git add metadata/
git commit -m "Init metadata"

# Create necessary dirs
for d in logs tmp unsigned /home/vagrant/.android /home/vagrant/.gradle; do 
  mkdir -p $d
done

# We need the source available locally since fdroid build will check it out natively
# No wait! F-droid will implicitly `git clone` from github.com/O4bit/Project-Asteria.git
# We can just let it do that so we perfectly follow the CI routine!

# Run fdroid build
HOME=/home/vagrant fdroid build --verbose --on-server space.o4bit.projectasteria.foss:40

# Now extract the built unsigned APK
cp unsigned/space.o4bit.projectasteria.foss_40.apk /home/vagrant/build/space.o4bit.projectasteria.foss/fdroid-unsigned.apk
