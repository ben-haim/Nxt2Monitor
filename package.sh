#!/bin/sh

#######################
# Package Nxt2Monitor #
#######################

if [ -z "$1" ] ; then
  echo "You must specify the version to package"
  exit 1
fi

VERSION="$1"

if [ ! -d package ] ; then
  mkdir package
fi

cd package
rm -R *
cp ../ChangeLog.txt ../LICENSE ../README.md ../Nxt2Monitor.conf .
cp ../target/Nxt2Monitor-$VERSION.jar .
cp -R ../target/lib lib
zip -r Nxt2Monitor-$VERSION.zip ChangeLog.txt LICENSE README.md Nxt2Monitor.conf Nxt2Monitor-$VERSION.jar lib
dos2unix ChangeLog.txt LICENSE README.md Nxt2Monitor.conf
tar zcf Nxt2Monitor-$VERSION.tar.gz ChangeLog.txt LICENSE README.md Nxt2Monitor.conf Nxt2Monitor-$VERSION.jar lib
exit 0

