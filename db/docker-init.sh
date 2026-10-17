#!/bin/sh
# Запускается контейнером postgres при самом первом старте (пустой том).
set -e
psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" --dbname postgres \
     -v owner_password="$DB_OWNER_PASSWORD" \
     -v app_password="$DB_APP_PASSWORD" \
     -f /db/init.sql
