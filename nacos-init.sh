#!/bin/sh
# Wait for Nacos to be ready, then change default password
# Runs in background — main Nacos process continues normally

PASSWORD="${NACOS_ADMIN_PASSWORD:-xfylovesxy}"

echo "[nacos-init] Waiting for Nacos API..."
until curl -s -o /dev/null "http://localhost:8848/nacos/v1/auth/login" 2>/dev/null; do
  sleep 2
done

echo "[nacos-init] Logging in with default password..."
TOKEN=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
  -d "username=nacos&password=nacos" | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "[nacos-init] Changing password..."
  curl -s -X PUT "http://localhost:8848/nacos/v1/auth/users?accessToken=$TOKEN&username=nacos&newPassword=$PASSWORD"
  echo "[nacos-init] Done."
else
  echo "[nacos-init] Password already changed or token not received — skipping."
fi
