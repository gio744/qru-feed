package br.com.qru.transito.ui

import java.net.URI

internal data class OfflinePdfDocument(
 val file:String="",val title:String="",val subtitle:String="",
 val source:String="",val sha256:String="",val pageCount:Int=0,val annexPage:Int?=null,
 val category:String="Resoluções"
)
internal data class OfflineDocumentTarget(val document:OfflinePdfDocument,val page:Int)

/** Match the exact bundled publication. Never substitute an older manual for a newer source. */
internal fun resolveOfflineDocument(documents:List<OfflinePdfDocument>,source:String):OfflineDocumentTarget? {
 val uri=runCatching { URI(source) }.getOrNull() ?: return null
 if(!uri.scheme.equals("https",ignoreCase=true) || uri.host==null || uri.userInfo!=null ||
    uri.port !in listOf(-1,443) || uri.rawQuery!=null) return null
 val page=if(uri.rawFragment==null) 1 else {
  val number=Regex("page=([0-9]+)").matchEntire(uri.rawFragment)?.groupValues?.get(1) ?: return null
  number.toIntOrNull() ?: return null
 }
 val matches=documents.filter { doc ->
  val original=runCatching { URI(doc.source) }.getOrNull()
  original!=null && original.scheme.equals("https",ignoreCase=true) && original.userInfo==null &&
   original.port in listOf(-1,443) && original.rawQuery==null && original.rawFragment==null &&
   uri.host.equals(original.host,ignoreCase=true) && uri.rawPath==original.rawPath &&
   doc.file.matches(Regex("[A-Za-z0-9]+\\.pdf")) && doc.sha256.matches(Regex("[a-f0-9]{64}")) &&
   page in 1..doc.pageCount
 }
 // An ambiguous manifest must never select an arbitrary PDF for an official URL.
 if(matches.size!=1) return null
 return OfflineDocumentTarget(matches.single(),page)
}
