package space.o4bit.projectasteria.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import space.o4bit.projectasteria.data.model.iss.IssPosition
import java.util.concurrent.TimeUnit

interface IssService {
    @GET("v1/satellites/{id}")
    suspend fun getSatellitePosition(
        @Path("id") satelliteId: Int = 25544 // 25544 is the NORAD ID for ISS
    ): IssPosition

    companion object {
        private const val BASE_URL = "https://api.wheretheiss.at/"

        fun create(): IssService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(IssService::class.java)
        }
    }
}
