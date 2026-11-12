package space.o4bit.projectasteria.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.NasaApodService
import space.o4bit.projectasteria.data.local.ApodDao
import space.o4bit.projectasteria.data.local.ApodEntity
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.paging.ApodPagingSource
import space.o4bit.projectasteria.utils.VideoEmbedUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Random

class SpaceRepository(
    private val apodDao: ApodDao,
    private val nasaApodService: NasaApodService = NasaApodService.create()
) {
    private var memoryCache: EnhancedAstronomyPicture? = null
    private var memoryCacheTimestamp: Long = 0L
    private val memoryCacheTtlMs: Long = 3_600_000L

    suspend fun getTodaysAstronomyPicture(
        forceRefresh: Boolean = false
    ): EnhancedAstronomyPicture = withContext(Dispatchers.IO) {
        if (!forceRefresh && System.currentTimeMillis() - memoryCacheTimestamp < memoryCacheTtlMs) {
            memoryCache?.takeIf { isValidApod(it.astronomyPicture) }?.let { return@withContext it }
        }
        try {
            val rawApod = runCatching {
                normalizeApod(nasaApodService.getLatestAstronomyPicture())
            }.getOrNull()

            val apod = if (rawApod != null && isValidApod(rawApod)) {
                rawApod
            } else {
                val todayStr = LocalDate.now(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                val code = VideoEmbedUtils.dateToApodCode(rawApod?.date ?: todayStr)
                val byCode = code?.let {
                    runCatching {
                        normalizeApod(nasaApodService.getScienceNasaApodByCode(it))
                    }.getOrNull()
                }
                if (byCode != null && isValidApod(byCode)) {
                    byCode
                } else {
                    val latestList = runCatching {
                        nasaApodService.getScienceNasaLatestApods()
                    }.getOrDefault(emptyList())
                    latestList.firstOrNull()?.let { normalizeApod(it) }
                        ?: rawApod
                        ?: throw IllegalStateException("Unable to load today's APOD")
                }
            }

            val existing = apodDao.getApodByDate(apod.date)
            val spaceFact = existing?.shortFact?.takeIf { it.isNotBlank() } ?: getRandomSpaceFact()
            val notificationTitle = "Today's Space Discovery: ${apod.title}"
            val notificationBody = createNotificationBody(apod)
            val enhanced = EnhancedAstronomyPicture(
                astronomyPicture = apod,
                shortFact = spaceFact,
                notificationTitle = notificationTitle,
                notificationBody = notificationBody,
                isFavorite = existing?.isFavorite ?: false
            )
            apodDao.insertApod(ApodEntity.from(enhanced))
            memoryCache = enhanced
            memoryCacheTimestamp = System.currentTimeMillis()
            enhanced
        } catch (e: Exception) {
            val cached = apodDao.getLatestApod()
            if (cached != null) {
                val enhanced = cached.toEnhancedAstronomyPicture()
                memoryCache = enhanced
                memoryCacheTimestamp = System.currentTimeMillis()
                enhanced
            } else {
                throw e
            }
        }
    }

    suspend fun getHistory(
        limit: Int = 20,
        offset: Int = 0
    ): List<EnhancedAstronomyPicture> = withContext(Dispatchers.IO) {
        val cached = apodDao.getPagedApods(limit, offset)
        if (cached.isEmpty() && offset == 0) {
            emptyList()
        } else {
            cached
                .map { it.toEnhancedAstronomyPicture() }
                .filter { !isPlaceholderApod(it.astronomyPicture) }
        }
    }

    fun clearMemoryCache() {
        memoryCache = null
        memoryCacheTimestamp = 0L
    }

    @Suppress("unused")
    internal suspend fun getAstronomyPictureForDate(date: Date): EnhancedAstronomyPicture = withContext(Dispatchers.IO) {
        val formattedDate = date.toInstant()
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cached = apodDao.getApodByDate(formattedDate)
        if (cached != null) {
            val cachedEnhanced = cached.toEnhancedAstronomyPicture()
            if (isValidApod(cachedEnhanced.astronomyPicture)) {
                return@withContext cachedEnhanced
            }
        }
        val apod = fetchRemoteApodForDate(formattedDate)
            ?: throw IllegalStateException("No APOD found for $formattedDate")
        val spaceFact = cached?.shortFact?.takeIf { it.isNotBlank() } ?: getRandomSpaceFact()
        val notificationTitle = "Space Discovery: ${apod.title}"
        val notificationBody = createNotificationBody(apod)
        val enhanced = EnhancedAstronomyPicture(
            astronomyPicture = apod,
            shortFact = spaceFact,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody,
            isFavorite = cached?.isFavorite ?: false
        )
        apodDao.insertApod(ApodEntity.from(enhanced))
        enhanced
    }

    suspend fun getApodByDate(dateString: String): EnhancedAstronomyPicture? = withContext(Dispatchers.IO) {
        try {
            val cached = apodDao.getApodByDate(dateString)
            if (cached != null) {
                val cachedEnhanced = cached.toEnhancedAstronomyPicture()
                if (isValidApod(cachedEnhanced.astronomyPicture)) {
                    return@withContext cachedEnhanced
                }
            }
            val apod = fetchRemoteApodForDate(dateString) ?: return@withContext null
            val enhanced = EnhancedAstronomyPicture(
                astronomyPicture = apod,
                shortFact = cached?.shortFact?.takeIf { it.isNotBlank() } ?: getRandomSpaceFact(),
                notificationTitle = "Space Discovery: ${apod.title}",
                notificationBody = createNotificationBody(apod),
                isFavorite = cached?.isFavorite ?: false
            )
            apodDao.insertApod(ApodEntity.from(enhanced))
            enhanced
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchRemoteApodForDate(dateString: String): AstronomyPicture? {
        val proxyApod = runCatching {
            normalizeApod(nasaApodService.getAstronomyPictureByDate(dateString))
        }.getOrNull()
        if (proxyApod != null && proxyApod.date == dateString && isValidApod(proxyApod)) {
            return proxyApod
        }

        val code = VideoEmbedUtils.dateToApodCode(dateString)
        if (code != null) {
            val scienceApod = runCatching {
                val raw = nasaApodService.getScienceNasaApodByCode(code)
                val withDate = if (raw.date.isBlank()) raw.copy(date = dateString) else raw
                normalizeApod(withDate)
            }.getOrNull()
            if (scienceApod != null && isValidApod(scienceApod)) {
                return scienceApod
            }
        }

        val rangeApod = runCatching {
            nasaApodService.getAstronomyPictureRange(dateString, dateString)
                .firstOrNull { it.date == dateString }
                ?.let { normalizeApod(it) }
        }.getOrNull()
        if (rangeApod != null && isValidApod(rangeApod)) {
            return rangeApod
        }

        val directApod = runCatching {
            normalizeApod(nasaApodService.getDirectAstronomyPictureByDate(dateString))
        }.getOrNull()
        if (directApod != null && isValidApod(directApod)) {
            return directApod
        }

        return null
    }

    private fun isPlaceholderApod(apod: AstronomyPicture): Boolean {
        return apod.title.equals("NASA Science", ignoreCase = true)
    }

    private fun isValidApod(apod: AstronomyPicture): Boolean {
        if (isPlaceholderApod(apod)) return false
        val url = apod.url
        val hasValidUrl = !url.isNullOrBlank() && !url.contains("science.nasa.gov/image-article/")
        val hdUrl = apod.hdUrl
        val hasValidHdUrl = !hdUrl.isNullOrBlank() && !hdUrl.contains("news-thumbnail.png")
        return hasValidUrl || hasValidHdUrl
    }

    private fun normalizeApod(raw: AstronomyPicture): AstronomyPicture {
        val cleanExplanation = stripHtml(raw.explanation)
        val extractedFromHtml = VideoEmbedUtils.extractMediaUrlFromHtml(raw.basicHtml)
        val cleanHd = raw.hdUrl?.trim()
            ?.takeIf { it.isNotEmpty() && !it.contains("news-thumbnail.png") }
            ?.let { VideoEmbedUtils.normalizeUrl(it).takeIf { u -> u.isNotEmpty() } }
        val cleanUrl = raw.url?.trim()
            ?.takeIf { it.isNotEmpty() && !it.contains("science.nasa.gov/image-article/") }
            ?.let { VideoEmbedUtils.normalizeUrl(it).takeIf { u -> u.isNotEmpty() } }
        val cleanThumb = (raw.thumbnail ?: raw.thumbnailUrl)?.trim()
            ?.takeIf { it.isNotEmpty() && !it.contains("news-thumbnail.png") }
            ?.let { VideoEmbedUtils.normalizeUrl(it).takeIf { u -> u.isNotEmpty() } }

        return if (raw.mediaType.equals("video", ignoreCase = true)) {
            val bestVideoUrl = extractedFromHtml ?: cleanUrl ?: cleanHd
            val bestThumb = cleanThumb
                ?: cleanHd?.takeIf { !VideoEmbedUtils.isDirectVideoUrl(it) }
                ?: VideoEmbedUtils.extractVideoThumbnailUrl(bestVideoUrl)
            raw.copy(
                explanation = cleanExplanation,
                hdUrl = cleanHd,
                url = bestVideoUrl,
                thumbnail = bestThumb,
                thumbnailUrl = bestThumb
            )
        } else {
            val bestImageUrl = cleanHd ?: cleanUrl ?: extractedFromHtml
            raw.copy(
                explanation = cleanExplanation,
                hdUrl = cleanHd ?: bestImageUrl,
                url = cleanUrl ?: bestImageUrl,
                thumbnail = cleanThumb,
                thumbnailUrl = cleanThumb
            )
        }
    }

    private fun stripHtml(input: String): String {
        if (!input.contains("<") && !input.contains("&")) return input
        val withoutTags = Regex("<[^>]*>").replace(input, " ")
        val decoded = withoutTags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8211;", "-")
            .replace("&#8212;", "--")
        return Regex("[ \\t]+").replace(decoded, " ").trim()
    }

    suspend fun fetchHistory(days: Int = 10) {
        val today = Date()
        val calendar = Calendar.getInstance()
        calendar.time = today
        val jobs = (0 until days).map {
            val date = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            date
        }
        withContext(Dispatchers.IO) {
            jobs.forEach { date ->
                try {
                    getAstronomyPictureForDate(date)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createNotificationBody(apod: AstronomyPicture): String {
        val explanation = apod.explanation
        val firstSentence = explanation.split(". ").firstOrNull()?.plus(".") ?: explanation
        return if (firstSentence.length <= 100) {
            firstSentence
        } else {
            firstSentence.substring(0, 97) + "..."
        }
    }

    private fun getRandomSpaceFact(): String {
        val facts = listOf(
            "Light from the Sun takes about 8 minutes to reach Earth.",
            "A day on Venus is longer than a year on Venus.",
            "The largest volcano in our solar system is on Mars - Olympus Mons.",
            "The Great Red Spot on Jupiter is a storm that has been raging for at least 400 years.",
            "Saturn's rings are made mostly of ice particles, with a small amount of rocky debris.",
            "Neptune's winds are the fastest in the solar system, reaching speeds of 1,200 mph.",
            "The temperature at the Sun's core is about 27 million degrees Fahrenheit.",
            "One million Earths could fit inside the Sun.",
            "The Milky Way galaxy is estimated to contain 100-400 billion stars.",
            "The Hubble Space Telescope orbits Earth at about 17,000 mph.",
            "The universe is estimated to be about 13.8 billion years old.",
            "Black holes have gravitational pulls so strong that even light cannot escape.",
            "Neutron stars can rotate up to 600 times per second.",
            "There are more stars in the universe than grains of sand on all of Earth's beaches.",
            "The closest known galaxy to the Milky Way is the Canis Major Dwarf Galaxy.",
            "The largest known star, UY Scuti, has a radius about 1,700 times that of the Sun.",
            "A teaspoonful of neutron star material would weigh about a billion tons.",
            "The footprints left by Apollo astronauts on the Moon will likely last for millions of years.",
            "The Moon is moving away from Earth at a rate of about 1.5 inches per year.",
            "Pluto's orbit is so eccentric that it sometimes comes closer to the Sun than Neptune."
        )
        return facts[Random().nextInt(facts.size)]
    }

    suspend fun getPagedHistory(page: Int, pageSize: Int): List<EnhancedAstronomyPicture> {
        val offset = (page - 1) * pageSize
        return getHistory(limit = pageSize, offset = offset)
    }

    fun searchApods(query: String): Flow<List<EnhancedAstronomyPicture>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return flowOf(emptyList())
        return apodDao.searchApods(trimmed).map { list ->
            list.map { it.toEnhancedAstronomyPicture() }
        }
    }

    suspend fun setFavorite(date: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        apodDao.setFavorite(date, isFavorite)
        if (memoryCache?.astronomyPicture?.date == date) {
            memoryCache = memoryCache?.copy(isFavorite = isFavorite)
        }
    }

    fun getFavoriteApods(): Flow<List<EnhancedAstronomyPicture>> {
        return apodDao.getFavoriteApods().map { list ->
            list.map { it.toEnhancedAstronomyPicture() }
        }
    }

    fun searchFavoriteApods(query: String): Flow<List<EnhancedAstronomyPicture>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getFavoriteApods()
        return apodDao.searchFavoriteApods(trimmed).map { list ->
            list.map { it.toEnhancedAstronomyPicture() }
        }
    }

    fun getApodHistory(): Flow<PagingData<EnhancedAstronomyPicture>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ApodPagingSource(this) }
        ).flow
    }
}
