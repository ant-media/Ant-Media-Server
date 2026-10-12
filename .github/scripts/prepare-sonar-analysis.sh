#!/usr/bin/env bash
set -euo pipefail

# The existing workflow invokes sonar:sonar unchanged. Configure that invocation
# through the scanner's JSON environment parameters instead of running it here.
if [[ "$GITHUB_REF_TYPE" != 'branch' ]]; then
  exit 0
fi

pull_requests=$(gh api --method GET --paginate --slurp \
  "repos/${GITHUB_REPOSITORY}/pulls" \
  -f state=open -f "head=${GITHUB_REPOSITORY_OWNER}:${GITHUB_REF_NAME}" \
  | jq --arg repository "$GITHUB_REPOSITORY" \
    '[.[][] | select(.head.repo.full_name == $repository)]')

count=$(jq 'length' <<< "$pull_requests")
if [[ "$count" == 0 ]]; then
  echo 'No open PR for this branch; retaining normal SonarCloud branch analysis.'
  exit 0
fi
if [[ "$count" != 1 ]]; then
  echo '::error::More than one open PR uses this branch. A single SonarCloud invocation cannot analyze multiple PRs.'
  exit 1
fi

base=$(jq -r '.[0].base.ref' <<< "$pull_requests")
# Checkout remains unchanged in test.yml. Expand its shallow history and fetch
# the target branch here so SonarCloud can identify new code against the base.
fetch_args=(--no-tags)
if [[ $(git rev-parse --is-shallow-repository) == true ]]; then
  fetch_args+=(--unshallow)
fi
git fetch "${fetch_args[@]}" origin \
  "+refs/heads/${GITHUB_REF_NAME}:refs/remotes/origin/${GITHUB_REF_NAME}" \
  "+refs/heads/${base}:refs/remotes/origin/${base}"

existing_params=${SONAR_SCANNER_JSON_PARAMS:-'{}'}
params=$(jq -cn --argjson existing "$existing_params" \
  --argjson prs "$pull_requests" \
  '$existing + {
    "sonar.pullrequest.key": ($prs[0].number | tostring),
    "sonar.pullrequest.branch": $prs[0].head.ref,
    "sonar.pullrequest.base": $prs[0].base.ref
  }')
echo "SONAR_SCANNER_JSON_PARAMS=${params}" >> "$GITHUB_ENV"
echo "Prepared SonarCloud analysis for PR #$(jq -r '.[0].number' <<< "$pull_requests")."
