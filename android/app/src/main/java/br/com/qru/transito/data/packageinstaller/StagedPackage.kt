package br.com.qru.transito.data.packageinstaller
data class StagedPackage(
    val payload:ByteArray,
    val fingerprint:String,
    val signature:String,
    val downloadedAt:Long=System.currentTimeMillis()
)
