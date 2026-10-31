package br.com.qru.transito.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device-level offline smoke test: uses only APK assets and Android's PdfRenderer. */
@RunWith(AndroidJUnit4::class)
class OfflineLibraryDeviceTest {
 @Test fun everyBundledPdfOpensAndRendersItsFirstPageWithoutNetwork() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val catalog=context.assets.open("norms/pdf-catalog.json").bufferedReader().use {
   JsonParser.parseReader(it).asJsonArray
  }
  assertTrue(catalog.size()>0)
  for (entry in catalog) {
   val doc=entry.asJsonObject
   val name=doc["file"].asString
   val expected=doc["sha256"].asString
   val target=File(context.cacheDir,"device-check-$name")
   try {
    context.assets.open("norms/$name").use { input ->
     target.outputStream().use { output -> input.copyTo(output) }
    }
    val actual=MessageDigest.getInstance("SHA-256").digest(target.readBytes())
     .joinToString("") { "%02x".format(it) }
    assertEquals("Integrity: $name",expected,actual)
    ParcelFileDescriptor.open(target,ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
     PdfRenderer(descriptor).use { renderer ->
      assertEquals("Page count: $name",doc["pageCount"].asInt,renderer.pageCount)
      renderer.openPage(0).use { page ->
       val bitmap=Bitmap.createBitmap(256,
        (256f*page.height/page.width).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
       try {
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
       } finally { bitmap.recycle() }
      }
     }
    }
   } finally { target.delete() }
  }
 }
}
