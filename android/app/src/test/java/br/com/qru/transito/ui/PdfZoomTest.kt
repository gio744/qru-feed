package br.com.qru.transito.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfZoomTest {
 @Test fun pinchCanEnlargeAndReduceContinuously() {
  assertEquals(1.5f,boundedPdfZoom(1f,1.5f),0.0001f)
  assertEquals(1.5f,boundedPdfZoom(3f,0.5f),0.0001f)
 }
 @Test fun cannotShrinkBelowPageWidthOrGrowPastLimit() {
  assertEquals(1f,boundedPdfZoom(1f,0.1f),0f)
  assertEquals(4f,boundedPdfZoom(3f,2f),0f)
 }
 @Test fun invalidSamplesDoNotPoisonScale() {
  listOf(Float.NaN,Float.POSITIVE_INFINITY,0f,-1f).forEach {
   assertEquals(2f,boundedPdfZoom(2f,it),0f)
  }
  assertEquals(1f,boundedPdfZoom(Float.NaN,1f),0f)
 }
 @Test fun buttonsAndGesturesUseSameLimits() {
  var scale=1f
  repeat(30) { scale=boundedPdfZoom(scale,1.25f) }
  assertEquals(4f,scale,0f)
  repeat(30) { scale=boundedPdfZoom(scale,1f/1.25f) }
  assertEquals(1f,scale,0f)
 }
}
