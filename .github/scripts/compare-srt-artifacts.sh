#!/usr/bin/env bash

set -euo pipefail

readonly SRT_VERSION="1.5.3-1.5.11"
readonly CI_REPOSITORY="${HOME}/.m2/repository"
readonly GITHUB_REPOSITORY="/tmp/srt-github-repository"
readonly REPORT_DIRECTORY="/tmp/srt-artifact-comparison"
readonly EXPERIMENT_POM="${REPORT_DIRECTORY}/pom.xml"

readonly -a ARTIFACTS=(
  "org/bytedeco/srt/${SRT_VERSION}/srt-${SRT_VERSION}.jar"
  "org/bytedeco/srt/${SRT_VERSION}/srt-${SRT_VERSION}.pom"
  "org/bytedeco/srt/${SRT_VERSION}/srt-${SRT_VERSION}-linux-arm64.jar"
  "org/bytedeco/srt/${SRT_VERSION}/srt-${SRT_VERSION}-linux-x86_64.jar"
  "org/bytedeco/srt-platform/${SRT_VERSION}/srt-platform-${SRT_VERSION}.jar"
  "org/bytedeco/srt-platform/${SRT_VERSION}/srt-platform-${SRT_VERSION}.pom"
)

mkdir -p "${REPORT_DIRECTORY}"

for artifact in "${ARTIFACTS[@]}"; do
  if [[ ! -f "${CI_REPOSITORY}/${artifact}" ]]; then
    echo "Missing CI-cached artifact: ${artifact}" | tee -a "${REPORT_DIRECTORY}/missing-ci-artifacts.txt"
  fi
done

cat > "${EXPERIMENT_POM}" <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.antmedia.experiment</groupId>
  <artifactId>srt-artifact-comparison</artifactId>
  <version>1</version>
  <repositories>
    <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/ant-media/javacpp-presets</url>
    </repository>
  </repositories>
  <dependencies>
    <dependency>
      <groupId>org.bytedeco</groupId>
      <artifactId>srt-platform</artifactId>
      <version>1.5.3-1.5.11</version>
    </dependency>
  </dependencies>
</project>
EOF

mvn --batch-mode \
  --settings mvn-settings.xml \
  --file "${EXPERIMENT_POM}" \
  -Dmaven.repo.local="${GITHUB_REPOSITORY}" \
  dependency:go-offline

comparison_failed=0
: > "${REPORT_DIRECTORY}/ci-sha256.txt"
: > "${REPORT_DIRECTORY}/github-sha256.txt"
: > "${REPORT_DIRECTORY}/comparison.txt"

for artifact in "${ARTIFACTS[@]}"; do
  ci_file="${CI_REPOSITORY}/${artifact}"
  github_file="${GITHUB_REPOSITORY}/${artifact}"

  if [[ ! -f "${ci_file}" ]]; then
    echo "MISSING_FROM_CI_CACHE ${artifact}" | tee -a "${REPORT_DIRECTORY}/comparison.txt"
    comparison_failed=1
    continue
  fi

  if [[ ! -f "${github_file}" ]]; then
    echo "MISSING_FROM_GITHUB ${artifact}" | tee -a "${REPORT_DIRECTORY}/comparison.txt"
    comparison_failed=1
    continue
  fi

  sha256sum "${ci_file}" | sed "s#${CI_REPOSITORY}/##" >> "${REPORT_DIRECTORY}/ci-sha256.txt"
  sha256sum "${github_file}" | sed "s#${GITHUB_REPOSITORY}/##" >> "${REPORT_DIRECTORY}/github-sha256.txt"

  if cmp --silent "${ci_file}" "${github_file}"; then
    echo "IDENTICAL ${artifact}" | tee -a "${REPORT_DIRECTORY}/comparison.txt"
  else
    echo "DIFFERENT ${artifact}" | tee -a "${REPORT_DIRECTORY}/comparison.txt"
    comparison_failed=1
  fi
done

exit "${comparison_failed}"
