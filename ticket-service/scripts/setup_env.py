#!/usr/bin/env python3
"""Generate local-only secrets. Never overwrites an existing .env."""
from pathlib import Path
import secrets
path = Path(__file__).resolve().parents[1] / '.env'
if path.exists():
    raise SystemExit('.env already exists; left unchanged.')
with path.open('x') as f:
    f.write(f'MYSQL_ROOT_PASSWORD={secrets.token_hex(24)}\nDB_PASSWORD={secrets.token_hex(24)}\nJWT_SECRET={secrets.token_urlsafe(48)}\nJWT_ISSUER=smart-event-booking\nJWT_AUDIENCE=ticket-api\nMOCK_NOTIFICATION_FAIL=false\nNOTIFICATION_MODE=mock\nMAIL_FROM=\nSMTP_HOST=\nSMTP_PORT=587\nSMTP_USERNAME=\nSMTP_PASSWORD=\nSMTP_AUTH=true\nSMTP_STARTTLS=true\nSMTP_STARTTLS_REQUIRED=true\nSMTP_SSL_ENABLED=false\n')
path.chmod(0o600)
print('Created .env with random local credentials.')
