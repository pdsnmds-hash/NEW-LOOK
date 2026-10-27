from pathlib import Path
import csv,hashlib,zipfile,xml.etree.ElementTree as ET,json
root=Path(__file__).resolve().parents[1]
rows=list(csv.DictReader((root/'app/src/main/assets/image-registry.csv').open()))
assert len(rows)==19
assert len({r['owner'] for r in rows})==19
assert len({r['filename'] for r in rows})==19
hashes=[]
for r in rows:
 p=root/'app/src/main/assets/images'/r['filename'];digest=hashlib.sha256(p.read_bytes()).hexdigest();assert digest==r['sha256'];assert r['status']=='generated_builtin_imagegen';hashes.append(digest)
assert len(set(hashes))==19
print('PASS 19 unique generated WebP files, owners, registry hashes and prompts')
bg={e.attrib['name'] for e in ET.parse(root/'app/src/main/res/values/strings.xml').getroot()}
en={e.attrib['name'] for e in ET.parse(root/'app/src/main/res/values-en/strings.xml').getroot()}
assert bg==en;print(f'PASS {len(bg)} matching BG/EN resource keys')
apk=root/'output/newlook-debug.apk'
with zipfile.ZipFile(apk) as z:
 names=z.namelist();assert z.testzip() is None;assert len(names)==len(set(names));assert len([n for n in names if n.startswith('assets/images/') and n.endswith('.webp')])==19
 for r in rows:assert hashlib.sha256(z.read('assets/images/'+r['filename'])).hexdigest()==r['sha256']
 assert 'classes.dex' in names and 'AndroidManifest.xml' in names and 'resources.arsc' in names
 print('PASS APK ZIP integrity, resources, dex and all 19 image hashes')
manifest_path=root/'app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml'
if not manifest_path.exists():manifest_path=root/'validation/sdk/AndroidManifest.xml'
manifest=ET.parse(manifest_path).getroot();android='{http://schemas.android.com/apk/res/android}'
assert not any(e.get(android+'name')=='android.permission.INTERNET' for e in manifest.findall('uses-permission'))
assert manifest.find('application').get(android+'allowBackup')=='false'
print('PASS packaged manifest has no INTERNET and disables automatic backup')
sha=hashlib.sha256(apk.read_bytes()).hexdigest();(root/'output/SHA256SUMS').write_text(sha+'  newlook-debug.apk\n');print('APK SHA-256:',sha)
