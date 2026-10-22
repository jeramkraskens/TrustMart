#!/bin/bash
set -euo pipefail
cd /home/jeramkraskens/TrustMart/backend
export DJANGO_SETTINGS_MODULE=config.pythonanywhere
exec /home/jeramkraskens/trustmart-venv/bin/uvicorn config.asgi:application --uds "${DOMAIN_SOCKET:?PythonAnywhere socket missing}" --workers 1 --proxy-headers --forwarded-allow-ips '*'
