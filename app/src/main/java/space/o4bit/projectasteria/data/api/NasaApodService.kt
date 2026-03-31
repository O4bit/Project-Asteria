package space.o4bit.projectasteria.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import space.o4bit.projectasteria.BuildConfig
import space.o4bit.projectasteria.data.model.AstronomyPicture
import java.util.concurrent.TimeUnit

/**
 * NASA APOD (Astronomy Picture of the Day) API interface
 */
interface NasaApodService {
    @GET("planetary/apod")
    suspend fun getLatestAstronomyPicture(
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): AstronomyPicture
    
    @GET("planetary/apod")
    suspend fun getAstronomyPictureByDate(
        @Query("date") date: String,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): AstronomyPicture

    companion object {
        private const val BASE_URL = "https://api.nasa.gov/"

        fun create(): NasaApodService {
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
                .create(NasaApodService::class.java)
        }
    }
}
