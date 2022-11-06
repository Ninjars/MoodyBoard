package networking

import Logger
import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pmap
import java.io.File

class FileDownloader(
    private val rootFile: File
) {

    suspend fun downloadAll(prefix: String, urls: List<String>): List<File> =
        urls.pmap { url ->
            val file = File(rootFile, "$prefix${url.toFileName()}")
            if (withContext(Dispatchers.IO) { file.createNewFile() }) {
                Logger.logv { "downloading $url" }
                Fuel.download(url)
                    .fileDestination { _, _ -> file }
                    .response()
            } else {
                Logger.logv { "skipping download of $url" }
            }
            file
        }
}

private fun String.toFileName(): String = split("/").last()
