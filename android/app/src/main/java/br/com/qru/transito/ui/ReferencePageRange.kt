package br.com.qru.transito.ui

internal data class PdfPageWindow(val firstPage:Int,val lastPage:Int,val documentPageCount:Int) {
 init { require(firstPage>=1 && firstPage<=lastPage && lastPage<=documentPageCount) }
 val count:Int get()=lastPage-firstPage+1
 fun documentIndex(listIndex:Int):Int {
  require(listIndex in 0 until count)
  return firstPage-1+listIndex
 }
 fun listIndex(sourcePage:Int):Int {
  require(sourcePage in firstPage..lastPage)
  return sourcePage-firstPage
 }
}
internal data class ReferencePageRange(
 val code:String="",val file:String="",val sha256:String="",val firstPage:Int=0,val lastPage:Int=0
)
internal data class OfflineReferenceTarget(val document:OfflinePdfDocument,val range:IntRange)

/** Requires the selected code, exact publication bytes and exact source anchor. */
internal fun resolveOfflineReference(documents:List<OfflinePdfDocument>,ranges:List<ReferencePageRange>,source:String,code:String?):OfflineReferenceTarget? {
 val target=resolveOfflineDocument(documents,source) ?: return null
 val range=ranges.singleOrNull {
  it.code==code && it.file==target.document.file && it.sha256==target.document.sha256 &&
  it.firstPage==target.page && it.firstPage>=1 && it.lastPage>=it.firstPage &&
  it.lastPage<=target.document.pageCount
 } ?: return null
 return OfflineReferenceTarget(target.document,range.firstPage..range.lastPage)
}
