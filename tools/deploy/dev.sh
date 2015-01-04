#!/bin/bash
#
# Setups build environment based on Ubuntu 14.04 Linux.
# Can be used as a "user data" when creating a new Digital Ocean's "droplet".

SBT_VERSION="0.13.7"

wget https://dl.bintray.com/sbt/debian/sbt-${SBT_VERSION}.deb

echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections

add-apt-repository -y ppa:webupd8team/java
apt-get -y update
apt-get -y upgrade

# Installing Java 8
apt-get -y install oracle-java8-installer

rm -rf /var/lib/apt/lists/*
rm -rf /var/cache/oracle-jdk8-installer

# Installing SBT
dpkg -i sbt-${SBT_VERSION}.deb

apt-get -y install sbt

# Something weird happens at this moment so this update is required to proceed
# further
apt-get -y update

# Installing GIT and Docker to be able to build a project and publish a docker
# image of the server to Docker Hub
apt-get -y install git docker.io

apt-get -y autoremove

export JAVA_HOME=/usr/lib/jvm/java-8-oracle

# Clonning repository in readonly mode
cd ~/
git clone git://github.com/goldobin/jetonmatik.git

# Building the project
cd ~/jetonmatik
sbt compile test

# To use Github in read/write mode it is required to genarate new or put
# already generated and registered private part of ssh key pair to the ~/.ssh
# directory.
#
# Snippet:
#   ssh root@5.101.102.81 "mkdir /root/.ssh"
#   scp ~/.ssh/id_rsa root@5.101.102.81:/root/.ssh/
#   ssh root@5.101.102.81 "chmod 600 /root/.ssh/id_rsa"