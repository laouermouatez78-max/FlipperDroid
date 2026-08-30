package com.example.flipperdroid.osint

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.ln


data class OsintFileReport(
    val name: String,
    val text: String,
    val warning: String? = null
)

object OsintFileAnalyzer {
    suspend fun analyze(context: Context, uri: Uri): OsintFileReport {
        val resolver = context.contentResolver
        val metadata = queryMetadata(context, uri)
        val name = metadata.first ?: "selected-file"
        val declaredSize = metadata.second
        val mime = resolver.getType(uri) ?: "unknown"

        val bytes = resolver.openInputStream(uri)?.use { input ->
            val digest256 = MessageDigest.getInstance("SHA-256")
            val digest1 = MessageDigest.getInstance("SHA-1")
            val sample = ByteArrayOutputCollector(262_144)
            var total = 0L
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                digest256.update(buffer, 0, read)
                digest1.update(buffer, 0, read)
                sample.write(buffer, 0, read)
            }
            FileBytes(
                totalSize = total,
                sha256 = digest256.digest().hex(),
                sha1 = digest1.digest().hex(),
                sample = sample.toByteArray()
            )
        } ?: return OsintFileReport(name, "Unable to read the selected file.", "Read failed")

        val signature = detectSignature(bytes.sample, name, mime)
        val entropy = shannonEntropy(bytes.sample)
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT).ifBlank { "none" }
        val mismatch = signature.extensionHints.isNotEmpty() && extension != "none" && extension !in signature.extensionHints

        val details = buildString {
            appendLine("File name: $name")
            appendLine("Declared MIME: $mime")
            appendLine("Extension: $extension")
            appendLine("Declared size: ${declaredSize?.let(::humanSize) ?: "unknown"}")
            appendLine("Read size: ${humanSize(bytes.totalSize)}")
            appendLine("Detected type: ${signature.label}")
            appendLine("Signature confidence: ${signature.confidence}")
            appendLine("Extension/type mismatch: $mismatch")
            appendLine("Sample entropy: ${"%.3f".format(Locale.US, entropy)} bits/byte")
            appendLine("SHA-256: ${bytes.sha256}")
            appendLine("SHA-1: ${bytes.sha1}")
            appendLine()

            when {
                signature.label.startsWith("JPEG") || signature.label.startsWith("PNG") || mime.startsWith("image/") -> {
                    append(imageMetadata(context, uri))
                }
                signature.label == "PDF document" || extension == "pdf" -> {
                    append(pdfMetadataHints(bytes.sample))
                }
                signature.label == "ZIP / OOXML / APK container" || extension in setOf("docx", "xlsx", "pptx", "zip", "apk") -> {
                    append(zipContainerMetadata(context, uri, extension))
                }
            }

            appendLine()
            appendLine("Privacy note: precise EXIF GPS coordinates are intentionally not displayed automatically. The analyzer only reports whether location metadata is present.")
        }

        val warning = when {
            mismatch -> "The file extension does not match the detected signature."
            entropy > 7.85 && bytes.sample.size > 4096 -> "Very high sample entropy may indicate compression, encryption or packed data."
            else -> null
        }
        return OsintFileReport(name, details.trim(), warning)
    }

    private fun imageMetadata(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        return runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                buildString {
                    appendLine("Image / EXIF metadata:")
                    appendLine("• make: ${exif.getAttribute(ExifInterface.TAG_MAKE) ?: "not present"}")
                    appendLine("• model: ${exif.getAttribute(ExifInterface.TAG_MODEL) ?: "not present"}")
                    appendLine("• software: ${exif.getAttribute(ExifInterface.TAG_SOFTWARE) ?: "not present"}")
                    appendLine("• date/time original: ${exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: "not present"}")
                    appendLine("• width: ${exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) ?: "not present"}")
                    appendLine("• height: ${exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) ?: "not present"}")
                    appendLine("• orientation: ${exif.getAttribute(ExifInterface.TAG_ORIENTATION) ?: "not present"}")
                    val gpsPresent = !exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE).isNullOrBlank() ||
                        !exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).isNullOrBlank()
                    appendLine("• GPS metadata present: $gpsPresent")
                    appendLine("• serial-number EXIF fields: not displayed by this analyzer")
                }
            } ?: "Image metadata unavailable.\n"
        }.getOrElse { "Image metadata unavailable: ${it.message ?: it::class.java.simpleName}\n" }
    }

    private fun pdfMetadataHints(sample: ByteArray): String {
        val text = sample.toString(Charsets.ISO_8859_1)
        fun field(name: String): String? {
            val regex = Regex("/${Regex.escape(name)}\\s*\\((.{0,300}?)\\)", setOf(RegexOption.DOT_MATCHES_ALL))
            return regex.find(text)?.groupValues?.getOrNull(1)?.replace(Regex("[\\r\\n\\t]+"), " ")?.take(220)
        }
        return buildString {
            appendLine("PDF metadata hints (best-effort plaintext scan):")
            appendLine("• title: ${field("Title") ?: "not observed"}")
            appendLine("• author: ${field("Author") ?: "not observed"}")
            appendLine("• creator: ${field("Creator") ?: "not observed"}")
            appendLine("• producer: ${field("Producer") ?: "not observed"}")
            appendLine("• creation date: ${field("CreationDate") ?: "not observed"}")
            appendLine("• modification date: ${field("ModDate") ?: "not observed"}")
            appendLine("• JavaScript marker observed: ${text.contains("/JavaScript") || text.contains("/JS")}")
            appendLine("• embedded-file marker observed: ${text.contains("/EmbeddedFile")}")
            appendLine("• encryption marker observed: ${text.contains("/Encrypt")}")
        }
    }

    private fun zipContainerMetadata(context: Context, uri: Uri, extension: String): String {
        val resolver = context.contentResolver
        return runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entries = 0
                    var coreXml: String? = null
                    var appXml: String? = null
                    var hasManifest = false
                    var hasMetaInf = false
                    var hasVba = false
                    var totalUncompressed = 0L
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries++
                        totalUncompressed += entry.size.coerceAtLeast(0)
                        val entryName = entry.name
                        if (entryName == "docProps/core.xml") coreXml = readEntryLimited(zip, 256_000)
                        if (entryName == "docProps/app.xml") appXml = readEntryLimited(zip, 256_000)
                        if (entryName.equals("AndroidManifest.xml", true)) hasManifest = true
                        if (entryName.startsWith("META-INF/", true)) hasMetaInf = true
                        if (entryName.endsWith("vbaProject.bin", true)) hasVba = true
                        if (entries >= 10_000) break
                    }

                    buildString {
                        appendLine("ZIP / package metadata:")
                        appendLine("• container extension: $extension")
                        appendLine("• entries observed: $entries")
                        appendLine("• declared uncompressed bytes: ${humanSize(totalUncompressed)}")
                        appendLine("• AndroidManifest.xml present: $hasManifest")
                        appendLine("• META-INF present: $hasMetaInf")
                        appendLine("• VBA macro project present: $hasVba")
                        if (!coreXml.isNullOrBlank()) {
                            appendLine("OOXML core properties:")
                            appendLine("• title: ${xmlTag(coreXml!!, "dc:title") ?: "not present"}")
                            appendLine("• creator: ${xmlTag(coreXml!!, "dc:creator") ?: "not present"}")
                            appendLine("• last modified by: ${xmlTag(coreXml!!, "cp:lastModifiedBy") ?: "not present"}")
                            appendLine("• created: ${xmlTag(coreXml!!, "dcterms:created") ?: "not present"}")
                            appendLine("• modified: ${xmlTag(coreXml!!, "dcterms:modified") ?: "not present"}")
                        }
                        if (!appXml.isNullOrBlank()) {
                            appendLine("OOXML application properties:")
                            appendLine("• application: ${xmlTag(appXml!!, "Application") ?: "not present"}")
                            appendLine("• company: ${xmlTag(appXml!!, "Company") ?: "not present"}")
                            appendLine("• manager: ${xmlTag(appXml!!, "Manager") ?: "not present"}")
                        }
                    }
                }
            } ?: "ZIP/package metadata unavailable.\n"
        }.getOrElse { "ZIP/package metadata unavailable: ${it.message ?: it::class.java.simpleName}\n" }
    }

    private fun queryMetadata(context: Context, uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return name to size
    }

    private data class FileBytes(val totalSize: Long, val sha256: String, val sha1: String, val sample: ByteArray)
    private data class Signature(val label: String, val extensionHints: Set<String>, val confidence: String)

    private fun detectSignature(sample: ByteArray, name: String, mime: String): Signature {
        fun starts(vararg bytes: Int): Boolean = sample.size >= bytes.size && bytes.indices.all { (sample[it].toInt() and 0xff) == bytes[it] }
        return when {
            starts(0xFF, 0xD8, 0xFF) -> Signature("JPEG image", setOf("jpg", "jpeg"), "high")
            starts(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> Signature("PNG image", setOf("png"), "high")
            starts(0x47, 0x49, 0x46, 0x38) -> Signature("GIF image", setOf("gif"), "high")
            sample.size >= 12 && sample.copyOfRange(0, 4).toString(Charsets.US_ASCII) == "RIFF" && sample.copyOfRange(8, 12).toString(Charsets.US_ASCII) == "WEBP" -> Signature("WebP image", setOf("webp"), "high")
            starts(0x25, 0x50, 0x44, 0x46, 0x2D) -> Signature("PDF document", setOf("pdf"), "high")
            starts(0x50, 0x4B, 0x03, 0x04) || starts(0x50, 0x4B, 0x05, 0x06) || starts(0x50, 0x4B, 0x07, 0x08) -> Signature("ZIP / OOXML / APK container", setOf("zip", "docx", "xlsx", "pptx", "apk", "jar", "odt", "ods", "epub"), "high")
            starts(0x7F, 0x45, 0x4C, 0x46) -> Signature("ELF executable", setOf("so", "elf", "bin"), "high")
            starts(0x4D, 0x5A) -> Signature("PE / DOS executable", setOf("exe", "dll", "sys"), "medium")
            starts(0x1F, 0x8B, 0x08) -> Signature("GZIP archive", setOf("gz", "tgz"), "high")
            starts(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C) -> Signature("7-Zip archive", setOf("7z"), "high")
            starts(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07) -> Signature("RAR archive", setOf("rar"), "high")
            else -> Signature("Unknown / text / unsupported signature", emptySet(), if (mime != "unknown" || name.contains('.')) "low" else "unknown")
        }
    }

    private fun shannonEntropy(bytes: ByteArray): Double {
        if (bytes.isEmpty()) return 0.0
        val counts = IntArray(256)
        bytes.forEach { counts[it.toInt() and 0xff]++ }
        var entropy = 0.0
        counts.filter { it > 0 }.forEach { count ->
            val p = count.toDouble() / bytes.size.toDouble()
            entropy -= p * (ln(p) / ln(2.0))
        }
        return entropy
    }

    private fun readEntryLimited(zip: ZipInputStream, maxBytes: Int): String {
        val buffer = ByteArray(4096)
        val out = ByteArrayOutputCollector(maxBytes)
        while (out.size < maxBytes) {
            val read = zip.read(buffer, 0, minOf(buffer.size, maxBytes - out.size))
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray().toString(Charsets.UTF_8)
    }

    private fun xmlTag(xml: String, tag: String): String? {
        val simple = tag.substringAfter(':')
        val regex = Regex("<(?:[A-Za-z0-9_-]+:)?${Regex.escape(simple)}(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z0-9_-]+:)?${Regex.escape(simple)}>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return regex.find(xml)?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]+>"), "")?.trim()?.take(300)
    }

    private fun humanSize(value: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = value.toDouble()
        var index = 0
        while (size >= 1024 && index < units.lastIndex) {
            size /= 1024.0
            index++
        }
        return if (index == 0) "$value B" else "%.2f %s".format(Locale.US, size, units[index])
    }

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

    private class ByteArrayOutputCollector(private val limit: Int) {
        private var data = ByteArray(0)
        val size: Int get() = data.size
        fun write(buffer: ByteArray, offset: Int, length: Int) {
            if (data.size >= limit || length <= 0) return
            val take = minOf(length, limit - data.size)
            data += buffer.copyOfRange(offset, offset + take)
        }
        fun toByteArray(): ByteArray = data
    }
}
