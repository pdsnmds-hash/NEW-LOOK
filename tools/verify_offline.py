"""Compile Kotlin and run domain JUnit without Gradle's socket service.
This is source verification, not an Android APK build or a replacement for Gradle.
Requires already cached Maven dependencies. Writes only validation/offline.
"""
from pathlib import Path
import subprocess,zipfile,re,os,json
root=Path(__file__).resolve().parents[1];os.chdir(root)
cache=root/'.gradle-local/caches/modules-2/files-2.1'
out=root/'validation/offline';out.mkdir(parents=True,exist_ok=True)
java='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java'
javac=str(Path(java).with_name('javac'))
def version(p):return tuple(int(n) if n.isdigit() else 0 for n in re.split(r'[.-]',p.name))
def latest(group,module,suffix='jar'):
 ps=list((cache/group/module).glob('*/*/*.'+suffix));return max(ps,key=lambda p:version(p.parents[1]))
def run(args,log):
 p=subprocess.run([str(a) for a in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(out/log).write_text(p.stdout);print(p.stdout[-18000:]);return p.returncode
cp=[]
for group in cache.iterdir():
 if not (group.name.startswith('androidx.') or group.name in ['org.jetbrains.kotlin','org.jetbrains.kotlinx','com.google.code.gson','org.jetbrains','junit','org.hamcrest','com.google.guava','androidx.concurrent']):continue
 for module in group.iterdir():
  if any(list((group/(module.name+suffix)).glob("*/*/*.jar"))+list((group/(module.name+suffix)).glob("*/*/*.aar")) for suffix in ["-jvm","-android"]):continue
  if group.name=='org.jetbrains.kotlin' and not module.name.startswith(('kotlin-stdlib','kotlin-parcelize-runtime')):continue
  if 'compiler' in module.name or 'gradle' in module.name or 'lint' in module.name or 'ksp' in module.name:continue
  artifacts=list(module.glob('*/*/*.aar'))+list(module.glob('*/*/*.jar'))
  if not artifacts:continue
  newest=max(artifacts,key=lambda p:version(p.parents[1]));
  if newest.suffix=='.aar':
   target=out/'jars'/f'{module.name}-{newest.parents[1].name}.jar';target.parent.mkdir(exist_ok=True)
   with zipfile.ZipFile(newest) as z:
    if 'classes.jar' in z.namelist():target.write_bytes(z.read('classes.jar'));cp.append(str(target))
  else:cp.append(str(newest))
android=Path('/Users/nomads_dm/Library/Android/sdk/platforms/android-36/android.jar');cp.append(str(android))
# Symbol stub is used only to check source typing; Android resources remain Gradle's responsibility.
strings=(root/'app/src/main/res/values/strings.xml').read_text();names=re.findall(r'name="([^"]+)"',strings)
source=out/'R.java';source.write_text('package bg.newlook; public final class R { public static final class string {'+''.join(f'public static final int {n}={i+1};' for i,n in enumerate(names))+'} public static final class font { public static final int manrope=1; } public static final class drawable { public static final int brand_icon=1; }}')
classes=out/'classes';classes.mkdir(exist_ok=True)
build_config=out/'BuildConfig.java';build_config.write_text('package bg.newlook; public final class BuildConfig { public static final String VERSION_NAME="1.1.0"; public static final int VERSION_CODE=2; }')
assert run([javac,'-d',classes,source,build_config],'javac.log')==0
cp.append(str(classes));classpath=':'.join(cp);(out/'classpath.txt').write_text(classpath)
compiler=':'.join(str(latest('org.jetbrains.kotlin',m)) for m in ['kotlin-compiler-embeddable','kotlin-stdlib','kotlin-script-runtime','kotlin-reflect','kotlin-daemon-embeddable'])+':'+str(latest('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm'))+':'+str(latest('org.jetbrains','annotations'))
compose=latest('org.jetbrains.kotlin','kotlin-compose-compiler-plugin-embeddable')
args=[java,'-Xmx3g','-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',classpath,'-Xplugin='+str(compose),'-d',str(classes)]+[str(p) for folder in [root/'app/src/main/java',root/'validation/ksp/kotlin'] for p in folder.rglob('*.kt')]
result=run(args,'compile.log')
if result:raise SystemExit(result)
tests=list((root/'app/src/test').rglob('*.kt'))
if tests:
 result=run([java,'-Xmx2g','-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',classpath,'-d',classes]+tests,'test-compile.log')
 if result:raise SystemExit(result)
 raise SystemExit(run([java,'-cp',classpath,'org.junit.runner.JUnitCore','bg.newlook.domain.RulesTest'],'tests.log'))
