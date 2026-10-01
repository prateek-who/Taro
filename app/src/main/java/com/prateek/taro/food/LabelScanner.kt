package com.prateek.taro.food

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LabelScanner {
    fun photoUri(context: Context): Uri {
        val file = File(context.cacheDir, "scans/label.jpg").apply { parentFile?.mkdirs() }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun read(context: Context, uri: Uri): LabelValues {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            val tokens = text.textBlocks.flatMap { block -> block.lines.flatMap { line -> line.elements } }.mapNotNull { element ->
                val box = element.boundingBox ?: return@mapNotNull null
                OcrToken(element.text, box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat())
            }
            return LabelParser.parse(tokens)
        } finally {
            recognizer.close()
        }
    }
}
