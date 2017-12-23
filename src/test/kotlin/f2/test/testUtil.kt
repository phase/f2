package f2.test

import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

val RED_COLOR = "\u001b[31m"
val GREEN_COLOR = "\u001b[32m"
val BLUE_COLOR = "\u001b[34m"
val RESET_COLOR = "\u001b[0m"

// Parallel map
fun <T, R> Iterable<T>.pmap(
        numThreads: Int = Runtime.getRuntime().availableProcessors() - 2,
        exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
        transform: (T) -> R): List<R> {

    // default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
    val defaultSize = if (this is Collection<*>) this.size else 10
    val destination = Collections.synchronizedList(ArrayList<R>(defaultSize))

    for (item in this) {
        exec.submit { destination.add(transform(item)) }
    }

    exec.shutdown()
    exec.awaitTermination(1, TimeUnit.DAYS)

    return ArrayList<R>(destination)
}
