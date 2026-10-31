#!/usr/bin/env python3
"""Local test helper, not an application auth endpoint. Uses Python standard library."""
import argparse, base64, hashlib, hmac, json, os, time
from pathlib import Path

def settings():
    values = {}
    path = Path(__file__).resolve().parents[1] / '.env'
    if path.exists():
        for line in path.read_text().splitlines():
            if line.strip() and not line.lstrip().startswith('#'):
                key, value = line.split('=', 1)
                values[key] = value
    values.update(os.environ)
    return values

def token(customer='customer-001', role='CUSTOMER'):
    env = settings()
    secret = env.get('JWT_SECRET', '')
    if len(secret.encode()) < 32:
        raise SystemExit('Set JWT_SECRET or run scripts/setup_env.py first.')
    def encode(value):
        return base64.urlsafe_b64encode(value).rstrip(b'=').decode()
    now = int(time.time())
    header = encode(json.dumps({'alg':'HS256','typ':'JWT'},separators=(',',':')).encode())
    payload = encode(json.dumps({'sub':customer,'role':role,'iss':env.get('JWT_ISSUER','smart-event-booking'),'aud':env.get('JWT_AUDIENCE','ticket-api'),'iat':now,'exp':now+3600},separators=(',',':')).encode())
    value = header + '.' + payload
    return value + '.' + encode(hmac.new(secret.encode(),value.encode(),hashlib.sha256).digest())

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--customer',default='customer-001')
    p.add_argument('--role',choices=['CUSTOMER','ADMIN'],default='CUSTOMER')
    args = p.parse_args()
    print(token(args.customer,args.role))
