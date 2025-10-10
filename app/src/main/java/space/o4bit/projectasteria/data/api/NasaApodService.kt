package space.o4bit.projectasteria.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import space.o4bit.projectasteria.BuildConfig
import space.o4bit.projectasteria.data.model.AstronomyPicture
import java.util.concurrent.TimeUnit

/**
 * NASA APOD (Astronomy Picture of the Day) API interface
 * Now using custom O4bit Space Mirror API
 */
interface NasaApodService {
    @GET("apod/latest")
    suspend fun getLatestAstronomyPicture(): AstronomyPicture
    
    @GET("apod/{date}")
    suspend fun getAstronomyPictureByDate(
        @Path("date") date: String
    ): AstronomyPicture

    companion object {
        private const val BASE_URL = "https://api.o4bit.space/"
        // No API key needed for the mirror API
        private val NASA_API_KEY = BuildConfig.NASA_API_KEY  // Kept for backwards compatibility

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
