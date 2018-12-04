package com.stepango.colorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Color.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.stepango.colorpicker.R.styleable
import android.opengl.ETC1.getWidth



@SuppressWarnings("MagicNumber")
class ColorPicker @JvmOverloads constructor(
        val ctx: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(ctx, attrs, defStyleAttr) {

    val colors: IntArray
    val strokeColor: Int
    var length: Float
    val strokeSize: Float
    val radius: Float
    val pickRadius: Float
    val previewBaseline: Float
    val rainbowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    val rainbowBackgroundPaint by lazy { bgPaint() }
    val pickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val previewRadius: Float
    var pick = 0.5f
    val rainbowBaseline: Float
    var showPreview = false
    var isVertical = false
    var listener: OnColorChangedListener? = null

    init {
        val a = ctx.theme.obtainStyledAttributes(attrs, styleable.ColorPicker, defStyleAttr, 0)
        val resId = a.getResourceId(styleable.ColorPicker_picker_pallet, 0)
        colors = if (resId != 0) resources.getIntArray(resId) else defColors()
        //@formatter:off
        strokeColor = a.getColor(styleable.ColorPicker_picker_strokeColor, WHITE)
        strokeSize = a.getDimension(styleable.ColorPicker_picker_strokeSize, 2.dpToPx(ctx))
        radius = a.getDimension(styleable.ColorPicker_picker_radius, 16.dpToPx(ctx))
        pickRadius = a.getDimension(styleable.ColorPicker_picker_radius, 12.dpToPx(ctx))
        previewRadius = a.getDimension(styleable.ColorPicker_picker_previewRadius, 16.dpToPx(ctx))
        rainbowBaseline = a.getDimension(styleable.ColorPicker_picker_baseline, 56.dpToPx(ctx))
        previewBaseline = a.getDimension(styleable.ColorPicker_picker_previewBaseline, 18.dpToPx(ctx))
        isVertical = a.getBoolean(styleable.ColorPicker_picker_isVertical, false)
        length = a.getDimension(styleable.ColorPicker_picker_length, if (isVertical) height.toFloat() else width.toFloat())
        //@formatter:on
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        drawPicker(canvas)
        drawColorAim(canvas, rainbowBaseline, pickRadius + strokeSize, pickRadius, color)
        if (showPreview) {
            drawColorAim(canvas, previewBaseline, previewRadius + strokeSize, previewRadius, color)
        }
    }

    private fun drawPicker(canvas: Canvas) {
        val lineX = if (!isVertical) radius else rainbowBaseline
        val lineY = if (!isVertical) rainbowBaseline else radius
        val lengthX = if (!isVertical) length else 0F
        val lengthY = if (isVertical) length else 0F
        rainbowPaint.strokeWidth = radius
        rainbowBackgroundPaint.strokeWidth = rainbowPaint.strokeWidth + strokeSize
        canvas.drawLine(lineX, lineY, lengthX + lineX, lengthY + lineY, rainbowBackgroundPaint)
        canvas.drawLine(lineX, lineY, lengthX + lineX, lengthY + lineY, rainbowPaint)
        val paint = Paint()
        paint.color = Color.BLACK
        canvas.drawLine(lineX, lineY, lengthX + lineX, lengthY + lineY, paint)//draw x axis
        canvas.drawLine(0F, 20F, rainbowBaseline, 20F, paint)
    }

    private fun drawColorAim(canvas: Canvas, baseLine: Float, offset: Float, size: Float, color: Int) {
        val circleCenterX = if (!isVertical) offset + pick * length else baseLine
        val circleCenterY = if (!isVertical) baseLine else offset + pick * length
        canvas.drawCircle(circleCenterX, circleCenterY, size + strokeSize, pickPaint.apply { this.color = strokeColor })
        canvas.drawCircle(circleCenterX, circleCenterY, size, pickPaint.apply { this.color = color })
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val viewLength = radius + length + previewRadius + 2 * strokeSize
        val viewHeight = rainbowBaseline + radius
        layoutParams.height = if (!isVertical) Math.ceil(viewHeight.toDouble()).toInt() else Math.ceil(viewLength.toDouble()).toInt()
        layoutParams.width = if (!isVertical) Math.ceil(viewLength.toDouble()).toInt() else Math.ceil(viewHeight.toDouble()).toInt()

        val x0 = if (!isVertical) (3F * radius) / 2F + strokeSize else 0F
        val y0 = if (!isVertical) 0F else (3F * radius) / 2F + strokeSize
        val x1 = x0 + (if (!isVertical) length else 0f)
        val y1 = y0 + (if (!isVertical) 0f else length)
        val shader = LinearGradient(
                x0,
                y0,
                x1,
                y1,
                colors,
                null,
                Shader.TileMode.CLAMP
        )
        rainbowPaint.shader = shader
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
            val coordinate = if (!isVertical) event.x else event.y
            pick = coordinate / length
            if (pick < 0) pick = 0f
            else if (pick > 1) pick = 1f
            listener?.onChangingColor(color)
            showPreview = true
        } else if (action == MotionEvent.ACTION_UP) {
            showPreview = false
            listener?.onColorChanged(color)
        }
        postInvalidateOnAnimation()
        return true
    }

    val color: Int
        get() = interpColor(pick, colors)

    fun setOnColorChangedListener(listener: OnColorChangedListener) {
        this.listener = listener
    }

    private fun bgPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    interface OnColorChangedListener {
        fun onChangingColor(color: Int)

        fun onColorChanged(color: Int)
    }

    companion object {
        private fun defColors() = intArrayOf(RED, GREEN, BLUE)
    }

}