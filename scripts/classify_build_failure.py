import sys,re,json
text=sys.stdin.read()
patterns=[
 ("KOTLIN_COMPILE",r"(e: .*\.kt:|Compilation error)"),
 ("ANDROID_RESOURCE",r"(Android resource linking failed|AAPT:)"),
 ("MANIFEST",r"(Manifest merger failed|uses-sdk:)"),
 ("DEPENDENCY",r"(Could not resolve|Could not find .*:)"),
 ("ROOM_KSP",r"(KSP|Room.*error|Schema export)"),
 ("GRADLE_CONFIG",r"(Could not compile build file|Plugin .* was not found)"),
]
kind="UNKNOWN"
for name,p in patterns:
    if re.search(p,text,re.I|re.M): kind=name; break
lines=[x for x in text.splitlines() if re.search(r"(error|failed|exception| e: )",x,re.I)]
print(json.dumps({"classification":kind,"signals":lines[:25]},ensure_ascii=False,indent=2))
