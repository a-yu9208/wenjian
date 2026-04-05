package com.example.yueyeushaokaojiaoziguan.merchant

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}

fun generateQrWithLabel(content: String, label: String, qrSize: Int = 512): Bitmap {
    val qr = generateQrBitmap(content, qrSize)
    val padding = 32
    val textSize = 48f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textHeight = (paint.descent() - paint.ascent()).toInt()
    val headerHeight = textHeight + padding * 2
    val totalWidth = qrSize + padding * 2
    val totalHeight = headerHeight + qrSize + padding

    val result = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(Color.WHITE)
    canvas.drawText(label, totalWidth / 2f, padding - paint.ascent(), paint)
    canvas.drawBitmap(qr, padding.toFloat(), headerHeight.toFloat(), null)
    return result
}

fun saveQrToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/夜月烧烤桌码")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return context.contentResolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        true
    } ?: false
}
