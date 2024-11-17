fun main(args: Array<String>) {
    println("Starting GitHub Class Name Analyzer...")
    val client = GitHubClient()
    client.analyzeRepositories()
}