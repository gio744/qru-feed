package br.com.qru.transito.ui

import java.util.Locale

// Numeric article queries match the article heading, not citations or dates in its body.
internal fun matchesCtbParagraph(text:String,query:String):Boolean {
 val requested=Regex("""^(?:(?:art\.?|artigo)\s*)?(\d+)(?:\s*[-–]\s*([a-z]))?[º°.]?$""",
  RegexOption.IGNORE_CASE).matchEntire(query.trim())
 if(requested==null) return matchesNorm("",text,query)
 val heading=Regex("""^\s*Art\.?\s*(\d+)(?:\s*[-–]\s*([a-z]))?(?=[º°.\s]|$)""",
  RegexOption.IGNORE_CASE).find(text) ?: return false
 return heading.groupValues[1]==requested.groupValues[1] &&
  heading.groupValues[2].uppercase(Locale.ROOT)==requested.groupValues[2].uppercase(Locale.ROOT)
}
