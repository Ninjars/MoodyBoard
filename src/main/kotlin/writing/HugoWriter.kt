package writing

import DataModel
import DataSection
import Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import networking.FileDownloader
import java.io.BufferedWriter
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HugoWriter {
    private const val HeaderSeparator = "---"
//    private const val TableSeparator = "|"
    private const val SectionHeaderPrefix = "### "
    private const val ImagesSubDir = "images"
    private const val FileSuffix = ".md"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private data class PreparedSection(
        val authorName: String,
        val authorHandle: String,
        val tweetId: String,
        val tweetContent: String,
        val photoFileNames: List<String>?,
        val videoFileNames: List<String>?,
    ) {
        val hasContent = !photoFileNames.isNullOrEmpty() || !videoFileNames.isNullOrEmpty()
    }

    fun output(destination: File, data: DataModel, tags: String?) {
        // create file and subfolder
        if (!destination.mkdir()) {
            Logger.log("Writing to existing directory ${destination.path}")
        }
        val titleWithSuffix =
            if (data.pageTitle.endsWith(FileSuffix)) {
                data.pageTitle
            } else {
                "${data.pageTitle}$FileSuffix"
            }
        val outputFile = File(destination, titleWithSuffix).also {
            if (!it.createNewFile()) {
                Logger.log("Overwriting existing document ${it.name}")
                it.delete()
                it.createNewFile()
            }
        }
        val imagesDestination = File(destination, ImagesSubDir).also { it.mkdir() }
        val fileDownloader = FileDownloader(imagesDestination)

        outputFile.bufferedWriter().use { writer ->
            writer.headerBlock(data.pageTitle, tags)

            data.sections.forEach {
                writer.writeSection(fileDownloader, it)
            }
        }
    }

    private fun BufferedWriter.headerBlock(pageTitle: String, tags: String?) {
        appendLine(HeaderSeparator)
        appendLine("title: \"$pageTitle\"")
        appendLine("date: \"${currentDate()}\"")
        if (tags != null) {
            appendLine("tags: [${tags.split(",").joinToString(",") { "\"$it\"" }}]")
        }
        appendLine(HeaderSeparator)
    }

    private fun currentDate(): String = LocalDate.now().format(dateFormatter)

    private fun String.toSanitisedTweetContent(): String =
        replace("\"", "").split("\\s".toRegex()).filterNot { it.startsWith("#") }.joinToString(" ")

    private fun BufferedWriter.writeSection(fileDownloader: FileDownloader, section: DataSection) {
        val sectionDeets = section.tweetData.map { data ->
            val photos = runBlocking(Dispatchers.IO) {
                data.photoUrls?.let { fileDownloader.downloadAll(it) }
            }
            val videos = runBlocking {
                data.videoUrls?.let { fileDownloader.downloadAll(it) }
            }
            PreparedSection(
                data.authorScreenName,
                data.authorHandle,
                data.tweetId,
                data.text.toSanitisedTweetContent(),
                photos?.map { it.name },
                videos?.map { it.name },
            )
        }.filter { it.hasContent }

        if (sectionDeets.isEmpty()) return

        appendLine()
        if (section.title != null) {
            appendLine("$SectionHeaderPrefix${section.title}")
        }
        sectionDeets.forEach { data ->
            writeTweetData(data)
            appendLine()
        }
    }

    private fun BufferedWriter.writeTweetData(data: PreparedSection) {
        appendLine("Posted by [${data.authorName}|@${data.authorHandle}](https://twitter.com/${data.authorHandle})")
        data.photoFileNames?.forEach {
            appendLine("[![](../$ImagesSubDir/${it} \"${data.tweetContent}\")](https://twitter.com/${data.authorHandle}/status/${data.tweetId})")
        }
        data.videoFileNames?.forEach {
            appendLine("[![](../$ImagesSubDir/${it}  \"${data.tweetContent}\")](https://twitter.com/${data.authorHandle}/status/${data.tweetId})")
        }
//        data.photoFileNames
//            ?.map { "[![](../$ImagesSubDir/${it} \"${data.tweetContent}\")](https://twitter.com/${data.authorHandle}/status/${data.tweetId})" }
//            ?.let {
//                appendLine()
//                appendLine("| --- | --- |")
//            appendMediaElements(it)
//        }
    }

//    private fun BufferedWriter.appendMediaElements(elements: List<String>) {
//        when (elements.count()) {
//            0 -> return
//            1 -> appendLine("|${elements.first()}| |")
//            else -> {
//                (0 until elements.size / 2).forEach {
//                    val a = elements.getOrNull(it * 2)
//                    val b = elements.getOrNull(it * 2 + 1)
//                    appendTableRow(a, b)
//                }
//            }
//        }
//    }
//
//    private fun BufferedWriter.appendTableRow(vararg columns: String?) {
//        val values = columns.filterNotNull()
//        if (values.isEmpty()) return
//
//        append(TableSeparator)
//        values.forEach {
//            append(it)
//            append(TableSeparator)
//        }
//        appendLine()
//    }
}
