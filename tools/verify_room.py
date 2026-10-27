"""Run cached KSP CLI to validate Room SQL and export its real schema, without Gradle."""
from pathlib import Path
import subprocess,re,os
root=Path(__file__).resolve().parents[1];os.chdir(root);cache=root/'.gradle-local/caches/modules-2/files-2.1';out=root/'validation/ksp';out.mkdir(exist_ok=True)
def latest(group,module):
 ps=list((cache/group/module).glob('*/*/*.jar'));return max(ps,key=lambda p:tuple(int(x) if x.isdigit() else 0 for x in re.split('[.-]',p.parents[1].name)))
modules={'androidx.room':['room-compiler','room-compiler-processing','room-common-jvm','room-migration-jvm','room-external-antlr'],'com.google.auto':['auto-common'],'com.google.auto.value':['auto-value-annotations'],'com.squareup':['kotlinpoet-jvm','kotlinpoet-javapoet','javapoet'],'com.google.guava':['guava','failureaccess'],'commons-codec':['commons-codec'],'org.xerial':['sqlite-jdbc'],'org.jetbrains.kotlin':['kotlin-stdlib','kotlin-metadata-jvm'],'org.jetbrains':['annotations'],'org.jspecify':['jspecify'],'org.jetbrains.kotlinx':['kotlinx-serialization-core-jvm','kotlinx-serialization-json-jvm']}
processor=':'.join(str(latest(g,m)) for g,ms in modules.items() for m in ms)
cp=(root/'validation/ksp-classpath.txt').read_text();libraries=(root/'validation/offline/classpath.txt').read_text()
args=['/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java','-Xmx3g','-cp',cp,'com.google.devtools.ksp.cmdline.KSPJvmMain','-jvm-target=17','-module-name=app','-source-roots='+str(root/'app/src/main/java'),'-libraries='+libraries,'-project-base-dir='+str(root),'-output-base-dir='+str(out),'-caches-dir='+str(out/'caches'),'-class-output-dir='+str(out/'classes'),'-kotlin-output-dir='+str(out/'kotlin'),'-java-output-dir='+str(out/'java'),'-resource-output-dir='+str(out/'resources'),'-language-version=2.4','-api-version=2.4','-processor-options=room.schemaLocation='+str(root/'app/schemas'),processor]
p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(out/'room.log').write_text(p.stdout);print(p.stdout);raise SystemExit(p.returncode)
