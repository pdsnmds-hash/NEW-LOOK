"""Verify actual signed APK/AAB contents; never reads or prints signing passwords."""
from pathlib import Path
import json,os,subprocess,re,zipfile,struct,hashlib,argparse
root=Path(__file__).resolve().parents[1]
out=root/'release/validation';out.mkdir(parents=True,exist_ok=True)
sdk=Path('/Users/nomads_dm/Library/Android/sdk/build-tools/36.0.0')
jdk=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
parser=argparse.ArgumentParser();parser.add_argument('--apk',type=Path,default=root/'app/build/outputs/apk/release/app-release.apk');parser.add_argument('--aab',type=Path,default=root/'app/build/outputs/bundle/release/app-release.aab');parser.add_argument("--bundletool",type=Path,default=Path("/tmp/philly-bundletool.jar"));args=parser.parse_args()
artifacts={'apk':args.apk,'aab':args.aab}
abis={'armeabi-v7a':(1,40),'arm64-v8a':(2,183),'x86':(1,3),'x86_64':(2,62)}
report={}
def run(args,name):
 r=subprocess.run([str(x) for x in args],capture_output=True,text=True,check=True)
 (out/name).write_text(r.stdout+r.stderr);return r.stdout+r.stderr
for kind,path in artifacts.items():
 result={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None
  names=z.namelist();native=[]
  assert not any(n.endswith(('.jks','.keystore','.properties')) and ('signing' in n or 'upload-key' in n) for n in names)
  for n in names:
   bits=n.split('/')
   if not n.endswith('.so') or 'lib' not in bits:continue
   abi=bits[bits.index('lib')+1];data=z.read(n)
   assert data[:4]==b'\x7fELF'
   klass=data[4];endian='<' if data[5]==1 else '>'
   fields=struct.unpack_from(endian+('HHIQQQIHHHHHH' if klass==2 else 'HHIIIIIHHHHHH'),data,16)
   assert (klass,fields[1])==abis[abi],(n,klass,fields[1])
   loads=[]
   for i in range(fields[9]):
    ph=struct.unpack_from(endian+('IIQQQQQQ' if klass==2 else 'IIIIIIII'),data,fields[4]+i*fields[8])
    if ph[0]==1:
     align=ph[7];offset=ph[2] if klass==2 else ph[1];vaddr=ph[3] if klass==2 else ph[2]
     if klass==2:assert align>=16384 and (vaddr-offset)%align==0,(n,align)
     loads.append(align)
   native.append({'path':n,'abi':abi,'bits':64 if klass==2 else 32,'machine':fields[1],'load_alignments':loads})
  assert {r['abi'] for r in native}==set(abis)
  libraries={Path(r['path']).name for r in native}
  for library in libraries:assert {r['abi'] for r in native if Path(r['path']).name==library}==set(abis)
  result['native_libraries']=native
 if kind=='apk':
  sig=run([sdk/'apksigner','verify','--verbose','--print-certs',path],'release-apk-signature.log')
  result['certificate_sha256']=re.search(r'certificate SHA-256 digest: (\S+)',sig).group(1)
  badging=run([sdk/'aapt2','dump','badging',path],'release-apk-badging.log')
  assert re.search(r"(?:minSdkVersion|sdkVersion):'26'",badging) and "targetSdkVersion:'36'" in badging
  assert "package: name='bg.newlook.organizer'" in badging
  xml=run([sdk/'aapt2','dump','xmltree',path,'--file','AndroidManifest.xml'],'release-apk-manifest.log')
  assert 'android.permission.INTERNET' not in xml
  # debuggable may be absent or explicitly false, but never true.
  assert not re.search(r'debuggable[^\n]*0xffffffff',xml)
  run([sdk/'zipalign','-c','-P','16','-v','4',path],'release-apk-zipalign.log')
 else:
  run([jdk/'jarsigner','-verify','-verbose','-certs',path],'release-aab-signature.log')
  cert=run([jdk/'keytool','-printcert','-jarfile',path],'release-aab-certificate.log')
  result['certificate_sha256']=re.search(r'SHA256:\s*([^\n]+)',cert).group(1).replace(':','').lower()
  run([jdk/'java','-jar',args.bundletool,'validate','--bundle='+str(path)],'bundletool-validate.log')
  manifest=run([jdk/'java','-jar',args.bundletool,'dump','manifest','--bundle='+str(path),'--module=base'],'bundletool-manifest.xml')
  assert 'android:minSdkVersion="26"' in manifest and 'android:targetSdkVersion="36"' in manifest
  assert 'android:debuggable="true"' not in manifest
 report[kind]=result
assert report['apk']['certificate_sha256']==report['aab']['certificate_sha256']
public=run([jdk/'keytool','-printcert','-file',root/'release/signing/newlook-upload-certificate.pem'],'upload-certificate.log')
assert re.search(r'SHA256:\s*([^\n]+)',public).group(1).replace(':','').lower()==report['apk']['certificate_sha256']
report['result']='PASS';report['scope']='Static ABI/ELF/signature/SDK/alignment verification. Runtime ABI coverage is reported separately.'
(out/'release-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS APK and AAB: all four actual ABIs, ELF identities, 16 KB alignment for 64-bit libraries, matching upload signature, minSdk 26 / targetSdk 36, non-debuggable')
