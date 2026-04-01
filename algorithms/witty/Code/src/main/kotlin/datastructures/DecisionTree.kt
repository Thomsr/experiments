package datastructures

import java.io.File
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

class DecisionTree(
    size: Int,
) {

    companion object {

        /**
         * The string consists of several segments s0 to s(n-1) where n is the number of vertices in this Decision Tree.
         * The segments are separated by the character "|".
         *
         * Each segment si corresponds to vertex i in the tree and contains 5 values separated by the character ":".
         *
         * The first value is the parent p of vertex i. If i is the root this value will be -1.
         *
         * The second value is either l or r and shows whether i is the left (l) or right (r) child of its parent.
         * If i is the root this value will be -1.
         *
         * The third value is the class of vertex i; either true or false. If i is an inner vertex this value will be -1.
         *
         * The fourth and fifth values are the dimension and threshold of the cut of i. If i is a leaf both values
         * will be -1.
         */
        fun createFromFormattedString(fString: String): DecisionTree {
            val segments = fString.split("|")
            val innerCount = (segments.size - 1) / 2
            val dt = DecisionTree(innerCount)

            // map vertices from the string to vertices in the tree such that the first innerCount vertices are inner
            // vertices and the rest are leafs.
            val vertMap = HashMap<Int, Int>()
            var nextInner = 0
            var nextLeaf = innerCount
            segments.forEachIndexed { vi, s ->
                val values = s.split(":")
                val v: Int
                if (values[2] == "-1") {
                    v = nextInner
                    nextInner++
                } else {
                    v = nextLeaf
                    nextLeaf++
                }
                vertMap[vi] = v
            }

            segments.forEachIndexed { vi, s ->
                val values = s.split(":")
                val v = vertMap[vi]!!
                if (values[0] == "-1") {
                    dt.parent[v] = -1
                    dt.root = v
                } else {
                    val p = vertMap[values[0].toInt()]!!
                    dt.parent[v] = p
                    if (values[1] == "l") {
                        dt.leftChild[p] = v
                    } else {
                        dt.rightChild[p] = v
                    }
                }
                if (values[2] == "-1") {
                    // v is an inner vertex
                    dt.dim[v] = values[3].toInt()
                    dt.thr[v] = values[4].toDouble()
                } else {
                    // v is a leaf
                    dt.cla[v] = values[2].toBoolean()
                }
            }
            return dt
        }

        fun createFromYaDTDOTOutput(file: File): DecisionTree {
            val lines = file.readLines().toMutableList()
            lines.removeFirst()
            lines.removeLast()
            val vertexLines = LinkedList<String>()
            val edgeLines = LinkedList<String>()
            for (line in lines) {
                if (line.split(" ")[1] == "->") {
                    edgeLines.add(line)
                } else {
                    vertexLines.add(line)
                }
            }
            val vertexCount = vertexLines.size
            val dt = DecisionTree((vertexCount - 1) / 2)

            var nextInner = 0
            var nextLeaf = (vertexCount - 1) / 2
            val vertMap = HashMap<String, Int>()
            for (vertex in vertexLines) {
                val splits = vertex.split(" ")
                val v = splits[0]
                if (splits[2] == "shape=box,") {
                    // v is inner vertex
                    vertMap[v] = nextInner
                    dt.dim[nextInner] = splits[3].removePrefix("label=\"").removeSuffix("\\n\"]").toInt() - 1
                    nextInner++
                } else {
                    // v is leaf
                    vertMap[v] = nextLeaf
                    dt.cla[nextLeaf] = splits[3].startsWith("label=\"1")
                    nextLeaf++
                }
            }

            for (edge in edgeLines) {
                val splits = edge.split(" ")
                val v1 = vertMap[splits[0]]!!
                val v2 = vertMap[splits[2]]!!
                dt.parent[v2] = v1
                val thr = splits[3].removePrefix("[label=\"").removeSuffix("\"]")
                if (thr.startsWith("<=")) {
                    // v2 is left child of v1
                    dt.leftChild[v1] = v2
                    dt.thr[v1] = thr.removePrefix("<=").toDouble()
                } else if (thr.startsWith(">")) {
                    // v2 is right child of v1
                    dt.rightChild[v1] = v2
                    dt.thr[v1] = thr.removePrefix(">").toDouble()
                } else {
                    val curThr = thr.toDouble()
                    if (dt.leftChild[v1] != -1) {
                        val prevThr = dt.thr[v1]
                        if (prevThr < curThr) {
                            dt.rightChild[v1] = v2
                        } else {
                            dt.thr[v1] = curThr
                            dt.rightChild[v1] = dt.leftChild[v1]
                            dt.leftChild[v1] = v2
                        }
                    } else {
                        dt.leftChild[v1] = v2
                        dt.thr[v1] = curThr
                    }
                }
            }

            dt.root = 0 // set root to 0 for special case where tree only has one leaf
            for (v in 0..<dt.innerCount) {
                if (dt.parent[v] == -1) {
                    dt.root = v
                    break
                }
            }

            return dt
        }

        fun createFromWekaOutput(file: File): DecisionTree {
            val lines = file.readLines().toMutableList()
            while (!lines.first().startsWith("------------------")) {
                lines.removeFirst()
            }
            lines.removeFirst()
            lines.removeFirst()
            val treeLines = LinkedList<String>()
            while (!lines.first().startsWith("Number of Leaves")) {
                treeLines.add(lines.removeFirst())
            }
            treeLines.removeLast()
            val leafCount = lines.first().trim().removePrefix("Number of Leaves").trim().removePrefix(":").trim().toInt()
            val innerCount = leafCount - 1
            val dt = DecisionTree(innerCount)
            dt.root = 0
            var nextInner = 1
            var nextLeaf = innerCount

            val curRootPath = LinkedList<Int>()
            curRootPath.addLast(0)
            while (treeLines.isNotEmpty()) {
                var curLine = treeLines.removeFirst()
                // count depth
                var curDepth = 0
                while (curLine.startsWith(" ") || curLine.startsWith("|")) {
                    curLine = curLine.trim()
                    if (curLine.startsWith("|")) {
                        curLine = curLine.removePrefix("|")
                        curDepth++
                    }
                }
                curLine = curLine.trim()
                // update root path
                while (curRootPath.size > curDepth + 1) {
                    curRootPath.removeLast()
                }
                val p = curRootPath.last
                // get vertex type
                val v: Int
                if (curLine.endsWith(")")) {
                    v = nextLeaf
                    nextLeaf++
                    // get class of leaf
                    dt.cla[v] = curLine.split("(")[0].trim().split(" ").last() == "1"
                } else {
                    v = nextInner
                    nextInner++
                }
                // get dimension of parent
                dt.dim[p] = curLine.split(" ")[0].removePrefix("d").toInt() - 1
                // get threshold of parent
                dt.thr[p] = curLine.split(" ")[2].removeSuffix(":").toDouble()
                // set parent
                dt.parent[v] = p
                // set child of parent
                if (curLine.contains("<=")) {
                    dt.leftChild[p] = v
                } else {
                    dt.rightChild[p] = v
                }
                curRootPath.addLast(v)
            }
            return dt
        }
    }

    val innerCount = size
    val leafCount = size + 1
    val vertexCount = innerCount + leafCount

    var root = -1
    val parent = IntArray(vertexCount) { -1 }
    val leftChild = IntArray(vertexCount) { -1 }
    val rightChild = IntArray(vertexCount) { -1 }

    val dim = IntArray(vertexCount) { -1 }
    val thr = DoubleArray(vertexCount) { -1.0 }
    val cla = BooleanArray(vertexCount) { false }

    /**
     * Returns True iff [vertex] is a leaf.
     */
    fun isLeaf(vertex: Int) = vertex >= innerCount

    /**
     * Decides the class for the given example [e]. The given example should have the same number of dimensions as
     * the data used to create this tree.
     */
    fun classifyExample(e: DoubleArray) = cla[getLeafOfExample(e)]

    /**
     * Returns the leaf that the given example [e] is assigned to in the subtree of [subRoot]. The given example
     * should have the same number of dimensions as the data used to create this tree.
     */
    fun getLeafOfExample(e: DoubleArray, subRoot: Int = root): Int {
        var curRoot = subRoot
        while (!isLeaf(curRoot)) {
            curRoot = if (e[dim[curRoot]] <= thr[curRoot]) {
                leftChild[curRoot]
            } else {
                rightChild[curRoot]
            }
        }
        return curRoot
    }

    fun getDepthSum(subRoot: Int = root, curDepth: Int = 0): Int {
        return if (isLeaf(subRoot)) {
            curDepth
        } else {
            getDepthSum(leftChild[subRoot], curDepth + 1) +
                    getDepthSum(rightChild[subRoot], curDepth + 1)
        }
    }

    fun getMinDepth(subRoot: Int = root, curDepth: Int = 0): Int {
        return if (isLeaf(subRoot)) {
            curDepth
        } else {
            min(
                getMinDepth(leftChild[subRoot], curDepth + 1),
                getMinDepth(rightChild[subRoot], curDepth + 1)
            )
        }
    }

    fun getMaxDepth(subRoot: Int = root, curDepth: Int = 0): Int {
        return if (isLeaf(subRoot)) {
            curDepth
        } else {
            max(
                getMaxDepth(leftChild[subRoot], curDepth + 1),
                getMaxDepth(rightChild[subRoot], curDepth + 1)
            )
        }
    }

    /**
     * The string consists of several segments s0 to s(n-1) where n is the number of vertices in this Decision Tree.
     * The segments are separated by the character "|".
     *
     * Each segment si corresponds to vertex i in the tree and contains 5 values separated by the character ":".
     *
     * The first value is the parent p of vertex i. If i is the root this value will be -1.
     *
     * The second value is either l or r and shows whether i is the left (l) or right (r) child of its parent.
     * If i is the root this value will be -1.
     *
     * The third value is the class of vertex i; either true or false. If i is an inner vertex this value will be -1.
     *
     * The fourth and fifth values are the dimension and threshold of the cut of i. If i is a leaf both values
     * will be -1.
     */
    fun toFormattedString(): String {
        return IntArray(vertexCount) { it }.joinToString(separator = "|") { v ->
            val p = if (root == v) -1 else parent[v]
            val side = if (root == v) {
                "-1"
            } else if (leftChild[parent[v]] == v) {
                "l"
            } else {
                "r"
            }
            if (isLeaf(v)) {
                "$p:$side:${cla[v]}:-1:-1"
            } else {
                "$p:$side:-1:${dim[v]}:${thr[v]}"
            }
        }
    }

    fun verifyDataStructure() {
        val found = HashSet<Int>()
        lateinit var checkSubtree: (Int, Int) -> Unit
        checkSubtree = { p, curRoot ->
            if (curRoot in found) {
                throw IllegalStateException("DT is a DAG not a tree!")
            }
            found.add(curRoot)
            if (root != curRoot) {
                assert(parent[curRoot] == p)
                assert(leftChild[p] == curRoot || rightChild[p] == curRoot)
            }
            if (!isLeaf(curRoot)) {
                checkSubtree(curRoot, leftChild[curRoot])
                checkSubtree(curRoot, rightChild[curRoot])
            }
        }
        assert(root != -1)
        checkSubtree(root, root)
        assert(found.size == vertexCount)
        for (v in 0..<vertexCount) {
            assert(v in found)
        }
    }
}
