from pathlib import Path
import hashlib,json,subprocess,sys,datetime,zipfile
if len(sys.argv)!=2:
    raise SystemExit("usage: register_apk.py <app-debug.apk>")
apk=Path(sys.argv[1])
if not apk.exists() or apk.stat().st_size==0:
    raise SystemExit("APK missing or empty")
try:
    with zipfile.ZipFile(apk) as z:
        bad=z.testzip()
        if bad: raise SystemExit("APK ZIP integrity failure: "+bad)
        names=set(z.namelist())
        if "AndroidManifest.xml" not in names:
            raise SystemExit("Not an Android APK: AndroidManifest.xml missing")
except zipfile.BadZipFile:
    raise SystemExit("Invalid APK ZIP container")
sha=hashlib.sha256(apk.read_bytes()).hexdigest()
record={
 "artifact":apk.name,
 "bytes":apk.stat().st_size,
 "sha256":sha,
 "registered_at_utc":datetime.datetime.now(datetime.timezone.utc).isoformat(),
 "integrity":"PASS",
 "install_status":"NOT_YET_DEVICE_TESTED"
}
out=apk.with_suffix(apk.suffix+".record.json")
out.write_text(json.dumps(record,indent=2),encoding="utf-8")
apk.with_suffix(apk.suffix+".sha256").write_text(sha+"  "+apk.name+"\n",encoding="utf-8")
print(json.dumps(record,indent=2))
