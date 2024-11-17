import kotlinx.serialization.Serializable

@Serializable
data class GitHubSearchReponse(
    val items: List<Repository>
)

@Serializable
data class Repository(
    val full_name: String,
    val html_url: String
)

@Serializable
data class GitHubContent(
    val name: String,
    val path: String,
    val type: String,
    val download_url: String?
)