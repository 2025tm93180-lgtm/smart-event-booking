#!/usr/bin/env python3
"""Run after Compose starts. Replays confirmations and checks ownership via real JWTs."""
import json,subprocess,sys,time,urllib.request,urllib.error,uuid
from pathlib import Path
from dev_token import token,settings
root=Path(__file__).resolve().parents[1]
base='http://localhost:8084'
if settings().get('NOTIFICATION_MODE','mock').lower()!='mock':
    raise SystemExit('This smoke test uses mock notifications. Use scripts/publish_email_demo.py for SMTP mode.')

def request(path,bearer=None):
    headers={'Authorization':'Bearer '+bearer} if bearer else {}
    try:
        with urllib.request.urlopen(urllib.request.Request(base+path,headers=headers),timeout=5) as response:
            return response.status,json.load(response)
    except urllib.error.HTTPError as ex:
        return ex.code,json.load(ex)

for _ in range(90):
    try:
        if request('/health')[0]==200: break
    except (OSError,ValueError): pass
    time.sleep(1)
else: raise SystemExit('Service did not become healthy.')
subprocess.run([sys.executable,str(root/'scripts/publish_event.py'),'--count','2'],check=True)
path='/api/v1/tickets/by-booking/booking-001'
for _ in range(45):
    status,body=request(path,token())
    if status==200 and body['total']==2: break
    time.sleep(1)
else: raise SystemExit('Expected two tickets, got '+str((status,body)))
ids=[item['id'] for item in body['items']]
subprocess.run([sys.executable,str(root/'scripts/publish_event.py'),'--count','20'],check=True)
time.sleep(3)
status,body=request(path,token())
assert status==200 and body['total']==2 and ids==[item['id'] for item in body['items']],body
assert request(path,token('another-customer'))[0]==403
assert request(path)[0]==401
assert request(path+'?size=101',token())[0]==400
for _ in range(30):
    status,notifications=request('/api/v1/notifications/by-booking/booking-001',token())
    if status==200 and notifications['total']==2 and all(n['status']=='SENT' for n in notifications['items']):break
    time.sleep(1)
else:raise SystemExit('Expected two SENT mock notifications. Is MOCK_NOTIFICATION_FAIL=false?')
print(json.dumps({'result':'PASS','ticketCount':2,'notificationCount':2,'ticketIds':ids,'checks':['duplicates','ownership','authentication','pagination','mock-delivery']},indent=2))
