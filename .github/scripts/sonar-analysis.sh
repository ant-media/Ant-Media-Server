#!/usr/bin/env bash
set -euo pipefail

# Reuse the unit-test JaCoCo report for every open PR from this branch.
# Explicit PR parameters override the scanner's push-event branch detection.
pull_requests='[]'
if [[ "$GITHUB_EVENT_NAME" == 'pull_request' ]]; then
  pull_requests=$(jq '[.pull_request | {number, head, base}]' "$GITHUB_EVENT_PATH")
elif [[ "$GITHUB_REF_TYPE" == 'branch' ]]; then
  pull_requests=$(gh api --method GET --paginate --slurp \
    "repos/${GITHUB_REPOSITORY}/pulls" \
    -f state=open -f "head=${GITHUB_REPOSITORY_OWNER}:${GITHUB_REF_NAME}" \
    | jq --arg repository "$GITHUB_REPOSITORY" \
      '[.[][] | select(.head.repo.full_name == $repository)]')
fi

sonar_args=(
  sonar:sonar
  -Dmaven.javadoc.skip=true
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
  "-Dsonar.scm.revision=$(git rev-parse HEAD)"
)

if [[ $(jq 'length' <<< "$pull_requests") == 0 ]]; then
  echo 'No open PR for this ref; running normal SonarCloud analysis.'
  mvn "${sonar_args[@]}"
else
  while IFS= read -r pull_request; do
    number=$(jq -r '.number' <<< "$pull_request")
    branch=$(jq -r '.head.ref' <<< "$pull_request")
    base=$(jq -r '.base.ref' <<< "$pull_request")
    echo "Running SonarCloud PR analysis for #${number}: ${branch} -> ${base}"
    mvn "${sonar_args[@]}" \
      "-Dsonar.pullrequest.key=${number}" \
      "-Dsonar.pullrequest.branch=${branch}" \
      "-Dsonar.pullrequest.base=${base}"
  done < <(jq -c '.[]' <<< "$pull_requests")
fi
