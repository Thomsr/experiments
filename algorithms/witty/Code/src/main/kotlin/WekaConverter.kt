import datastructures.DataSet
import datastructures.importDataFromFile
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintStream
import java.util.*

/**
 * [0] Path of input file
 * [1] Path of output file
 */
fun main(args: Array<String>) {
    val inputPath = args[0]
    val outputPath = args[1]
    val data = importDataFromFile(inputPath)
    exportDataForWeka(outputPath, data)
}

class DataSetWeka(
    /**
     * The number of examples.
     */
    val n: Int,
    /**
     * The number of dimensions.
     */
    val d: Int,
    /**
     * Should have size n x d.
     * Each entry (e, i) represents the value of example e in dimension i.
     */
    val values: Array<DoubleArray>,
    /**
     * Should have size n.
     * Each entry (e) represents the class that is assigned to example e.
     */
    val cla: BooleanArray,
) {

    override fun toString(): String {
        val result = StringBuilder()
        for (e in 0..<n) {
            result.append("E$e ${cla[e]}: ${values[e].contentToString()}\n")
        }
        return result.toString()
    }

}

fun importDataFromFile(inputPath: String): DataSetWeka {
    val file = File(inputPath)
    val br = BufferedReader(InputStreamReader(file.inputStream()))
    val lines = LinkedList<String>()
    br.forEachLine {
        lines.add(it)
    }
    lines.removeFirst() // remove first line comment
    val n = lines.size
    val d = lines.first.split(",").size - 1
    val data = DataSetWeka(
        n = n,
        d = d,
        values = Array(n) { DoubleArray(d) },
        cla = BooleanArray(n),
    )
    for ((e, line) in lines.withIndex()) {
        val splits = line.split(",")
        for (i in 0..<splits.size - 1) {
            data.values[e][i] = splits[i].toDouble()
        }
        data.cla[e] = splits[splits.size - 1].toInt() == 1
    }
    return data
}


fun exportDataForWeka(outputPath: String, data: DataSetWeka) {
    // create data file
    val dataFile = File(outputPath)
    val dataOut = PrintStream(dataFile)
    // first write metadata
    dataOut.println("@relation $outputPath")
    for (i in 1..data.d) {
        dataOut.println("@attribute d$i real")
    }
    dataOut.println("@attribute class {1, 0}")
    // next write data
    dataOut.println("@data")
    for (e in 0..<data.n) {
        for (i in 0..<data.d) {
            if (data.values[e][i].toInt().toDouble() == data.values[e][i]) {
                dataOut.print("${data.values[e][i].toInt()},")
            } else {
                dataOut.print("${data.values[e][i]},")
            }
        }
        dataOut.print(if (data.cla[e]) "1" else "0")
        if (e < data.n - 1) {
            // last line should not have a line break at the end
            dataOut.println()
        }
    }
    dataOut.close()
}

