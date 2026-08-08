package space.o4bit.projectasteria.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.core.graphics.PathParser
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.iss.IssPosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Accuracy ISS Widget Map Renderer.
 *
 * Uses the same high-fidelity SVG path as the in-app IssGlobe composable.
 * Colors adapt based on Android 12+ system dynamic color attributes where available,
 * with a polished dark fallback for older devices.
 */
object IssWidgetMapRenderer {

    // Cache the parsed world path — parse once, reuse forever across widget updates
    @Volatile private var cachedPathString: String? = null
    @Volatile private var cachedAndroidPath: Path? = null

    private fun getWorldPath(context: Context): Path {
        cachedAndroidPath?.let { return it }
        val pathStr = cachedPathString ?: run {
            context.resources.openRawResource(R.raw.world_map_path)
                .bufferedReader().use { it.readText() }
                .also { cachedPathString = it }
        }
        return PathParser.createPathFromPathData(pathStr).also { cachedAndroidPath = it }
    }

    fun generateMapBitmap(
        context: Context,
        widthPx: Int = 540,
        heightPx: Int = 290,
        issPosition: IssPosition? = null
    ): Bitmap {
        // Use fixed, premium dark-mode colors. The widget card background is hardcoded dark (#121820),
        // so we must use dark-mode map colors regardless of the system's day/night mode to prevent
        // a "color inverted" look (e.g. white ocean on a dark widget card).
        val surfaceColor     = Color.parseColor("#090F19")
        val surfaceHighColor = Color.parseColor("#111A29")
        val landFill         = Color.parseColor("#1D385C")
        val landStroke       = Color.parseColor("#7BA5E0")
        val gridLineColor    = Color.parseColor("#1A293D")
        val errorColor       = Color.parseColor("#F43F5E")
        val nightColor       = Color.parseColor("#03070D")

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        // Clip canvas to smooth rounded rectangle (e.g. 24px radius)
        val cornerRadius = 24f
        val clipPath = Path().apply {
            addRoundRect(
                android.graphics.RectF(0f, 0f, w, h),
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)

        // 1. Ocean gradient background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                w * 0.5f, 0f, w * 0.5f, h,
                surfaceHighColor, surfaceColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Lat/Lon Grid
        val gridPaint = Paint().apply {
            color = gridLineColor; strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
        }
        for (lat in -60..60 step 30) {
            val y = (90f - lat) / 180f * h
            canvas.drawLine(0f, y, w, y, gridPaint)
        }
        for (lon in -150..150 step 30) {
            val x = (lon + 180f) / 360f * w
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }

        // Equator & Prime Meridian
        val axisPaint = Paint().apply {
            color = gridLineColor; strokeWidth = 1.5f; style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, 90f / 180f * h, w, 90f / 180f * h, axisPaint)
        canvas.drawLine(180f / 360f * w, 0f, 180f / 360f * w, h, axisPaint)

        // 3. Day/Night Terminator
        drawDayNightTerminator(canvas, w, h, nightColor)

        // 4. High-fidelity SVG continents
        drawSvgContinents(context, canvas, w, h, landFill, landStroke)

        // 5. ISS reticle
        issPosition?.let { pos ->
            val tx = ((pos.longitude + 180.0) / 360.0 * w).toFloat()
            val ty = ((90.0 - pos.latitude) / 180.0 * h).toFloat()

            // Outer glow
            canvas.drawCircle(tx, ty, 19f, Paint().apply {
                color = errorColor; strokeWidth = 3f
                style = Paint.Style.STROKE; isAntiAlias = true; alpha = 75
            })
            // Ring
            canvas.drawCircle(tx, ty, 12f, Paint().apply {
                color = errorColor; strokeWidth = 2f; style = Paint.Style.STROKE; isAntiAlias = true
            })
            // Core fill
            canvas.drawCircle(tx, ty, 5f, Paint().apply { color = errorColor; isAntiAlias = true })
            canvas.drawCircle(tx, ty, 2.5f, Paint().apply { color = Color.WHITE; isAntiAlias = true })
            // Crosshair ticks
            val tickPaint = Paint().apply { color = errorColor; strokeWidth = 1.8f; isAntiAlias = true }
            val tL = 11f; val tG = 6f
            canvas.drawLine(tx - tL, ty, tx - tG, ty, tickPaint)
            canvas.drawLine(tx + tG, ty, tx + tL, ty, tickPaint)
            canvas.drawLine(tx, ty - tL, tx, ty - tG, tickPaint)
            canvas.drawLine(tx, ty + tG, tx, ty + tL, tickPaint)
        }

        // 6. Smooth rounded map border frame
        val mapBorderPaint = Paint().apply {
            color = Color.parseColor("#263D5C")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            android.graphics.RectF(1.5f, 1.5f, w - 1.5f, h - 1.5f),
            cornerRadius,
            cornerRadius,
            mapBorderPaint
        )

        return bitmap
    }

    private fun drawSvgContinents(
        context: Context, canvas: Canvas, w: Float, h: Float,
        landFill: Int, landStroke: Int
    ) {
        val srcPath = getWorldPath(context)
        val matrix = Matrix().apply { setScale(w / 360f, h / 180f) }
        val scaledPath = Path(srcPath).apply { transform(matrix) }

        canvas.drawPath(scaledPath, Paint().apply {
            color = landFill; style = Paint.Style.FILL; isAntiAlias = true
        })
        canvas.drawPath(scaledPath, Paint().apply {
            color = landStroke; style = Paint.Style.STROKE
            strokeWidth = 1.5f; isAntiAlias = true
        })
    }

    private fun drawDayNightTerminator(canvas: Canvas, w: Float, h: Float, nightColor: Int) {
        val now = System.currentTimeMillis() / 1000.0
        val dayOfYear = ((now % 31536000) / 86400).toInt()
        val decRad = 23.45 * sin(2 * PI * (dayOfYear - 81) / 365) * PI / 180.0
        val sunLon = -((now % 86400) / 86400.0 * 360.0 - 180.0)

        val shadow = Path()
        shadow.moveTo(0f, h)
        
        for (xPx in 0..w.toInt() step 4) {
            val lon = (xPx.toFloat() / w) * 360.0 - 180.0
            val lonDiffRad = (lon - sunLon) * PI / 180.0
            val latRad = kotlin.math.atan(-cos(lonDiffRad) / kotlin.math.tan(decRad))
            val yPx = ((90.0 - latRad * 180.0 / PI) / 180.0 * h).toFloat().coerceIn(0f, h)
            shadow.lineTo(xPx.toFloat(), yPx)
        }
        
        shadow.lineTo(w, h)
        shadow.close()
        canvas.drawPath(shadow, Paint().apply { color = nightColor; alpha = 130; isAntiAlias = true })
    }
}
