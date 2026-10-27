from pathlib import Path
import re,html
strings={}
for p in Path('app/src/main/java').rglob('*.kt'):
 text=p.read_text()
 for a,b in re.findall(r'l\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*,?\s*\)',text):
  import json
  a=json.loads('"'+a+'"');b=json.loads('"'+b+'"');h=0
  for ch in a:h=(31*h+ord(ch))&0xffffffff
  key='ui_'+format(h,'x');strings[key]=(a,b)
for loc,idx in [('values',0),('values-en',1)]:
 lines=['<resources>','<string name="app_name">NEW LOOK</string>','<string name="reminders">'+('Напомняния' if idx==0 else 'Reminders')+'</string>','<string name="complete">'+('Завърши' if idx==0 else 'Complete')+'</string>']
 for key,pair in strings.items():
  value=html.escape(pair[idx],quote=False).replace("'","\\'").replace('"','\\"').replace('\n','\\n')
  lines.append(f'<string name="{key}" formatted="false">{value}</string>')
 lines.append('</resources>');Path(f'app/src/main/res/{loc}/strings.xml').write_text('\n'.join(lines))
print(len(strings),'localized UI strings')
