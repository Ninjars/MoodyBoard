import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.*
import networking.TweetData
import networking.getTweets
import parsing.Data
import parsing.markdownToPageData
import writing.HugoWriter
import java.io.File

data class DataModel(
    val pageTitle: String,
    val pageIntro: String?,
    val sections: List<DataSection>,
)

data class DataSection(
    val title: String?,
    val tweetData: List<TweetData>,
)

private fun readFrom(inputFile: File): List<Data> {
    Logger.logv { "Input file: ${inputFile.name}" }
    require(inputFile.exists()) { "Input file must exist" }

    val rawInput = inputFile.inputStream().bufferedReader().use { it.readText() }
    Logger.logv { "Raw Input:\n$rawInput" }

    //    val sections = markdownToTweetSections(rawInput)
//    return Data(nameOverride ?: inputFile.name, sections)
    return markdownToPageData(rawInput)
}

private fun buildModel(data: Data): DataModel {
    val sections = runBlocking(Dispatchers.Default) {
        data.sections.pmap { section ->
            DataSection(
                title = section.title,
                tweetData = getTweets(section.tweetIds),
            )
        }
    }
    return DataModel(data.title, data.intro, sections)
}

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

fun main(args: Array<String>) {
    Logger.log("Program arguments: ${args.joinToString()}")

    val parser = ArgParser("Moody")
    val verbose by parser.option(ArgType.Boolean, shortName = "v", description = "Enable verbose logging")
        .default(false)
    val inputFileName by parser.option(ArgType.String, shortName = "i", description = "Input file").required()
//    val fileNameOverride by parser.option(ArgType.String, shortName = "n", description = "Name for output file")
    val output by parser.option(
        ArgType.String,
        shortName = "o",
        description = "Output directory, defaults to current dir"
    ).default(System.getProperty("user.dir"))
    val tags by parser.option(
        ArgType.String,
        shortName = "t",
        description = "Tags to add to the output file"
    )

    parser.parse(args)

    Logger.verboseEnabled = verbose

    val inputData = readFrom(File(inputFileName))
    val modelledData = inputData.map { buildModel(it) }

    for (model in modelledData) {
        HugoWriter.output(File(output), model, tags)
    }
}
