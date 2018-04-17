package f2.test

import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface Test {
    val id: Int
    val name: String

    fun getOutputFiles(): List<File>
}

data class SingleSourceTest(
        override val id: Int,
        override val name: String,
        val source: String
) : Test {
    override fun getOutputFiles(): List<File> {
        return outputFiles.filter { it.isFile }.map { Pair(it.nameWithoutExtension, it) }.filter { it.first.toInt() == id }.map { it.second }
    }
}

data class MultipleSourceTest(
        override val id: Int,
        override val name: String,
        val sources: Map<String/* = File Name */, String/* = Source Code */>
) : Test {
    override fun getOutputFiles(): List<File> {
        return outputFiles.find { it.isDirectory && it.name.toInt() == id }?.listFiles()?.toList() ?: listOf()
    }
}

val Tests: List<Test> by lazy {
    File("tests/").listFiles().mapNotNull {
        if (it.isDirectory) {
            if (it.name == "out") null else {
                val folderNameParts = it.name.split("-")
                val id = folderNameParts.getOrNull(0)?.toIntOrNull() ?: -1
                val name = folderNameParts.subList(1, folderNameParts.size).joinToString(" ")
                val sources = it.listFiles().mapNotNull {
                    if (it.isDirectory) {
                        null
                    } else {
                        it.nameWithoutExtension to it.readText()
                    }
                }.toMap()
                MultipleSourceTest(id, name, sources)
            }
        } else {
            val fileNameParts = it.nameWithoutExtension.split("-")
            val id = fileNameParts.getOrNull(0)?.toIntOrNull() ?: -1
            val name = fileNameParts.subList(1, fileNameParts.size).joinToString(" ")
            val source = it.readText()
            SingleSourceTest(id, name, source)
        }
    }
}

private val outputFiles = File("tests/out/").listFiles()

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
