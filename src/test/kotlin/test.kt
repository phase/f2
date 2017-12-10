import java.io.File

data class Test(val id: Int, val name: String, val source: String)

val Tests: List<Test> = File("tests/").listFiles().mapNotNull {
    if (it.isDirectory) return@mapNotNull null
    val fileNameParts = it.nameWithoutExtension.split("-")
    val id = fileNameParts[0].toIntOrNull() ?: -1
    val name = fileNameParts.subList(1, fileNameParts.size).joinToString(" ")
    val source = it.readText()
    Test(id, name, source)
}

private val outputFiles = File("tests/out/").listFiles()

fun getOutputFiles(id: Int): List<File> =
        outputFiles.map { Pair(it.nameWithoutExtension, it) }.filter { it.first.toInt() == id }.map { it.second }
