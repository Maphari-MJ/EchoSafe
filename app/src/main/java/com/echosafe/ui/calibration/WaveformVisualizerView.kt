package com.echosafe.ui.calibration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.math.sqrt

class WaveformVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D946EF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33D946EF")
        style = Paint.Style.FILL
    }

    private var amplitude: Float = 0.2f
    private var phase: Float = 0f

    fun setAmplitude(amp: Float) {
        this.amplitude = amp.coerceIn(0.05f, 1.0f)
        this.phase += 0.2f
        invalidate()
    }

    /**
     * Updates the waveform with real audio amplitude from a PCM buffer.
     */
    fun setAudioAmplitude(samples: ShortArray) {
        if (samples.isEmpty()) return
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / samples.size)
        val amp = (rms / 32768.0).toFloat().coerceIn(0.05f, 1.0f)
        setAmplitude(amp)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val centerY = h / 2f
        val points = 60
        val step = w / points

        var prevX = 0f
        var prevY = centerY

        for (i in 0..points) {
            val x = i * step
            val wave = sin((i * 0.3f) + phase) * (amplitude * (h * 0.4f))
            val y = centerY + wave
            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, linePaint)
            }
            prevX = x
            prevY = y
        }
    }
}
