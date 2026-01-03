#!/bin/bash
set -e

# Determine the version based on the event type
if [[ "$GITHUB_EVENT_NAME" == "pull_request" ]]; then
  # Format: 0.0.0-PR<number>-<short_sha>
  PR_NUMBER=$(echo "$GITHUB_REF" | awk -F'/' '{print $3}')
  CALCULATED_VERSION="0.0.0-PR${PR_NUMBER}-SNAPSHOT"
elif [[ "$GITHUB_REF" == refs/tags/v* ]]; then
  CALCULATED_VERSION="${GITHUB_REF#refs/tags/v}"
else
  CALCULATED_VERSION="0.0.1-SNAPSHOT"
fi

echo "Calculated version: ${CALCULATED_VERSION}"

if [[ -n "$GITHUB_ENV" ]]; then
  echo "VERSION=${CALCULATED_VERSION}" >> "$GITHUB_ENV"
fi