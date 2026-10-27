"""Package a universal APK from the built AAB using private local signing properties."""
from pathlib import Path
import tempfile,subprocess,os,zipfile,shutil,argparse
parser=argparse.ArgumentParser();parser.add_argument('--bundletool',default='/tmp/philly-bundletool.jar');args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
props=dict(line.split('=',1) for line in (root/'release/upload-key.properties').read_text().splitlines() if '=' in line)
java='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java'
with tempfile.TemporaryDirectory(prefix='bundle-sign-',dir=root/'release/signing') as temp:
 store=Path(temp)/'store-pass';key=Path(temp)/'key-pass'
 store.write_text(props['storePassword']);key.write_text(props['keyPassword']);os.chmod(store,0o600);os.chmod(key,0o600)
 command=[java,'-jar',args.bundletool,'build-apks','--bundle='+str(root/'app/build/outputs/bundle/release/app-release.aab'),'--output='+str(root/'release/validation/universal.apks'),'--mode=universal','--overwrite','--ks='+str(root/props['storeFile']),'--ks-key-alias='+props['keyAlias'],'--ks-pass=file:'+str(store),'--key-pass=file:'+str(key)]
 r=subprocess.run(command,capture_output=True,text=True,check=True)
 (root/'release/validation/build-universal.log').write_text(r.stdout+r.stderr)
with zipfile.ZipFile(root/'release/validation/universal.apks') as z:(root/'release/NEW_LOOK-1.1.0-universal.apk').write_bytes(z.read('universal.apk'))
shutil.copy2(root/'app/build/outputs/bundle/release/app-release.aab',root/'release/NEW_LOOK-1.1.0.aab')
print('Prepared signed universal APK and AAB; temporary password files removed')
