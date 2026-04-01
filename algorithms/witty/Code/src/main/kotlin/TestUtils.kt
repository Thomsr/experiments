import AlgoType.entries
import datastructures.DataSet
import datastructures.DecisionTree
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask
import kotlin.math.max
import kotlin.system.measureTimeMillis

const val resultsDir = "../experiments/results"
const val scriptDir = "../experiments"
const val dataDir = "../data"

val dataSets =
    arrayOf(
        "appendicitis",
        "australian",
        "auto",
        "backache",
        "biomed",
        "breast-cancer",
        "bupa",
        "cars",
        "cleve",
        "cleveland",
        "cleveland-nominal",
        "cloud",
        "colic",
        "contraceptive",
        "dermatology",
        "diabetes",
        "ecoli",
        "glass",
        "glass2",
        "haberman",
        "hayes-roth",
        "heart-c",
        "heart-h",
        "heart-statlog",
        "hepatitis",
        "hungarian",
        "lupus",
        "lymphography",
        "molecular_biology_promoters",
        "new-thyroid",
        "postoperative-patient-data",
        "schizo",
        "soybean",
        "spect",
        "tae",
    )

enum class AlgoType(
    val strategy: Int,
    val useDirtyPriority: Boolean,
    val doPreProcessing: Boolean,
    val useLowerBounds: Boolean,
    val useSubsetConstraints: Boolean,
    val useSubsetCaching: Boolean,
    val pairLBStrategy: Int,
) {
    BASIC(
        1,
        false,
        false,
        false,
        false,
        false,
        0,
    ),
    IMP1(
        1,
        true,
        false,
        false,
        false,
        false,
        0,
    ),
    IMP2(
        1,
        true,
        true,
        false,
        false,
        false,
        0,
    ),
    IMP3(
        1,
        true,
        true,
        true,
        false,
        false,
        0,
    ),
    IMP4(
        1,
        true,
        true,
        true,
        true,
        false,
        0,
    ),
    STRATEGY1(
        1,
        true,
        true,
        true,
        true,
        true,
        0,
    ),
    STRATEGY2(
        2,
        true,
        true,
        true,
        true,
        true,
        0,
    ),
    STRATEGY3(
        3,
        true,
        true,
        true,
        true,
        true,
        0,
    ),
    DECISION(
        0,
        true,
        true,
        true,
        true,
        true,
        0,
    ),
    PAIR_LB1(
        1,
        true,
        true,
        true,
        true,
        true,
        1,
    ),
    PAIR_LB2(
        1,
        true,
        true,
        true,
        true,
        true,
        2,
    ),
    PAIR_LB3(
        1,
        true,
        true,
        true,
        true,
        true,
        3,
    ),
    PAIR_LB4(
        1,
        true,
        true,
        true,
        true,
        true,
        4,
    ),
    PAIR_LB5(
        1,
        true,
        true,
        true,
        true,
        true,
        5,
    ),
    PAIR_LB6(
        1,
        true,
        true,
        true,
        true,
        true,
        6,
    ),
    PAIR_LB7(
        1,
        true,
        true,
        true,
        true,
        true,
        7,
    ),
    PAIR_LB8(
        1,
        true,
        true,
        true,
        true,
        true,
        8,
    ),
    PAIR_LB9(
        1,
        true,
        true,
        true,
        true,
        true,
        9,
    ),
    PAIR_LB10(
        1,
        true,
        true,
        true,
        true,
        true,
        10,
    ),
    PAIR_LB11(
        1,
        true,
        true,
        true,
        true,
        true,
        11,
    ),
    PAIR_LB12(
        1,
        true,
        true,
        true,
        true,
        true,
        12,
    ),
    PAIR_LB13(
        1,
        true,
        true,
        true,
        true,
        true,
        13,
    ),
    PAIR_LB14(
        1,
        true,
        true,
        true,
        true,
        true,
        14,
    ),
    PAIR_LB15(
        1,
        true,
        true,
        true,
        true,
        true,
        15,
    ),
    PAIR_LB16(
        1,
        true,
        true,
        true,
        true,
        true,
        16,
    ),
    PAIR_LB17(
        1,
        true,
        true,
        true,
        true,
        true,
        17,
    ),
    PAIR_LB18(
        1,
        true,
        true,
        true,
        true,
        true,
        18,
    ),
    ;
    /*
    ## |    algo     |  max depth  | after caching
    ----------------------------------------------
    01 |   normal    |     1       |    false
    02 |   normal    |     1       |    true
    03 |   normal    |     3       |    false
    04 |   normal    |     3       |    true
    05 |   normal    |     5       |    false
    06 |   normal    |     5       |    true
    07 | no presolve |     1       |    false
    08 | no presolve |     1       |    true
    09 | no presolve |     3       |    false
    10 | no presolve |     3       |    true
    11 | no presolve |     5       |    false
    12 | no presolve |     5       |    true
    13 |   reduced   |     1       |    false
    14 |   reduced   |     1       |    true
    15 |   reduced   |     3       |    false
    16 |   reduced   |     3       |    true
    17 |   reduced   |     5       |    false
    18 |   reduced   |     5       |    true
     */

    val id: Int
        get() = ordinal

    val pairLBAlgo: Int = when (pairLBStrategy) {
        in 1..6 -> 1
        in 7..12 -> 2
        in 13..18 -> 3
        else -> 0
    }
    val pairLBMaxDepth: Int = when (pairLBStrategy % 6) {
        in 1..2 -> 1
        in 3..4 -> 3
        else -> 5
    }
    val pairLBAfterCaching: Boolean = pairLBStrategy % 2 == 0

    companion object {
        fun getAlgoTypeFromId(id: Int): AlgoType {
            if (id in 0..<entries.size) {
                return entries[id]
            }
            throw NoSuchElementException("The id $id does not belong to any algorithm.")
        }
    }
}

class TestResult(
    val id: Int = -1,
    val dataSetName: String = "",
    val algoType: AlgoType = AlgoType.STRATEGY1,
    val subsetRatio: Float = 0f,
    val d: Int = 0,
    val s: Int = 0,
    val foundTree: Boolean = false,
    val treeSize: Int = 0,
    val timeMS: Long = 0,
    val memoryMiB: Long = 0,
    val timeoutHappened: Boolean = true,
    val correctTrainingData: Float = 0f,
    val searchTreeNodes: Int = 0,
    val lowerBoundEffect: Int = 0,
    val subsetConstraintEffect: Int = 0,
    val uniqueSets: Int = 0,
    val copiedSets: Int = 0,
    val setTrieSize: Int = 0,
    val upperBound: Int = 0,
    val depthSum: Int = 0,
    val minDepth: Int = 0,
    val maxDepth: Int = 0,
    val pairLBCalculated: Int = 0,
    val pairLBEffect: Int = 0,
    val cachingSearchTreeNodes: Int = 0,
    val pairLBGoodTime: Long = 0,
    val pairLBBadTime: Long = 0,
    val greedyEffect: Int = 0,
    val greedyTime: Long = 0,
    val dtFString: String = "-1:-1:false:-1:-1",
)

/**
 * Columns of the output file:
 *
 *    [00] problem ID: Int
 *    [01] algo type ID: Int
 *    [02] dataset name: String
 *    [03] dataset size: Int
 *    [04] subset ratio: Float
 *    [05] subset seed: Int
 *    [06] # of dimensions: Int
 *    [07] s: Int
 *    [08] timeoutSec: Long
 *    [09] timeMS: Long
 *    [10] memoryMiB: Long
 *    [11] timeout: Boolean
 *    [12] found tree: Boolean
 *    [13] tree size: Int
 *    [14] correct training data: Float
 *    [15] # of search tree nodes: Int
 *    [16] lower bound effect: Int
 *    [17] subset constraint effect: Int
 *    [18] unique sets: Int
 *    [19] copied sets: Int
 *    [20] setTrie size: Int
 *    [21] upper bound: Int
 *    [22] depth sum: Int
 *    [23] min depth: Int
 *    [24] max depth: Int
 *    [25] delta: Int
 *    [26] cuts: Int
 *    [27] big D: Int
 *    [28] pairLB calculations: Int
 *    [29] pairLB effect: Int
 *    [30] # of caching search tree nodes: Int
 *    [31] pairLB good time: Long
 *    [32] pairLB bad time: Long
 *    [33] greedy effect: Int
 *    [34] greedy time: Long
 *    [35] result decision tree as a formatted string: String
 */
fun printInfoCSV(
    outputPath: String,
    problemID: Int,
    algoId: Int,
    dataSet: DataSet,
    subsetRatio: Float,
    subsetSeed: Int,
    timeoutSec: Long,
    testResult: TestResult,
    ubTimeMS: Int,
) {
    val normData = dataSet.normalize()
    val builder = StringBuilder()
    builder.append("$problemID;")
    builder.append("$algoId;")
    builder.append("${testResult.dataSetName};")
    builder.append("${dataSet.n};")
    builder.append("$subsetRatio;")
    builder.append("$subsetSeed;")
    builder.append("${testResult.d};")
    builder.append("${testResult.s};")
    builder.append("$timeoutSec;")
    builder.append("${testResult.timeMS + ubTimeMS};")
    builder.append("${testResult.memoryMiB};")
    builder.append("${testResult.timeoutHappened};")
    builder.append("${testResult.foundTree};")
    builder.append("${testResult.treeSize};")
    builder.append("${String.format("%.2f", testResult.correctTrainingData)};")
    builder.append("${testResult.searchTreeNodes};")
    builder.append("${testResult.lowerBoundEffect};")
    builder.append("${testResult.subsetConstraintEffect};")
    builder.append("${testResult.uniqueSets};")
    builder.append("${testResult.copiedSets};")
    builder.append("${testResult.setTrieSize};")
    builder.append("${testResult.upperBound};")
    builder.append("${testResult.depthSum};")
    builder.append("${testResult.minDepth};")
    builder.append("${testResult.maxDepth};")
    builder.append("${normData.getDelta()};")
    builder.append("${normData.getNumberOfCuts()};")
    builder.append("${normData.getBigD()};")
    builder.append("${testResult.pairLBCalculated};")
    builder.append("${testResult.pairLBEffect};")
    builder.append("${testResult.cachingSearchTreeNodes};")
    builder.append("${testResult.pairLBGoodTime};")
    builder.append("${testResult.pairLBBadTime};")
    builder.append("${testResult.greedyEffect};")
    builder.append("${testResult.greedyTime};")
    builder.append("${testResult.dtFString};")

    val out = PrintStream(FileOutputStream(File(outputPath), true))
    out.println(builder.toString())
    out.close()
}

fun readNormalResults(filePath: String): List<TestResult> {
    val results = LinkedList<TestResult>()

    File(filePath).forEachLine { l ->
        val splits = l.split(";").filter { it.isNotBlank() }
        results.add(
            TestResult(
                (splits.getOrNull(0) ?: "0").toInt(),
                (splits.getOrNull(2) ?: "0"),
                AlgoType.getAlgoTypeFromId((splits.getOrNull(1) ?: "0").toInt()),
                (splits.getOrNull(4) ?: "0.0").toFloat(),
                (splits.getOrNull(6) ?: "0").toInt(),
                (splits.getOrNull(7) ?: "0").toInt(),
                (splits.getOrNull(12) ?: "false").toBoolean(),
                (splits.getOrNull(13) ?: "0").toInt(),
                (splits.getOrNull(9) ?: "0").toLong(),
                (splits.getOrNull(10) ?: "0").toLong(),
                (splits.getOrNull(11) ?: "false").toBoolean(),
                (splits.getOrNull(14) ?: "0.0").toFloat(),
                (splits.getOrNull(15) ?: "0").toInt(),
                (splits.getOrNull(16) ?: "0").toInt(),
                (splits.getOrNull(17) ?: "0").toInt(),
                (splits.getOrNull(18) ?: "0").toInt(),
                (splits.getOrNull(19) ?: "0").toInt(),
                (splits.getOrNull(20) ?: "0").toInt(),
                (splits.getOrNull(21) ?: "0").toInt(),
                (splits.getOrNull(22) ?: "0").toInt(),
                (splits.getOrNull(23) ?: "0").toInt(),
                (splits.getOrNull(24) ?: "0").toInt(),
                (splits.getOrNull(28) ?: "0").toInt(),
                (splits.getOrNull(29) ?: "0").toInt(),
                (splits.getOrNull(30) ?: "0").toInt(),
                (splits.getOrNull(31) ?: "0").toLong(),
                (splits.getOrNull(32) ?: "0").toLong(),
                (splits.getOrNull(33) ?: "0").toInt(),
                (splits.getOrNull(34) ?: "0").toLong(),
                (splits.getOrNull(35) ?: "-1:-1:false:-1:-1"),
            )
        )
    }

    return results
}

fun readSATResults(filePath: String): List<TestResult> {
    val results = LinkedList<TestResult>()

    File(filePath).forEachLine { l ->
        val splits = l.split(";").filter { it.isNotBlank() }
        results.add(
            TestResult(
                id = (splits.getOrNull(0) ?: "0").toInt(),
                dataSetName = (splits.getOrNull(1) ?: ""),
                subsetRatio = (splits.getOrNull(2) ?: "0.0").toFloat(),
                timeMS = (splits.getOrNull(5) ?: "0").toLong(),
                timeoutHappened = (splits.getOrNull(6) ?: "false").toBoolean(),
                foundTree = !(splits.getOrNull(6) ?: "false").toBoolean()
            )
        )
    }

    return results
}

fun readTreeResults(filePath: String): List<TestResult> {
    val results = LinkedList<TestResult>()

    File(filePath).forEachLine { l ->
        val splits = l.split(",").filter { it.isNotBlank() }
        results.add(
            TestResult(
                dataSetName = splits[0],
                dtFString = splits[1],
                timeoutHappened = false,
                foundTree = true,
            )
        )
    }

    return results
}

fun getInstanceName(dataSetName: String, ratio: Float, seed: Int) = "${dataSetName}_${ratio}_$seed"

fun getValuesFromInstanceName(name: String): Triple<String, Float, Int> {
    val splits = name.split("_")
    val ratio = splits[splits.size - 2].toFloat()
    val seed = splits[splits.size - 1].toInt()
    val dataSetName = name.removeSuffix("_${ratio}_$seed")
    return Triple(dataSetName, ratio, seed)
}

fun readMurTreeResults(filePath: String): List<TestResult> {
    val results = LinkedList<TestResult>()

    File(filePath).forEachLine { l ->
        val splits = l.split(";").filter { it.isNotBlank() }
        val instanceName = splits[0].replace("_bin_", "_")
        val (_, ratio, _) = getValuesFromInstanceName(instanceName)
        results.add(
            TestResult(
                dataSetName = instanceName,
                subsetRatio = ratio,
                treeSize = splits[1].toInt(),
                timeoutHappened = splits[1].toInt() == -1,
                foundTree = splits[1].toInt() != -1,
                timeMS = splits[2].toLong(),
            )
        )
    }

    return results
}

fun runTestFor(
    algoType: AlgoType,
    dataSetName: String,
    trainingData: DataSet,
    subsetRatio: Float,
    maxSize: Int,
    timeoutSec: Long,
    upperBound: Int,
): TestResult {
    val algo =
        WitnessTreeAlgo(
            trainingData,
            algoType,
        )
    var tree: DecisionTree? = null
    var timeMS = 0L
    val (timer, timeoutTask) = scheduleTimeout(algo, timeoutSec)
    val memoryMiB =
        measureMemoryMiB {
            timeMS =
                measureTimeMillis {
                    tree = algo.findTree(maxSize, upperBound)
                }
        }
    val timeoutHappened = !timeoutTask.cancel()
    timer.cancel()
    val correctTrainingData = tree?.let { testClassification(it, trainingData).second } ?: 0f
    return TestResult(
        -1,
        dataSetName,
        algoType,
        subsetRatio,
        trainingData.d,
        if (algoType.strategy > 0) -1 else maxSize,
        tree != null,
        tree?.innerCount ?: -1,
        timeMS,
        memoryMiB,
        timeoutHappened,
        correctTrainingData,
        algo.searchTreeNodes,
        algo.lowerBoundEffect,
        algo.subsetConstraintEffect,
        algo.uniqueSets,
        algo.copiedSets,
        algo.setTrieSize,
        upperBound,
        tree?.getDepthSum() ?: -1,
        tree?.getMinDepth() ?: -1,
        tree?.getMaxDepth() ?: -1,
        algo.pairLBCalculated,
        algo.pairLBEffect,
        algo.cachingSearchTreeNodes,
        algo.pairLBGoodTime,
        algo.pairLBBadTime,
        algo.greedyEffect,
        algo.greedyTime,
        tree?.toFormattedString() ?: "-1:-1:false:-1:-1"
    )
}

fun testClassification(
    t: DecisionTree,
    data: DataSet,
): Pair<Int, Float> {
    var countCorrectData = 0
    for (e in 0..<data.n) {
        val c = t.classifyExample(data.values[e])
        if (c == data.cla[e]) {
            countCorrectData++
        }
    }
    return Pair(countCorrectData, countCorrectData.toFloat() / data.n.toFloat())
}

fun printInfoConsole(
    dataSetSize: Int,
    subsetSeed: Int,
    timeoutSec: Long,
    testResult: TestResult,
) {
    println("Dataset:           ${testResult.dataSetName}")
    println("Total Examples:    $dataSetSize")
    println("Training Examples: ${testResult.subsetRatio}")
    println("Dimensions:        ${testResult.d}")
    println("Max Tree Size:     ${testResult.s}")
    println()
    if (testResult.timeoutHappened) {
        println("Timeout after $timeoutSec seconds.")
    } else {
        if (testResult.foundTree) {
            println("Tree found!")
            println("Size:                  ${testResult.treeSize}")
            println("Correct Training Data: ${testResult.correctTrainingData}")
        } else {
            println("Tree not found!")
        }
        println("Time:   ${prettyTime(testResult.timeMS)}")
        println("Memory: ${testResult.memoryMiB}MiB")

        println("Search Tree:   ${testResult.searchTreeNodes}")
        println("LB Effect:     ${testResult.lowerBoundEffect}")

        println("Unique Sets:   ${testResult.uniqueSets}")
        println("Copied Sets:   ${testResult.copiedSets}")
        println("SetTrie Size:  ${testResult.setTrieSize}")
    }
}

fun createTestReport(
    reportName: String,
    results: List<TestResult>,
) {
    // create new file
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    val curTime = sdf.format(cal.time)
    var foundName = false
    var num = 0
    var outputFile = File("reports/debug.txt")
    while (!foundName) {
        outputFile = File("reports/Report_${curTime}_${reportName}_$num.txt")
        if (outputFile.exists()) {
            num++
        } else {
            outputFile.createNewFile()
            foundName = true
        }
    }
    val out = PrintStream(outputFile)
    // gather some general data for formatting the output
    val maxMemoryLength = results.maxOf { it.memoryMiB.toString().length }
    val maxSearchTreeNodesLength = results.maxOf { it.searchTreeNodes.toString().length }
    // print results
    // group results by the used data set
    results.groupBy { it.dataSetName }.toSortedMap().forEach { (dataSetName, l) ->
        out.println("Results for $dataSetName (d = ${l.first().d}):")
        // now group and sort results by the size of the used subset
        l.groupBy { it.subsetRatio }.toSortedMap().forEach { (n, l2) ->
            // now group and sort results by the parameter h
            l2.groupBy { it.s }.toSortedMap().forEach { (s, resL) ->
                out.print("# Instances = %2d, n = %4d, s = %2d: ".format(resL.size, n, s))
                if (resL.all { it.timeoutHappened }) {
                    out.println("Timeout for all Instances")
                } else {
                    val resLFiltered = resL.filter { !it.timeoutHappened }
                    if (resLFiltered.any { it.foundTree && it.correctTrainingData != 1.0f }) throw Exception("Not 1")
                    val timeMSAvg = resLFiltered.sumOf { it.timeMS } / resLFiltered.size
                    val timeMSWorst = resLFiltered.maxOf { it.timeMS }
                    val memoryMiBAvg = resLFiltered.sumOf { it.memoryMiB } / resLFiltered.size
                    out.print("${prettyTime(timeMSAvg)} ${prettyTime(timeMSWorst)} ")
                    out.print("%${maxMemoryLength}dMiB ".format(memoryMiBAvg))
                    out.print("Timeout: ${resL.count { it.timeoutHappened }}/${resL.size} ")
                    out.print("Tree found: ${resLFiltered.count { it.foundTree }}/${resLFiltered.size} ")
                    out.print(
                        "Search tree nodes: %${maxSearchTreeNodesLength}d ".format(
                            resLFiltered.sumOf { it.searchTreeNodes } / resLFiltered.size,
                        ),
                    )
                    out.println("LB: ${resLFiltered[0].lowerBoundEffect} ")
                    // out.print("Unique: ${resLFiltered[0].uniqueSets},")
                    // out.print("Copied: ${resLFiltered[0].copiedSets},")
                    // out.println("Median: ${resLFiltered[0].setSizeMedian}")
                }
            }
        }
    }
    out.close()
}

fun scheduleTimeout(
    algo: WitnessTreeAlgo,
    timeoutSec: Long,
): Pair<Timer, TimerTask> {
    algo.timeoutHappened = false
    val timer = Timer()
    return Pair(
        timer,
        timer.schedule(timeoutSec * 1000) {
            algo.timeoutHappened = true
        },
    )
}

fun prettyTime(millis: Long): String {
    var h = 0L
    var m = 0L
    var s = 0L
    var ms = millis
    if (ms >= 3600000L) {
        h = ms / 3600000L
        ms %= 3600000L
    }
    if (ms >= 60000L) {
        m = ms / 60000L
        ms %= 60000L
    }
    if (ms >= 1000L) {
        s = ms / 1000L
        ms %= 1000L
    }
    return "%d:%02d:%02d.%03d".format(h, m, s, ms)
}

fun measureMemoryMiB(task: () -> Unit): Long {
    val runtime = Runtime.getRuntime()
    var memory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    val timer = Timer()
    timer.scheduleAtFixedRate(
        timerTask {
            memory = max(memory, (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
        },
        0,
        100,
    )
    task()
    timer.cancel()
    return max(memory, (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
}
