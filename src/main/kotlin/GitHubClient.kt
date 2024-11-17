import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json


class GitHubClient {
    private val token = ""

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            requestTimeout = 60_000 // 60 seconds
        }
    }

    suspend fun GitHubClient.fetchAllJavaFiles(repoFullName: String, path: String = ""): List<String> {
        val url = "https://api.github.com/repos/$repoFullName/contents/$path"
        val contents: List<GitHubContent> = try {
            client.get(url) {
                header("Authorization", "Bearer $token")
            }.body()
        } catch (e: Exception) {
            println("Error fetching contents at path $path: ${e.message}")
            return emptyList()
        }

        val javaFiles = mutableListOf<String>()
        for (content in contents) {
            if (content.type == "file" && content.name.endsWith(".java")) {
                try {
                    val fileContent = fetchFileContent(content.download_url!!)
                    if (fileContent != null) {
                        println("Fetched content of file: ${content.name}")
                        javaFiles.add(fileContent)
                    }
                } catch (e: Exception) {
                    println("Error fetching content of Java file: ${e.message}")
                }
            } else if (content.type == "dir") {
                // Recursively fetch contents from directories
                javaFiles.addAll(fetchAllJavaFiles(repoFullName, content.path))
            }
        }
        return javaFiles
    }

    suspend fun GitHubClient.fetchFileContent(downloadUrl: String): String? {
        return try {
            val response = client.get(downloadUrl) {
                header("Authorization", "Bearer $token")
            }

            // Check if the response content type is JSON
            val contentType = response.headers["Content-Type"]
            if (contentType != null && contentType.startsWith("application/json")) {
                println("Skipping non-text content: $downloadUrl")
                return null
            }

            response.body()
        } catch (e: Exception) {
            println("Error fetching file content: ${e.message}")
            null
        }
    }


    fun extractClassNames(fileContent: String): List<String> {
        // Improved regex to handle classes with annotations, modifiers, and generics
        val classRegex = Regex("""\b(public|abstract|final)?\s*class\s+([A-Z][a-zA-Z0-9]*)""")
        return classRegex.findAll(fileContent).map { it.groupValues[2] }.toList()
    }

    suspend fun searchRepositories(query: String): List<Repository> {
        val url = "https://api.github.com/search/repositories?q=$query+language:Java&sort=stars"
        val response: GitHubSearchReponse = client.get(url).body()
        println("Fetched ${response.items.size} repositories")
        return response.items
    }

    fun analyzeRepositories() = runBlocking {
        val client = GitHubClient()
        val repositories: List<Repository> = client.searchRepositories("spring")

        // Iterate over the list of repositories
        repositories.forEach { repo ->
            println("Analyzing repository: ${repo.full_name}")
            val javaFiles = client.fetchAllJavaFiles(repo.full_name)

            // Extract class names from the fetched Java files
            val classNames = javaFiles.flatMap { extractClassNames(it) }

            println("Classes found in ${repo.full_name}:")
            classNames.forEach { className ->
                println(className)
            }
        }
    }

}
