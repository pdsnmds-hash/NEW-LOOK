"""Resolve the application's cached Maven artifacts for a socket-free SDK build.
The normal, supported project entry point remains ./gradlew assembleDebug.
"""
from pathlib import Path
import re,json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];cache=root/'.gradle-local/caches/modules-2/files-2.1'
ns={'m':'http://maven.apache.org/POM/4.0.0'}
def version(v):return tuple(int(x) if x.isdigit() else 0 for x in re.split('[.-]',v))
def coord(g,m,v):
 v=v.strip('[]()')
 if m.endswith('-jvm') and (cache/g/(m[:-4]+'-android')/v).exists():m=m[:-4]+'-android'
 
 for family,pinned in [('androidx.lifecycle','2.10.0'),('androidx.activity','1.13.0'),('androidx.savedstate','1.4.0'),('androidx.collection','1.5.0')]:
  if g==family and (cache/g/m/pinned).exists() and version(pinned)>version(v):v=pinned
 folder=cache/g/m/v
 if not folder.exists():
  candidates=[p.name for p in (cache/g/m).glob('*') if p.is_dir() and version(p.name)>=version(v)]
  if not candidates:raise ValueError(f'Missing cached dependency {g}:{m}:{v}')
  nextv=min(candidates,key=version);print(f'Cached compatible selection: {g}:{m} {v} -> {nextv}');v=nextv
 for suffix in ['-android','-jvm']:
  other=cache/g/(m+suffix)/v
  if list(other.glob('*/*.aar')) or list(other.glob('*/*.jar')):return g,m+suffix,v
 return g,m,v
bom=next((cache/'androidx.compose/compose-bom/2026.06.01').glob('*/*.pom'))
constraints={}
for d in ET.parse(bom).findall('.//m:dependencyManagement/m:dependencies/m:dependency',ns):
 constraints[(d.findtext('m:groupId',namespaces=ns),d.findtext('m:artifactId',namespaces=ns))]=d.findtext('m:version',namespaces=ns)
roots=[('androidx.activity','activity-compose','1.13.0'),('androidx.core','core-ktx','1.18.0'),('androidx.compose.material3','material3',constraints[('androidx.compose.material3','material3')]),('androidx.navigation','navigation-compose','2.9.8'),('androidx.lifecycle','lifecycle-viewmodel-compose','2.10.0'),('androidx.lifecycle','lifecycle-runtime-compose','2.10.0'),('androidx.datastore','datastore-preferences','1.2.1'),('androidx.room','room-runtime','2.8.5'),('androidx.room','room-ktx','2.8.5'),('androidx.work','work-runtime-ktx','2.12.0'),('org.jetbrains.kotlinx','kotlinx-coroutines-android','1.11.0'),('com.google.code.gson','gson','2.11.0'),('org.jetbrains.kotlin','kotlin-stdlib','2.4.21')]
resolved={};queue=list(roots)
while queue:
 g,m,v=queue.pop(0)
 v=constraints.get((g,m),v);g,m,v=coord(g,m,v)
 prior=resolved.get((g,m))
 if prior and version(prior)>=version(v):continue
 resolved[g,m]=v
 pom=next((cache/g/m/v).glob('*/*.pom'),None)
 if not pom:continue
 tree=ET.parse(pom);properties={p.tag.split('}')[-1]:p.text for p in tree.findall('./m:properties/*',ns)};properties.update({'project.version':v,'pom.version':v})
 def replace(s):
  if s and s.startswith('${'):return properties.get(s[2:-1],s)
  return s
 management={}
 for d in tree.findall('./m:dependencyManagement/m:dependencies/m:dependency',ns):management[(d.findtext('m:groupId',namespaces=ns),d.findtext('m:artifactId',namespaces=ns))]=replace(d.findtext('m:version',namespaces=ns))
 for d in tree.findall('./m:dependencies/m:dependency',ns):
  if d.findtext('m:scope',namespaces=ns) in ['test','provided'] or d.findtext('m:optional',namespaces=ns)=='true':continue
  dg=d.findtext('m:groupId',namespaces=ns);dm=d.findtext('m:artifactId',namespaces=ns);dv=replace(d.findtext('m:version',namespaces=ns)) or management.get((dg,dm))
  if dv:queue.append((dg,dm,dv))
  else:raise ValueError(f'Unresolved dependency version: {dg}:{dm} in {g}:{m}')
artifacts=[]
for (g,m),v in resolved.items():
 if any((g,m+suffix) in resolved for suffix in ['-android','-jvm']):continue
 files=list((cache/g/m/v).glob('*/*.aar'))+list((cache/g/m/v).glob('*/*.jar'))
 if files:artifacts.append({'group':g,'module':m,'version':v,'path':str(files[0])})
(root/'validation/resolved-artifacts.json').write_text(json.dumps(artifacts,indent=2));print(len(artifacts),'resolved artifacts')
