package de.meintraining.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Machine photos and videos live in the app's private storage. Every new file gets a
 * new name, so image caches never show an old picture. Videos are kept in their own
 * folder, which is left out of the automatic Android backup (see backup_rules.xml):
 * a few videos would exceed its 25 MB limit and stop plans and photos being backed up.
 */
class MediaFiles(private val context: Context) {

    class Stored(val path: String, val isVideo: Boolean, val thumbPath: String?)

    private val photoDir = File(context.filesDir, "photos").apply { mkdirs() }
    private val videoDir = File(context.filesDir, "videos").apply { mkdirs() }

    /** Empty file the camera app writes a photo into, and the content Uri to hand to it. */
    fun newPhotoCapture(): Pair<File, Uri> = capture(File(photoDir, "aufnahme_${now()}.jpg"))

    fun newVideoCapture(): Pair<File, Uri> = capture(File(videoDir, "aufnahme_${now()}.mp4"))

    private fun capture(file: File): Pair<File, Uri> =
        file to FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Shrinks and straightens a camera picture. Returns null if it could not be read. */
    suspend fun importPhotoCapture(capture: File, exerciseId: String): Stored? = withContext(Dispatchers.IO) {
        try {
            if (!capture.exists() || capture.length() == 0L) return@withContext null
            storePhoto(exerciseId) { capture.inputStream() }
        } finally {
            capture.delete()
        }
    }

    /** Keeps a recorded video as it is and makes a still picture of it for lists. */
    suspend fun importVideoCapture(capture: File, exerciseId: String): Stored? = withContext(Dispatchers.IO) {
        if (!capture.exists() || capture.length() == 0L) {
            capture.delete()
            return@withContext null
        }
        val target = File(videoDir, "${safe(exerciseId)}_${now()}.mp4")
        if (!capture.renameTo(target)) {
            capture.delete()
            return@withContext null
        }
        Stored(target.absolutePath, isVideo = true, thumbPath = thumbnail(target))
    }

    /** Copies a photo or video chosen from the gallery. */
    suspend fun importFromGallery(uri: Uri, exerciseId: String): Stored? = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri).orEmpty()
        if (!type.startsWith("video/")) return@withContext storePhoto(exerciseId) { context.contentResolver.openInputStream(uri) }
        val ext = when {
            type.contains("3gpp") -> "3gp"
            type.contains("webm") -> "webm"
            else -> "mp4"
        }
        val target = File(videoDir, "${safe(exerciseId)}_${now()}.$ext")
        runCatching {
            (context.contentResolver.openInputStream(uri) ?: return@withContext null).use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            Stored(target.absolutePath, isVideo = true, thumbPath = thumbnail(target))
        }.onFailure {
            Log.w(TAG, "Could not copy video", it)
            target.delete()
        }.getOrNull()
    }

    fun discard(file: File) {
        file.delete()
    }

    fun delete(path: String) {
        val file = File(path)
        // Only ever delete inside our own folders.
        val parent = file.parentFile?.canonicalPath
        if (parent == photoDir.canonicalPath || parent == videoDir.canonicalPath) file.delete()
    }

    private fun thumbnail(video: File): String? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.absolutePath)
            val frame = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return null
            val scale = minOf(1f, THUMB_SIDE.toFloat() / maxOf(frame.width, frame.height))
            val small = Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt(), (frame.height * scale).toInt(), true)
            val out = File(videoDir, video.nameWithoutExtension + "_vorschau.jpg")
            out.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            out.absolutePath
        } finally {
            retriever.release()
        }
    }.onFailure { Log.w(TAG, "Could not make video thumbnail", it) }.getOrNull()

    private fun storePhoto(exerciseId: String, open: () -> InputStream?): Stored? = runCatching {
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
        val out = File(photoDir, "${safe(exerciseId)}_${now()}.jpg")
        out.outputStream().use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (finalBitmap !== decoded) finalBitmap.recycle()
        decoded.recycle()
        Stored(out.absolutePath, isVideo = false, thumbPath = null)
    }.onFailure { Log.w(TAG, "Could not store photo", it) }.getOrNull()

    private fun safe(id: String) = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    // Several gallery files are imported within the same millisecond.
    private var last = 0L
    @Synchronized
    private fun now(): Long {
        last = maxOf(System.currentTimeMillis(), last + 1)
        return last
    }

    private companion object {
        const val TAG = "MediaFiles"
        const val MAX_SIDE = 1600
        const val THUMB_SIDE = 640
    }
}
