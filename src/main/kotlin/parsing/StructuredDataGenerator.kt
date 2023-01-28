package parsing

import Logger
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

private const val TwitterUrl = "https://twitter.com/"

data class Data constructor(
    val title: String,
    val intro: String?,
    val sections: List<TweetSection>
)

data class TweetSection(
    val title: String?,
    val tweetIds: List<String>,
)

fun markdownToPageData(input: String): List<Data> {
    val flavour = CommonMarkFlavourDescriptor()
    val parser = MarkdownParser(flavour)
    val pages = input.splitStringOnMarkdownTitles().map {
        val parsedTree = parser.buildMarkdownTreeFromString(it)
        Data(
            title = parsedTree.firstHeading(it) ?: "",
            intro = parsedTree.introPara(it),
            sections = markdownToTweetSections(parsedTree, it),
        )
    }
    return pages
}

private fun String.splitStringOnMarkdownTitles(): List<String> = this.split("\n(?=#)".toRegex())

private fun ASTNode.firstHeading(input: String): String? =
    children.firstOrNull { it.type.name.startsWith("ATX") }
        ?.getTextInNode(input)
        ?.toString()
        ?.replace("#", "")
        ?.trim()

private fun ASTNode.introPara(input: String): String? =
    children.firstOrNull { it.type.name == "PARAGRAPH" }
        ?.getTextInNode(input)
        ?.toString()
        ?.takeUnless {
            it.split("\n").firstOrNull()?.isTwitterUrl() ?: true
        }

private fun markdownToTweetSections(parsedTree: ASTNode, input: String): List<TweetSection> {
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
