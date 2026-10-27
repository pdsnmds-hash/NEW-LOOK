from pathlib import Path
import json, zipfile
root=Path(__file__).resolve().parents[2]
out=root/'docs/refactoring'
blocks=json.loads((out/'blocks.json').read_text())
with zipfile.ZipFile(out/'before.zip') as z:
    old=z.read('app/src/main/java/bg/newlook/ui/Editors.kt').decode()
common='@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n\npackage bg.newlook.ui\n\n'
imports={
 'TaskEditor.kt': ['androidx.compose.foundation.layout.*','androidx.compose.material3.*','androidx.compose.runtime.*','androidx.compose.runtime.saveable.rememberSaveable','androidx.compose.ui.Modifier','androidx.compose.ui.platform.LocalContext','androidx.compose.ui.semantics.contentDescription','androidx.compose.ui.semantics.semantics','androidx.compose.ui.unit.dp','bg.newlook.data.*','bg.newlook.domain.*','java.time.*','kotlinx.coroutines.*'],
 'ProjectEditor.kt': ['androidx.compose.material3.*','androidx.compose.runtime.*','androidx.compose.runtime.saveable.rememberSaveable','bg.newlook.data.*','bg.newlook.domain.*','java.time.LocalDate'],
 'EventEditor.kt': ['androidx.compose.foundation.layout.*','androidx.compose.material3.*','androidx.compose.runtime.*','androidx.compose.runtime.saveable.rememberSaveable','androidx.compose.ui.Modifier','androidx.compose.ui.unit.dp','bg.newlook.data.*','bg.newlook.domain.*','java.time.*'],
 'NoteEditor.kt': ['androidx.compose.foundation.layout.*','androidx.compose.material3.*','androidx.compose.runtime.*','androidx.compose.runtime.saveable.rememberSaveable','androidx.compose.ui.Modifier','androidx.compose.ui.unit.dp','androidx.lifecycle.Lifecycle','androidx.lifecycle.LifecycleEventObserver','androidx.lifecycle.compose.LocalLifecycleOwner','bg.newlook.data.*','bg.newlook.domain.*','kotlinx.coroutines.*'],
}
headers={}
for file, names in imports.items():
    path='app/src/main/java/bg/newlook/ui/'+file
    header=common+''.join('import '+i+'\n' for i in names)+'\n'
    parts=[]
    for b in blocks:
        if b['target']!=path: continue
        a=old.index(b['start']);end=old.index(b['end'],a) if b['end'] else len(old)
        parts.append(old[a:end])
    target=root/path
    assert not target.exists(),f'Will not overwrite {target}'
    target.write_text(header+''.join(parts))
    headers[path]=header
    # Each source block is removed literally, preserving every remaining declaration.
    source=root/'app/src/main/java/bg/newlook/ui/Editors.kt'
    s=source.read_text()
    for part in parts:
        assert s.count(part)==1
        s=s.replace(part,'',1)
    source.write_text(s)
removals=['import androidx.lifecycle.Lifecycle\n','import androidx.lifecycle.LifecycleEventObserver\n','import androidx.lifecycle.compose.LocalLifecycleOwner\n','import androidx.compose.ui.semantics.contentDescription\n','import androidx.compose.ui.semantics.semantics\n','import kotlinx.coroutines.*\n']
p=root/'app/src/main/java/bg/newlook/ui/Editors.kt';s=p.read_text()
for line in removals: s=s.replace(line,'',1)
p.write_text(s)
headers['app/src/main/java/bg/newlook/data/Entities.kt']='package bg.newlook.data\n\nimport androidx.room.*\nimport java.util.UUID\n\n'
(out/'headers.json').write_text(json.dumps(headers,indent=2)+'\n')
(out/'removed-imports.json').write_text(json.dumps({'app/src/main/java/bg/newlook/ui/Editors.kt':removals,'app/src/main/java/bg/newlook/data/Database.kt':['import java.util.UUID\n']},indent=2)+'\n')
