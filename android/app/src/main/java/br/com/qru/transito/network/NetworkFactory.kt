package br.com.qru.transito.network
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory{
    fun legalApi(baseUrl:String):LegalReleaseApi{
        require(baseUrl.startsWith("https://")){"Production legal API must use HTTPS"}
        val client=OkHttpClient.Builder()
            .connectTimeout(15,TimeUnit.SECONDS)
            .readTimeout(30,TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LegalReleaseApi::class.java)
    }
}
