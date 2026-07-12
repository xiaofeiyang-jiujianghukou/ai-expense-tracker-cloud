#!/bin/sh
# Wait for Nacos to be ready, then:
# 1. Change default password
# 2. Upload gateway-routes.yaml to expense-cloud group
# Runs in background — main Nacos process continues normally

PASSWORD="${NACOS_ADMIN_PASSWORD:-xfylovesxy}"
NACOS_URL="http://localhost:8848"

echo "[nacos-init] Waiting for Nacos API..."
until curl -s -o /dev/null "$NACOS_URL/nacos/v1/auth/login" 2>/dev/null; do
  sleep 2
done

echo "[nacos-init] Logging in with default password..."
TOKEN=$(curl -s -X POST "$NACOS_URL/nacos/v1/auth/login" \
  -d "username=nacos&password=nacos" | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "[nacos-init] Changing password..."
  curl -s -X PUT "$NACOS_URL/nacos/v1/auth/users?accessToken=$TOKEN&username=nacos&newPassword=$PASSWORD"
  echo "[nacos-init] Password changed."
  # Get new token with new password (old token invalid after password change)
  TOKEN=$(curl -s -X POST "$NACOS_URL/nacos/v1/auth/login" \
    -d "username=nacos&password=$PASSWORD" | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
else
  echo "[nacos-init] Password already changed — logging in with new password..."
  TOKEN=$(curl -s -X POST "$NACOS_URL/nacos/v1/auth/login" \
    -d "username=nacos&password=$PASSWORD" | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
fi

# Upload gateway routes config
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "[nacos-init] Uploading gateway routes config..."
  curl -s -X POST "$NACOS_URL/nacos/v1/cs/configs" \
    -F "dataId=expense-gateway.yaml" \
    -F "group=expense-cloud" \
    -F "content=$(cat /home/nacos/gateway-routes.yaml)" \
    -F "accessToken=$TOKEN"
  echo "[nacos-init] Gateway routes uploaded."
fi

echo "[nacos-init] Done."
