#!/bin/bash

set -e
set -x

MACHINE=$(uname -m | sed 's/arm64/aarch64/')

(
  # Do a manual build of magen using a newer build env.
  # Manually install the magen deps - the prereq script uses a token and not user/token auth
  echo "Boost is required. Make sure to have it installed. Uses the Velox Boost version setup previously."
  if [[ "$MACHINE" == "x86_64" ]]; then
    LIBFA_VERSION=1.5.3
    curl -u ${artifactory_user}:${artifactory_token} https://na-public.artifactory.swg-devops.com/artifactory/hyc-mpc-artifactory-team-generic-local/data-privacy/com/ibm/dp/libfa/$LIBFA_VERSION/fa.h -o /tmp/fa.h
    chmod 755 /tmp/fa.h
    cp /tmp/fa.h /usr/local/include/fa.h
    rm -rf /tmp/fa.h

    curl -u ${artifactory_user}:${artifactory_token} https://na-public.artifactory.swg-devops.com/artifactory/hyc-mpc-artifactory-team-generic-local/data-privacy/com/ibm/dp/libfa/$LIBFA_VERSION/libfa.so.rhel8.x.tgz -o /tmp/libfa.so.rhel8.x.tgz
    cd /tmp
    tar -xvzf libfa.so.rhel8.x.tgz > /dev/null
    chmod 755 libfa.so.1.5.3
    cp libfa.so.1.5.3 /usr/local/lib/libfa.so.1.5.3
    cd /usr/local/lib
    ln -s libfa.so.1.5.3 libfa.so.1
    ln -s libfa.so.1.5.3 libfa.so
    rm -rf /tmp/libfa.so.rhel8.x.tgz
  else
    if [ -x $(command -v dnf) ]; then
      dnf install -y libxml2-devel https://rpmfind.net/linux/centos-stream/9-stream/AppStream/${MACHINE}/os/Packages/readline-devel-8.1-4.el9.${MACHINE}.rpm
    else
      apt install -y libreadline-dev libxml2-dev
    fi
    wget -q http://download.augeas.net/augeas-1.6.0.tar.gz
    tar -xzf augeas-1.6.0.tar.gz
    (
      cd augeas-1.6.0
      ./configure
      make
      make install
    )
  fi

  # Fetch ICU 72 headers to match libraries and to be used for build.
  cd /tmp
  if [[ "$MACHINE" == "aarch64" || "$MACHINE" == "ppc64le" ]]; then
    # Download ICU source to build for aarch64 or ppc64le
    wget https://github.com/unicode-org/icu/archive/refs/tags/release-72-1.tar.gz
    tar -xvzf release-72-1.tar.gz
    (
      cd icu-release-72-1/icu4c/source
      ./configure
      make
      make install
    )
  else
    # Fetch ICU 72 headers to match libraries and to be used for build.
    wget https://github.com/unicode-org/icu/releases/download/release-72-1/icu4c-72_1-Fedora_Linux36-x64.tgz
    tar -xvzf icu4c-72_1-Fedora_Linux36-x64.tgz
    cp -r icu/usr/local/include/unicode /usr/local/include/unicode
    rm -rf icu4c-72_1-Fedora_Linux36-x64.tgz icu

    # Get ICU libs
    curl -u ${artifactory_user}:${artifactory_token} https://na-public.artifactory.swg-devops.com/artifactory/hyc-mpc-artifactory-team-generic-local/data-privacy/com/ibm/dp/magen-cpp/4.7.2/icu4clibs-rhel_8.x.tgz -o icu4clibs-rhel_8.x.tgz
    tar -xvf icu4clibs-rhel_8.x.tgz -C /usr/local/lib/
    rm -f icu4clibs-rhel_8.x.tgz
  fi
)

# Magen source
curl -u ${artifactory_user}:${artifactory_token} https://na-public.artifactory.swg-devops.com/artifactory/hyc-mpc-artifactory-team-generic-local/data-privacy/com/ibm/dp/magen-cpp/4.7.2/magen_src.tgz -o magen_src.tgz
tar -xvf magen_src.tgz
mkdir -p /usr/local/magen
cp -rf magen-cpp/resources /usr/local/magen/.
cd magen-cpp
# Cleanup artifacts from MacOS that were packaged into the tar file and causes build issues.
find . -name "._*" | xargs rm -rf

# Make sure libraries are found for the Magen test.
ldconfig /usr/local/lib

# Do the build and test.
./cmake-linux.sh
mv -f build/libmagen.so /usr/local/lib/libmagen.so
cd ..

rm -rf magen-cpp
rm -rf magen_src.tgz
