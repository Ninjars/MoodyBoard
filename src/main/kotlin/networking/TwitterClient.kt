package networking

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.serialization.responseObject
import com.github.kittinunf.result.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.jez.moodboard_builder.BuildConfig

private const val TwitterHost = "https://api.twitter.com/"

@Serializable
private data class NetworkTweetResponseRoot(val data: List<NetworkTweetPayload>, val includes: NetworkIncludes)

@Serializable
private data class NetworkIncludes(
    val media: List<NetworkMedia>,
    val users: List<NetworkUser>,
)

@Serializable
private data class NetworkMedia(
    val media_key: String,
    val type: String,
    val url: String,
) {
    val isPhoto = type == "photo"
}

@Serializable
private data class NetworkUser(
    val id: String,
    val name: String,
    val username: String,
)

@Serializable
private data class NetworkTweetPayload(
    val attachments: NetworkTweetAttachments,
    val text: String,
    val author_id: String,
    val id: String,
)

@Serializable
private data class NetworkTweetAttachments(val media_keys: List<String>)

data class TweetData(
    val tweetId: String,
    val authorId: String,
    val authorScreenName: String,
    val authorHandle: String,
    val text: String,
    val photoUrls: List<String>?,
    val videoUrls: List<String>?,
)

fun getTweets(ids: List<String>): List<TweetData> {
    val urlPath =
        "${TwitterHost}2/tweets?ids=${ids.joinToString(",")}" +
                "&user.fields=id,name,username" +
                "&tweet.fields=author_id,attachments" +
                "&expansions=attachments.media_keys,author_id" +
                "&media.fields=url,type"

    val (request, _, result) = Fuel.get(urlPath)
        .set("Authorization", "Bearer ${BuildConfig.twitterToken}")
        .responseObject<NetworkTweetResponseRoot>(json = Json { ignoreUnknownKeys = true })

    return when (result) {
        is Result.Failure -> {
            val ex = result.getException()
            Logger.log("failed to get results from ${request.url}.\nException: $ex")
            emptyList()
        }
        is Result.Success -> {
            val resultData = result.get()
            resultData.toData()
        }
    }
}

private fun NetworkTweetResponseRoot.toData(): List<TweetData> {
    val mediaMap = includes.media.associateBy { it.media_key }
    val userMap = includes.users.associateBy { it.id }
    return data.map { tweet ->
        val user = userMap.getValue(tweet.author_id)
        val media = tweet.attachments.media_keys.mapNotNull { mediaMap[it] }
        TweetData(
            tweetId = tweet.id,
                authorId = tweet.author_id,
                authorScreenName = user.name,
                authorHandle = user.username,
                text = tweet.text,
                photoUrls = media.filter { it.isPhoto }.map { it.url },
                videoUrls = media.filterNot { it.isPhoto }.map { it.url },
        )
    }
}
