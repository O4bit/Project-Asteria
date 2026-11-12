package space.o4bit.projectasteria.data.api
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import space.o4bit.projectasteria.BuildConfig
import space.o4bit.projectasteria.data.model.AstronomyPicture
import java.util.concurrent.TimeUnit
interface NasaApodService {
    @GET("v2/nasa/apod/today")
    suspend fun getLatestAstronomyPicture(): AstronomyPicture
    @GET("v2/nasa/apod")
    suspend fun getAstronomyPictureByDate(
        @Query("date") date: String
    ): AstronomyPicture
    @GET("v2/nasa/apod/range")
    suspend fun getAstronomyPictureRange(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): List<AstronomyPicture>
    @GET("https://science.nasa.gov/wp-json/wp/v2/apod-basic")
    suspend fun getScienceNasaLatestApods(): List<AstronomyPicture>
    @GET("https://science.nasa.gov/wp-json/wp/v2/apod-basic/{code}")
    suspend fun getScienceNasaApodByCode(
        @Path("code") code: String
    ): AstronomyPicture
    @GET("https://api.nasa.gov/planetary/apod")
    suspend fun getDirectAstronomyPictureByDate(
        @Query("date") date: String,
        @Query("api_key") apiKey: String = "DEMO_KEY",
        @Query("thumbs") thumbs: Boolean = true
    ): AstronomyPicture
    companion object {
        fun create(): NasaApodService {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
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
                .baseUrl(BuildConfig.ASTERIA_API_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(NasaApodService::class.java)
        }
    }
}