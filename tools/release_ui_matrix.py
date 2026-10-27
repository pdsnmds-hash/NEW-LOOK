from pathlib import Path
import subprocess,json,argparse
parser=argparse.ArgumentParser();parser.add_argument('--width',type=int);args=parser.parse_args()
root=Path(__file__).resolve().parents[1];adb=['adb','-s','emulator-5556'];results=[]
try:
 for width in ([args.width] if args.width else [411,380,320]):
  subprocess.run(adb+['shell','wm','size',f'{width*3}x2400'],check=True)
  subprocess.run(adb+['shell','wm','density','480'],check=True)
  for locale in ['en','bg']:
   log=root/f'release/validation/ui-{width}-{locale}.log'
   with log.open('w') as f: subprocess.run(adb+['shell','am','instrument','-w','-r','-e','width',str(width),'-e','locale',locale,'-e','class','bg.newlook.ResponsiveReleaseTest#auditPhoneSectionsAndEditors,bg.newlook.ResponsiveReleaseTest#firstLaunchUsesSafeInsets','bg.newlook.organizer.test/androidx.test.runner.AndroidJUnitRunner'],stdout=f,stderr=subprocess.STDOUT,check=True)
   passed='OK (2 tests)' in log.read_text();results.append({'width_dp':width,'locale':locale,'passed':passed})
   (root/'release/validation/responsive-results.json').write_text(json.dumps(results,indent=2)+'\n');print(('PASS' if passed else 'FAIL'),width,locale,flush=True)
   if not passed:raise RuntimeError(f'Inspect {log}')
finally:
 subprocess.run(adb+['shell','wm','size','reset'])
 subprocess.run(adb+['shell','wm','density','reset'])
