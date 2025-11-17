#!/bin/bash

AMS_DIR="/usr/local/antmedia"
KEYSTORE_PATH="$AMS_DIR/conf/keystore.jks"  
KEYSTORE_PASS=`sed -n 's/^rtmps\.keystorepass=\(.*\)$/\1/p' $AMS_DIR/conf/red5.properties`
ALIAS="tomcat"                  
DAYS_THRESHOLD=30

SERVICE_PATH="/etc/systemd/system/antmedia-ssl-renew.service"
TIMER_PATH="/etc/systemd/system/antmedia-ssl-renew.timer"

if [ ! -f "$SERVICE_PATH" ]; then
    cat <<EOF > "$SERVICE_PATH"
[Unit]
Description=Ant Media Server SSL Renew Service
Wants=network-online.target
After=network-online.target

[Service]
Type=oneshot
ExecStart=$0
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target timers.target
EOF
    chmod 644 "$SERVICE_PATH"
fi

if [ ! -f "$TIMER_PATH" ]; then
    cat <<EOF > "$TIMER_PATH"
[Unit]
Description=Ant Media Server SSL Renew Timer

[Timer]
OnCalendar=03:00
Persistent=true

[Install]
WantedBy=timers.target
EOF
    chmod 644 "$TIMER_PATH"

    systemctl daemon-reload
    systemctl enable --now antmedia-ssl-renew.timer
fi

CERT_EXPIRY=$(keytool -list -v -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASS" -alias "$ALIAS" | grep "until:" | awk -F"until: " '{print $2}'|head -1)


CERT_EXPIRY_EPOCH=$(date -d "$CERT_EXPIRY" +%s)
CURRENT_DATE_EPOCH=$(date +%s)

DAYS_LEFT=$(( (CERT_EXPIRY_EPOCH - CURRENT_DATE_EPOCH) / 86400 ))

if [ "$DAYS_LEFT" -le "$DAYS_THRESHOLD" ]; then
    $AMS_DIR/enable_ssl.sh -d $KEYSTORE_PASS
    echo $DAYS_LEFT
fi
