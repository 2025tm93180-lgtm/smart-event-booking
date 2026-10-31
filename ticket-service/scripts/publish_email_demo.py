#!/usr/bin/env python3
"""Publish a fresh two-seat confirmation addressed to the supplied test recipient."""
import argparse,json,subprocess,uuid
from datetime import datetime,timezone
from pathlib import Path
from dev_token import settings
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--email',required=True,help='Recipient you are authorized to email.')
a=p.parse_args()
if settings().get('NOTIFICATION_MODE','mock').lower()!='smtp':
    raise SystemExit('Configure NOTIFICATION_MODE=smtp in .env and rebuild/recreate the app first.')
if '@' not in a.email or any(c in a.email for c in '\r\n ,;'):
    raise SystemExit('Supply one email address without display-name syntax.')
now=datetime.now(timezone.utc).isoformat()
booking='email-demo-'+uuid.uuid4().hex
value={'eventId':str(uuid.uuid4()),'schemaVersion':1,'eventType':'booking.confirmed.v1','correlationId':str(uuid.uuid4()),'occurredAt':now,'bookingId':booking,'payload':{'customerId':'customer-001','customerEmail':a.email,'targetEventId':101,'confirmedAt':now,'items':[{'seatId':501},{'seatId':502}]}}
line=booking+'|'+json.dumps(value,separators=(',',':'))+'\n'
subprocess.run(['docker','compose','exec','-T','kafka','/opt/kafka/bin/kafka-console-producer.sh','--bootstrap-server','kafka:19092','--topic','booking.confirmed.v1','--property','parse.key=true','--property','key.separator=|'],input=line,text=True,cwd=root,check=True)
print('Published booking: '+booking)
print('This requests two real emails, one per ticket. Inspect notifications for this booking ID.')
