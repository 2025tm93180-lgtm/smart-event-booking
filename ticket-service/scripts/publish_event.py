#!/usr/bin/env python3
"""Publish sample confirmation to the local Compose Kafka broker."""
import argparse,json,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('file',nargs='?',default=str(root/'examples/booking-confirmed.json'))
p.add_argument('--count',type=int,default=1)
a=p.parse_args()
event=json.loads(Path(a.file).read_text())
line=event['bookingId']+'|'+json.dumps(event,separators=(',',':'))+'\n'
subprocess.run(['docker','compose','exec','-T','kafka','/opt/kafka/bin/kafka-console-producer.sh','--bootstrap-server','kafka:19092','--topic','booking.confirmed.v1','--property','parse.key=true','--property','key.separator=|'],input=line*a.count,text=True,cwd=root,check=True)
print(f'Published {a.count} confirmation(s) for {event["bookingId"]}.')
