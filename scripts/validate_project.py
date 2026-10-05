from pathlib import Path
import sys,re,json
root=Path(__file__).resolve().parents[1]
required=[
"android/settings.gradle.kts","android/build.gradle.kts","android/app/build.gradle.kts",
"android/app/src/main/AndroidManifest.xml",
"android/app/src/main/java/br/com/qru/transito/MainActivity.kt",
"android/app/src/main/java/br/com/qru/transito/QruApplication.kt",
"android/app/src/main/java/br/com/qru/transito/ui/QruApp.kt",
"android/app/src/main/java/br/com/qru/transito/data/local/QruDatabase.kt",
"android/app/src/main/java/br/com/qru/transito/data/local/QruMigrations.kt",
"android/app/src/main/java/br/com/qru/transito/network/LegalReleaseApi.kt",
]
missing=[x for x in required if not (root/x).exists()]
manifest=(root/"android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
app=(root/"android/app/build.gradle.kts").read_text(encoding="utf-8")
checks={
 "required_files":not missing,
 "application_class":'android:name=".QruApplication"' in manifest,
 "launcher_activity":'android:name=".MainActivity"' in manifest,
 "record_audio_permission":"android.permission.RECORD_AUDIO" in manifest,
 "internet_permission":"android.permission.INTERNET" in manifest,
 "namespace":'namespace = "br.com.qru.transito"' in app,
 "compile_sdk_35":"compileSdk = 35" in app,
 "min_sdk_28":"minSdk = 28" in app,
}
result={"missing":missing,"checks":checks,"passed":all(checks.values())}
print(json.dumps(result,indent=2,ensure_ascii=False))
sys.exit(0 if result["passed"] else 1)
