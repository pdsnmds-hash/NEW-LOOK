from pathlib import Path
import subprocess,xml.etree.ElementTree as ET,re,time,sys
ADB='/Users/nomads_dm/Library/Android/sdk/platform-tools/adb';DEVICE='emulator-5556'
def adb(*args):return subprocess.run([ADB,'-s',DEVICE,*args],capture_output=True,check=True).stdout
def nodes():
 adb('shell','uiautomator','dump','/sdcard/newlook-ui.xml');return ET.fromstring(adb('exec-out','cat','/sdcard/newlook-ui.xml')).iter('node')
def tap(label):
 for attempt in range(4):
  for n in nodes():
   if n.get('text')==label or n.get('content-desc')==label:
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    if y2-y1<12:continue
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(1.5);return
  time.sleep(1)
 raise RuntimeError('Not visible: '+label)
def screenshot(path):
 time.sleep(1.2)
 Path(path).write_bytes(adb('exec-out','screencap','-p'))
if __name__=='__main__':
 if len(sys.argv)>2 and sys.argv[1]=='tap':tap(sys.argv[2])
 elif len(sys.argv)>2 and sys.argv[1]=='shot':screenshot(sys.argv[2])
 else:
  for n in nodes():
   if n.get('text') or n.get('content-desc'):print(n.get('text'),n.get('content-desc'),n.get('bounds'))
