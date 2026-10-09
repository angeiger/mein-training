package de.meintraining.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Machine photos live in the app's private storage. Every new photo gets a new file
 * name, so image caches never show an old picture.
 */
class PhotoStore(private val context: Context) {

    private val dir = File(context.filesDir, "photos").apply { mkdirs() }

    /** Empty file the camera app writes into, and the content Uri to hand to it. */
    fun newCapture(): Pair<File, Uri> {
        val file = File(dir, "aufnahme_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /** Shrinks and straightens a camera picture. Returns the stored path, or null. */
    suspend fun importCapture(capture: File, exerciseId: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!capture.exists() || capture.length() == 0L) return@withContext null
            store(exerciseId, { capture.inputStream() })
        } finally {
            capture.delete()
        }
    }

    suspend fun importFromGallery(uri: Uri, exerciseId: String): String? = withContext(Dispatchers.IO) {
        store(exerciseId, { context.contentResolver.openInputStream(uri) })
    }

    fun discard(file: File) {
        file.delete()
    }

    fun delete(path: String) {
        val file = File(path)
        // Only ever delete inside our own photo folder.
        if (file.parentFile?.canonicalPath == dir.canonicalPath) file.delete()
    }

    private fun store(exerciseId: String, open: () -> InputStream?): String? = runCatching {
        // With inJustDecodeBounds the call returns null by design; only the options are filled in.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (open() ?: return null).use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = open()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = open()?.use { ExifInterface(it).rotationDegrees } ?: 0

        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val safeId = exerciseId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val out = File(dir, "${safeId}_${System.currentTimeMillis()}.jpg")
        out.outputStream().use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (finalBitmap !== decoded) finalBitmap.recycle()
        decoded.recycle()
        out.absolutePath
    }.onFailure { Log.w("PhotoStore", "Could not store photo", it) }.getOrNull()

    private companion object {
        const val MAX_SIDE = 1600
    }
}
