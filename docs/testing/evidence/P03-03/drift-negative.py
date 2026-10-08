from pathlib import Path
import subprocess,json,hashlib
root=Path(__file__).resolve().parents[4]
file=root/'packages/api-contracts/openapi/backend.openapi.json'
original=file.read_bytes()
before=hashlib.sha256(original).hexdigest()
try:
 doc=json.loads(original)
 doc['components']['schemas']['PageResponseFieldErrorDetail']['properties']['total']['type']='number'
 file.write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n')
 injected=file.read_bytes()
 result=subprocess.run(['python3',str(Path(__file__).with_name('run-check.py')),'drift-negative','.', 'pnpm','contracts:check'],cwd=root)
 assert result.returncode==1, result.returncode
 assert file.read_bytes()==injected,'check不应覆盖注入的产物'
finally:
 file.write_bytes(original)
assert hashlib.sha256(file.read_bytes()).hexdigest()==before
Path(__file__).with_name('drift-negative-proof.json').write_text(json.dumps({'injectedChange':'total string -> number in production snapshot','expectedExitCode':1,'snapshotNotOverwritten':True,'restoredSHA256':before},indent=2)+'\n')
