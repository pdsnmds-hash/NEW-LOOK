"""Configure the existing Android SDK using its newest installed sdkmanager."""
import os
from pathlib import Path
import re
import subprocess


def revision(tool):
    properties = tool.parents[1] / 'source.properties'
    if properties.is_file():
        match = re.search(r'^Pkg\.Revision\s*=\s*(.+)$', properties.read_text(), re.M)
        if match:
            return tuple(int(x) for x in re.findall(r'\d+', match.group(1)))
    return (0,)


def main():
    sdk = Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
               or '/usr/local/share/android-sdk')
    candidates = list((sdk / 'cmdline-tools').glob('*/bin/sdkmanager'))
    candidates.append(sdk / 'tools/bin/sdkmanager')
    candidates = [p for p in candidates if p.is_file() and os.access(p, os.X_OK)]
    if not candidates:
        raise SystemExit(f'sdkmanager executable not found under Android SDK: {sdk}')
    tool = max(candidates, key=revision)
    print(f'Using sdkmanager: {tool} (revision {revision(tool)})', flush=True)
    # Android's repository publishes API 37 as android-37.0, not android-37.
    packages = ['platforms;android-37.0', 'build-tools;36.0.0']
    print('Installing SDK packages: ' + ', '.join(packages), flush=True)
    subprocess.run([str(tool), f'--sdk_root={sdk}', *packages], check=True)
    (Path(os.environ['CM_BUILD_DIR']) / 'local.properties').write_text(
        'sdk.dir=' + str(sdk).replace('\\', '\\\\') + '\n')


if __name__ == '__main__':
    main()
