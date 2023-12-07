#!/bin/bash

check_marketplace() {
  INSTALL_DIRECTORY="/usr/local/antmedia"
  REST_URL='http://localhost:5080/rest/v2/server-settings'

  # Store original values
  ORIGINAL_SERVER_JWT_CONTROL=$(grep "^server.jwtServerControlEnabled=" $INSTALL_DIRECTORY/conf/red5.properties)
  ORIGINAL_SERVER_JWT_SECRET=$(grep "^server.jwtServerSecretKey=" $INSTALL_DIRECTORY/conf/red5.properties)

  # JWT Secret Key
  SECRET_KEY=$(openssl rand -base64 32 | head -c 32)

  # Update red5.properties
  sed -i "s^server.jwtServerControlEnabled=.*^server.jwtServerControlEnabled=true^" $INSTALL_DIRECTORY/conf/red5.properties
  sed -i "s^server.jwtServerSecretKey=.*^server.jwtServerSecretKey=$SECRET_KEY^" $INSTALL_DIRECTORY/conf/red5.properties

  # Restart antmedia
  systemctl restart antmedia

  jwt_generate() {
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
  JWT_KEY=$(jwt_generate)

  # Make REST API call and check response
  if response=$(curl -s -L "$REST_URL" --header "ProxyAuthorization: $JWT_KEY" | jq -e '.buildForMarket' 2>/dev/null); then
    echo "true"
  else
    echo "false"
  fi

  # Revert to original values
  sed -i "s/^server.jwtServerControlEnabled=.*/$ORIGINAL_SERVER_JWT_CONTROL/" $INSTALL_DIRECTORY/conf/red5.properties
  sed -i "s/^server.jwtServerSecretKey=.*/$ORIGINAL_SERVER_JWT_SECRET/" $INSTALL_DIRECTORY/conf/red5.properties
}