package space.o4bit.projectasteria.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import space.o4bit.projectasteria.data.model.solarsystem.BodiesResponse
import space.o4bit.projectasteria.data.model.solarsystem.CelestialBodyDto
import java.util.concurrent.TimeUnit

interface SolarSystemService {
    @GET("rest/bodies")
    suspend fun getBodies(
        @Header("Authorization") authHeader: String? = null,
        @Query("order") order: String? = null
    ): BodiesResponse

    @GET("rest/bodies/{id}")
    suspend fun getBody(
        @Path("id") id: String,
        @Header("Authorization") authHeader: String? = null
    ): CelestialBodyDto

    companion object {
        private const val BASE_URL = "https://api.le-systeme-solaire.net/"

        fun create(): SolarSystemService {
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
                .create(SolarSystemService::class.java)
        }
    }
}
