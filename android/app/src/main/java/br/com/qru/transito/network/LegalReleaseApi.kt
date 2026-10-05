package br.com.qru.transito.network
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface LegalReleaseApi{
    @GET("legal/releases/current/package")
    suspend fun currentPackage():Response<ResponseBody>
}
