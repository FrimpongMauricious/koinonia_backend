#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

set -e

findMavenBasedir() {
  BASEDIR=$(dirname "$0")
  BASEDIR=$(cd "$BASEDIR" && pwd -P)
  WDIR="$BASEDIR"
  while [ -n "$WDIR" ] && [ "$WDIR" != "/" ]; do
    if [ -d "$WDIR"/.mvn ]; then
      BASEDIR=$WDIR
      break
    fi
    WDIR=$(dirname "$WDIR")
  done
  echo "$BASEDIR"
}

BASE_DIR=$(findMavenBasedir)

MAVEN_PROJECTBASEDIR="$BASE_DIR"
export MAVEN_PROJECTBASEDIR

WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$WRAPPER_PROPERTIES" ]; then
  DISTRIBUTION_URL=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | sed 's/distributionUrl=//')
else
  echo "Cannot find $WRAPPER_PROPERTIES" >&2
  exit 1
fi

MAVEN_DIST_NAME=$(basename "$DISTRIBUTION_URL")
MAVEN_DIST_NAME="${MAVEN_DIST_NAME%.zip}"
MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MAVEN_DIST_DIR="$MAVEN_USER_HOME/wrapper/dists/$MAVEN_DIST_NAME"
MAVEN_HOME="$MAVEN_DIST_DIR/$MAVEN_DIST_NAME"

if [ ! -d "$MAVEN_HOME" ]; then
  mkdir -p "$MAVEN_DIST_DIR"
  echo "Downloading $DISTRIBUTION_URL"
  DOWNLOAD_TEMP="$MAVEN_DIST_DIR/download.zip"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$DOWNLOAD_TEMP" "$DISTRIBUTION_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$DOWNLOAD_TEMP" "$DISTRIBUTION_URL"
  else
    echo "Neither curl nor wget found. Please install one to download Maven." >&2
    exit 1
  fi
  unzip -q "$DOWNLOAD_TEMP" -d "$MAVEN_DIST_DIR"
  rm -f "$DOWNLOAD_TEMP"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
