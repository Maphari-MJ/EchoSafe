package com.echosafe.ui.dashboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.echosafe.data.model.Hotspot

class HotspotMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hotspots: List<Hotspot> = emptyList()
    var onHotspotSelected: ((Hotspot) -> Unit)? = null

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1D2D44")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val highRiskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E63946")
        style = Paint.Style.FILL
    }

    private val highRiskGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44E63946")
        style = Paint.Style.FILL
    }

    private val moderateRiskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F4A261")
        style = Paint.Style.FILL
    }

    private val moderateRiskGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44F4A261")
        style = Paint.Style.FILL
    }

    private val userLocationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64FFDA")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8892B0")
        textSize = 22f
    }

    private val pinPoints = mutableListOf<Pair<PointF, Hotspot>>()

    fun setHotspots(list: List<Hotspot>) {
        this.hotspots = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // Background dark map texture
        canvas.drawColor(Color.parseColor("#0B132B"))

        // Grid lines to simulate map topology
        val step = 60f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += step
        }

        // Draw GBV Heatmap Overlay zone
        val heatmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.45f, h * 0.45f, w * 0.35f,
                Color.parseColor("#33E63946"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.45f, h * 0.45f, w * 0.35f, heatmapPaint)

        // Draw User Live Location Pin
        val userX = w * 0.5f
        val userY = h * 0.65f
        canvas.drawCircle(userX, userY, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3364FFDA")
        })
        canvas.drawCircle(userX, userY, 8f, userLocationPaint)
        canvas.drawText("You are here", userX + 15f, userY + 8f, subTextPaint)

        pinPoints.clear()

        // Distribute and draw Hotspot Pins
        val positions = listOf(
            PointF(w * 0.35f, h * 0.30f),
            PointF(w * 0.70f, h * 0.25f),
            PointF(w * 0.25f, h * 0.75f),
            PointF(w * 0.80f, h * 0.60f)
        )

        hotspots.forEachIndexed { index, hotspot ->
            val pos = positions.getOrElse(index) {
                PointF(w * (0.2f + (index * 0.15f) % 0.6f), h * (0.2f + (index * 0.2f) % 0.6f))
            }
            pinPoints.add(Pair(pos, hotspot))

            val isHigh = hotspot.riskLevel.equals("HIGH", ignoreCase = true)
            val fill = if (isHigh) highRiskPaint else moderateRiskPaint
            val glow = if (isHigh) highRiskGlow else moderateRiskGlow

            canvas.drawCircle(pos.x, pos.y, 24f, glow)
            canvas.drawCircle(pos.x, pos.y, 12f, fill)

            val label = if (isHigh) "(!)" else "(i)"
            canvas.drawText(label, pos.x - 10f, pos.y - 18f, if (isHigh) highRiskPaint else moderateRiskPaint)
        }

        // Map Header Banner
        canvas.drawText("GBV CRIME HOTSPOT HEATMAP", 30f, 50f, textPaint)
        canvas.drawText("● High-Risk (Red)   ● Moderate-Risk (Yellow)", 30f, 85f, subTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val touchX = event.x
            val touchY = event.y

            for (pair in pinPoints) {
                val pt = pair.first
                val hotspot = pair.second
                val dist = Math.hypot((touchX - pt.x).toDouble(), (touchY - pt.y).toDouble())
                if (dist < 40) {
                    onHotspotSelected?.invoke(hotspot)
                    return true
                }
            }
        }
        return true
    }
}
