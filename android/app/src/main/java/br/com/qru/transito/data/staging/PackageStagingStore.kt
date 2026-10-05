package br.com.qru.transito.data.staging
import android.content.Context
import br.com.qru.transito.data.packageinstaller.StagedPackage
import java.io.File

class PackageStagingStore(context:Context){
    private val dir=File(context.filesDir,"legal-staging").apply{mkdirs()}
    private val payload=File(dir,"candidate.json")
    private val meta=File(dir,"candidate.meta")

    fun write(stage:StagedPackage){
        val tmp=File(dir,"candidate.tmp")
        tmp.writeBytes(stage.payload)
        if(!tmp.renameTo(payload)){ tmp.copyTo(payload,true); tmp.delete() }
        meta.writeText(stage.fingerprint+"\n"+stage.signature,Charsets.UTF_8)
    }
    fun read():StagedPackage?{
        if(!payload.exists()||!meta.exists())return null
        val lines=meta.readLines()
        if(lines.size<2)return null
        return StagedPackage(payload.readBytes(),lines[0],lines[1])
    }
    fun clear(){payload.delete();meta.delete()}
}
