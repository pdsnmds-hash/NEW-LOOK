"""NEW LOOK-specific byte comparisons; not the Safora verifier."""
from pathlib import Path
import json, zipfile, hashlib, difflib
root=Path(__file__).resolve().parents[2]
out=root/'docs/refactoring'
blocks=json.loads((out/'blocks.json').read_text())
headers=json.loads((out/'headers.json').read_text())
removals=json.loads((out/'removed-imports.json').read_text())
with zipfile.ZipFile(out/'before.zip') as z:
    before={name:z.read(name) for name in z.namelist()}
parts={}; results=[]; residuals={p:before[p].decode() for p in removals}
for b in blocks:
    src=before[b['source']].decode()
    a=src.index(b['start']);end=src.index(b['end'],a) if b['end'] else len(src)
    block=src[a:end]
    sha=hashlib.sha256(block.encode()).hexdigest()
    assert sha==b['sha256'],f"Baseline mismatch: {b['name']}"
    assert (root/b['target']).read_bytes().count(block.encode())==1,f"Non-verbatim or duplicated block: {b['name']}"
    parts.setdefault(b['target'],[]).append(block)
    residuals[b['source']]=residuals[b['source']].replace(block,'',1)
    results.append(dict(name=b['name'],source=b['source'],target=b['target'],bytes=b['bytes'],before_sha256=sha,after_sha256=sha,equal=True))
for path, chunks in parts.items():
    assert (root/path).read_bytes()==(headers[path]+''.join(chunks)).encode(),f"Unexpected content: {path}"
for path,expected in residuals.items():
    for line in removals[path]: expected=expected.replace(line,'',1)
    assert (root/path).read_bytes()==expected.encode(),f"Source body changed: {path}"
unchanged=0
for path,data in before.items():
    if path in residuals: continue
    assert (root/path).read_bytes()==data,f"Unrelated change: {path}"
    unchanged+=1
baseline_sources={p for p in before if p.startswith('app/src/')}
current_sources={str(p.relative_to(root)) for p in (root/'app/src').rglob('*') if p.is_file()}
assert current_sources-baseline_sources==set(headers), 'Unexpected new source/resource/test files'
assert not (baseline_sources-current_sources), 'Unexpected deleted source/resource/test files'
report=dict(project='NEW LOOK',comparison='UTF-8 bytes, no whitespace/comment normalization',blocks=results,unchanged_archived_files=unchanged,source_residuals_exact=True,new_files_only_headers_and_verbatim_blocks=True)
(out/'preservation.json').write_text(json.dumps(report,indent=2)+'\n')
patch=[]
for path in sorted(set(residuals)|set(headers)):
    old=before.get(path,b'').decode().splitlines(keepends=True)
    new=(root/path).read_text().splitlines(keepends=True)
    patch.extend(difflib.unified_diff(old,new,fromfile='a/'+path if old else '/dev/null',tofile='b/'+path))
(out/'refactor.patch').write_text(''.join(patch))
print(f'PASS: {len(results)} verbatim blocks; {unchanged} other archived files unchanged; exact source residuals and target contents')
