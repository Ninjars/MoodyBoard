package parsing

import Logger
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

private const val TwitterUrl = "https://twitter.com/"

data class Data(
    val title: String,
    val sections: List<TweetSection>
)

data class TweetSection(
    val title: String?,
    val tweetIds: List<String>,
)

fun markdownToTweetSections(input: String): List<TweetSection> {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(input)
    Logger.logv { "parsedTree:\n$parsedTree" }
    Logger.logv { "parent: ${parsedTree.parent}" }
    Logger.logv { "children:\n${parsedTree.children.map { "\n${it.type}\n${it.getTextInNode(input)}" }}" }

    val paragraphs = parsedTree.children
        .filter { it.type.name == "PARAGRAPH" }
        .map { it.getTextInNode(input).toString() }
    Logger.logv { "paragraphs:\n$paragraphs" }

    return paragraphs.map { toSection(it) }.also {
        Logger.logv { "sections:\n$it" }
    }
}

private fun toSection(paragraph: String): TweetSection {
    val lines = paragraph.split("\n").map { it.trim() }
    val title = lines.firstOrNull()?.takeIf { !it.isTwitterUrl() }
    val tweetIds = lines
        .flatMap { it.split(" ") }
        .filter { it.isTwitterUrl() }
        .map { it.split("/").last().split("?").first() }

    Logger.logv {
        "toSection:\n$paragraph" +
                "\n\nlines:\n$lines" +
                "\n\ntweet ids:\n$tweetIds"
    }
    return TweetSection(title, tweetIds)
}

private fun String.isTwitterUrl() = startsWith(TwitterUrl)
