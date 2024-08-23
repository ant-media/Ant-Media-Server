#!/bin/bash
#
# Marketplace products are created with jwtServerControlEnabled and random jwtServerSecretKey.
#

INSTALL_DIRECTORY="$1"

generate_jwt() {
  REST_URL='http://localhost:5080/rest/v2/server-settings'
  SECRET_KEY=$(grep -oP '^server\.jwtServerSecretKey=\K.*' $INSTALL_DIRECTORY/conf/red5.properties)
  create_jwt_token() {
    # Header
    header='{"typ":"JWT","alg":"HS256"}'
    base64_header=$(echo -n "$header" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
    # Payload
    current_time=$(date +%s)
    expiration_time=$((current_time + 31536000)) # 365 days from now
    payload='{"name":"antmedia.io","exp":'$expiration_time'}'
    base64_payload=$(echo -n "$payload" | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
    # Signature
    signature=$(echo -n "$base64_header.$base64_payload" | openssl dgst -sha256 -hmac "$SECRET_KEY" -binary | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
    # JWT
    echo "$base64_header.$base64_payload.$signature"
  }

  # Get JWT key
  JWT_KEY=$(create_jwt_token)

}
