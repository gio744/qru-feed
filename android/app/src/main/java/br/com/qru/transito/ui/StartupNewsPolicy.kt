package br.com.qru.transito.ui

/** Each publication has its own identity; reading one never suppresses a new one. */
internal fun shouldShowStartupNews(publicationId:String,seen:Set<String>):Boolean =
 publicationId.isNotBlank() && publicationId !in seen

internal fun seenNewsAfterDismissal(seen:Set<String>,publicationId:String,dontShowAgain:Boolean):Set<String> =
 if(dontShowAgain && publicationId.isNotBlank()) seen + publicationId else seen
