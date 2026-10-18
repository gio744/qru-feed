package br.com.qru.transito.ui

/** Bounds layout growth; invalid gesture samples must not corrupt the reader. */
internal fun boundedPdfZoom(current:Float,factor:Float):Float {
 val safeCurrent=if(current.isFinite()) current.coerceIn(1f,4f) else 1f
 if(!factor.isFinite() || factor<=0f) return safeCurrent
 return (safeCurrent*factor).coerceIn(1f,4f)
}
