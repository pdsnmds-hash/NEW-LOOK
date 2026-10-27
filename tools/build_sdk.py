"""Fallback Android SDK build without Gradle services or networking.
Uses the standard AAPT2, Kotlin/Compose compiler, generated Room code, D8,
zipalign and apksigner. Does not imply assembleDebug or device tests passed.
"""
from pathlib import Path
import subprocess,zipfile,json,shutil,xml.etree.ElementTree as ET,os,re
root=Path(__file__).resolve().parents[1];os.chdir(root)
out=root/'validation/sdk';out.mkdir(exist_ok=True)
cache=root/'.gradle-local/caches/modules-2/files-2.1'
sdk=Path('/Users/nomads_dm/Library/Android/sdk');bt=sdk/'build-tools/36.0.0';android=sdk/'platforms/android-36/android.jar';jhome=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home');java=jhome/'bin/java';javac=jhome/'bin/javac'
def run(args,name):
 env=dict(os.environ);env['JAVA_OPTS']='-Xmx3g'
 p=subprocess.run([str(a) for a in args],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(out/(name+'.log')).write_text(p.stdout);print(name, p.returncode,flush=True)
 if p.returncode:print(p.stdout[-14000:]);raise SystemExit(p.returncode)
 return p.stdout
def latest(g,m):
 return max((cache/g/m).glob('*/*/*.jar'),key=lambda p:tuple(int(x) if x.isdigit() else 0 for x in re.split('[.-]',p.parents[1].name)))
artifacts=json.loads((root/'validation/resolved-artifacts.json').read_text());jars=[];manifests=[];resources=[];packages=set();native=[]
for a in reversed(artifacts):
 f=Path(a['path'])
 if f.suffix=='.jar':jars.append(f);continue
 folder=out/'aar'/a['module'];folder.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(f) as z:z.extractall(folder)
 if (folder/'classes.jar').exists():jars.append(folder/'classes.jar')
 jars+=list((folder/'libs').glob('*.jar'))
 manifest=folder/'AndroidManifest.xml'
 if manifest.exists():
  manifests.append(manifest);package=ET.parse(manifest).getroot().get('package')
  if package:packages.add(package)
 if (folder/'res').exists():
  target=folder/'compiled.zip';run([bt/'aapt2','compile','--dir',folder/'res','-o',target],a['module']+'-resources');resources.append(target)
 for lib in (folder/'jni').rglob('*.so'):native.append((lib,lib.relative_to(folder/'jni')))
# Use the official Android manifest merger.
tooljars=[]
for group in cache.iterdir():
 if group.name.startswith('com.android.tools') or group.name in ['com.google.guava','com.google.code.gson','org.jetbrains.kotlin','org.jetbrains','net.sf.kxml','org.bouncycastle']:
  for module in group.iterdir():
   if group.name=='org.jetbrains.kotlin' and module.name!='kotlin-stdlib':continue
   ps=list((module/'31.13.2').glob('*/*.jar')) or list(module.glob('*/*/*.jar'))
   if ps:tooljars.append(max(ps,key=lambda p:p.stat().st_size))
toolcp=':'.join(map(str,tooljars));merger=out/'Merge.java'
merger.write_text('''import com.android.manifmerger.*;import com.android.utils.*;import java.io.*;import java.nio.file.*;import java.util.*;
public class Merge {public static void main(String[] a)throws Exception{
var merger=ManifestMerger2.newMerger(new File(a[0]),new StdLogger(StdLogger.Level.INFO),ManifestMerger2.MergeType.APPLICATION);
for(int i=2;i<a.length;i++)merger.addLibraryManifest(new File(a[i]));

merger.setOverride(ManifestSystemProperty.valueOf("PACKAGE"),"bg.newlook.organizer");
merger.setOverride(ManifestSystemProperty.valueOf("MIN_SDK_VERSION"),"26");
merger.setOverride(ManifestSystemProperty.valueOf("TARGET_SDK_VERSION"),"36");
merger.setOverride(ManifestSystemProperty.valueOf("VERSION_CODE"),"2");
merger.setOverride(ManifestSystemProperty.valueOf("VERSION_NAME"),"1.1.0");
var report=merger.merge();if(report.getResult()==MergingReport.Result.ERROR)throw new RuntimeException(report.getReportString());
Files.writeString(Path.of(a[1]),report.getMergedDocument(MergingReport.MergedManifestKind.MERGED));}}
''')
run([javac,'-cp',toolcp,'-d',out,merger],'manifest-merger-compile')
manifest=out/'AndroidManifest.xml';run([java,'-cp',str(out)+':'+toolcp,'Merge',root/'app/src/main/AndroidManifest.xml',manifest]+manifests,'manifest-merge')
# Namespace is bg.newlook, applicationId is bg.newlook.organizer.
appres=out/'app-res.zip';run([bt/'aapt2','compile','--dir',root/'app/src/main/res','-o',appres],'app-resources')
rjava=out/'rjava';rjava.mkdir(exist_ok=True);resource_apk=out/'resources.apk'
args=[bt/'aapt2','link','-o',resource_apk,'-I',android,'--manifest',manifest,'--java',rjava,'--custom-package','bg.newlook','--extra-packages',':'.join(sorted(packages)),'--auto-add-overlay','-A',root/'app/src/main/assets']
for r in resources+[appres]:args+=['-R',r]
run(args,'resource-link')
classes=out/'classes';shutil.rmtree(classes,ignore_errors=True);classes.mkdir(exist_ok=True);run([javac,'-source','17','-target','17','-cp',android,'-d',classes]+list(rjava.rglob('*.java')),'r-compile')
build_config=out/'BuildConfig.java';build_config.write_text('package bg.newlook; public final class BuildConfig { public static final String VERSION_NAME="1.1.0"; public static final int VERSION_CODE=2; }');run([javac,'-d',classes,build_config],'build-config')
classpath=':'.join(map(str,jars+[android,classes]));(out/'classpath.txt').write_text(classpath)
compiler=':'.join(str(latest('org.jetbrains.kotlin',m)) for m in ['kotlin-compiler-embeddable','kotlin-stdlib','kotlin-script-runtime','kotlin-reflect','kotlin-daemon-embeddable'])+':'+str(latest('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm'))+':'+str(latest('org.jetbrains','annotations'))
compose=latest('org.jetbrains.kotlin','kotlin-compose-compiler-plugin-embeddable')
run([java,'-Xmx3g','-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',classpath,'-Xplugin='+str(compose),'-d',classes]+list((root/'app/src/main/java').rglob('*.kt'))+list((root/'validation/ksp/kotlin').rglob('*.kt')),'kotlin-compose')
program=out/'program.jar'
with zipfile.ZipFile(program,'w',zipfile.ZIP_DEFLATED) as z:
 for f in classes.rglob('*.class'):z.write(f,str(f.relative_to(classes)))
dex=out/'dex';shutil.rmtree(dex,ignore_errors=True);dex.mkdir(exist_ok=True)
run([bt/'d8','--debug','--min-api','26','--lib',android,'--output',dex,program]+jars,'dex')
unsigned=out/'unsigned.apk';shutil.copyfile(resource_apk,unsigned)
with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:
 for f in dex.glob('*.dex'):z.write(f,f.name)
 for f,rel in native:z.write(f,'lib/'+str(rel))
aligned=out/'aligned.apk';run([bt/'zipalign','-f','-P','16','4',unsigned,aligned],'zipalign')
key=out/'debug.keystore'
if not key.exists():run([jhome/'bin/keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Android Debug,O=Android,C=US','-keyalg','RSA','-keysize','2048','-validity','10000'],'debug-key')
final=root/'output/newlook-debug.apk';final.parent.mkdir(exist_ok=True)
run([bt/'apksigner','sign','--ks',key,'--ks-pass','pass:android','--key-pass','pass:android','--out',final,aligned],'sign')
run([bt/'apksigner','verify','--verbose',final],'signature-verify');run([bt/'aapt2','dump','badging',final],'apk-badging');print('SDK APK built:',final)
