import datastructures.MultiLinkedList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MultiLinkedListTest {

    private lateinit var mList1: MultiLinkedList
    private lateinit var mList2: MultiLinkedList
    private lateinit var mList3: MultiLinkedList

    @BeforeEach
    fun setup() {
        mList1 = MultiLinkedList(1, 1, 0)
        mList2 = MultiLinkedList(1, 10, 0)
        mList3 = MultiLinkedList(5, 10, 0)
        for (e in 5 until 10) {
            mList3.move(e, 1)
        }
    }

    @Test
    fun `validate start state of mList1`() {
        assertEquals(1, mList1.getSize(0))
        assertEquals(0, mList1.getListOf(0))
        assertEquals(0, mList1.getHead(0))
        assertEquals(0, mList1.getTail(0))
        assertEquals(-1, mList1.getNext(0))
        assertEquals(-1, mList1.getPrev(0))
        val iter1 = mList1.getIterator(0)
        assertTrue(iter1.hasNext())
        assertEquals(0, iter1.next())
        assertFalse(iter1.hasNext())
        var count = 0
        mList1.forEach(0) {
            count++
        }
        assertEquals(1, count)
    }

    @Test
    fun `validate start state of mList2`() {
        assertEquals(10, mList2.getSize(0))
        for (e in 0 until 10) {
            assertEquals(0, mList2.getListOf(e))
        }
        assertEquals(0, mList2.getHead(0))
        assertEquals(9, mList2.getTail(0))
        assertEquals(1, mList2.getNext(0))
        assertEquals(-1, mList2.getPrev(0))
        val iter2 = mList2.getIterator(0)
        for (e in 0 until 10) {
            assertTrue(iter2.hasNext())
            assertEquals(e, iter2.next())
        }
        assertFalse(iter2.hasNext())
        var count = 0
        mList2.forEach(0) {
            count++
        }
        assertEquals(10, count)
    }

    @Test
    fun `validate start state of mList3`() {
        assertEquals(5, mList3.getSize(0))
        assertEquals(5, mList3.getSize(1))
        assertEquals(0, mList3.getSize(2))
        assertEquals(0, mList3.getSize(3))
        assertEquals(0, mList3.getSize(4))
        for (e in 0 until 5) {
            assertEquals(0, mList3.getListOf(e))
        }
        for (e in 5 until 10) {
            assertEquals(1, mList3.getListOf(e))
        }
        assertEquals(0, mList3.getHead(0))
        assertEquals(4, mList3.getTail(0))
        assertEquals(5, mList3.getHead(1))
        assertEquals(9, mList3.getTail(1))
        assertEquals(-1, mList3.getHead(2))
        assertEquals(-1, mList3.getTail(2))
        assertEquals(-1, mList3.getHead(3))
        assertEquals(-1, mList3.getTail(3))
        assertEquals(-1, mList3.getHead(4))
        assertEquals(-1, mList3.getTail(4))
        val iter3l0 = mList3.getIterator(0)
        val iter3l1 = mList3.getIterator(1)
        val iter3l2 = mList3.getIterator(2)
        val iter3l3 = mList3.getIterator(3)
        val iter3l4 = mList3.getIterator(4)
        for (e in 0 until 5) {
            assertTrue(iter3l0.hasNext())
            assertEquals(e, iter3l0.next())
        }
        for (e in 5 until 10) {
            assertTrue(iter3l1.hasNext())
            assertEquals(e, iter3l1.next())
        }
        assertFalse(iter3l0.hasNext())
        assertFalse(iter3l1.hasNext())
        assertFalse(iter3l2.hasNext())
        assertFalse(iter3l3.hasNext())
        assertFalse(iter3l4.hasNext())
        var count0 = 0
        var count1 = 0
        var count2 = 0
        var count3 = 0
        var count4 = 0
        mList3.forEach(0) { count0++ }
        mList3.forEach(1) { count1++ }
        mList3.forEach(2) { count2++ }
        mList3.forEach(3) { count3++ }
        mList3.forEach(4) { count4++ }
        assertEquals(5, count0)
        assertEquals(5, count1)
        assertEquals(0, count2)
        assertEquals(0, count3)
        assertEquals(0, count4)
    }

    @Test
    fun `move one element to same list`() {
        mList1.move(0, 0)
        assertEquals(1, mList1.getSize(0))
        assertEquals(0, mList1.getListOf(0))
        assertEquals(0, mList1.getHead(0))
        assertEquals(0, mList1.getTail(0))
        assertEquals(-1, mList1.getNext(0))
        assertEquals(-1, mList1.getPrev(0))
        val iter1 = mList1.getIterator(0)
        assertTrue(iter1.hasNext())
        assertEquals(0, iter1.next())
        assertFalse(iter1.hasNext())
        var count = 0
        mList1.forEach(0) {
            count++
        }
        assertEquals(1, count)
    }

    @Test
    fun `move one element to different list`() {
        mList3.move(2, 2)
        assertEquals(4, mList3.getSize(0))
        assertEquals(5, mList3.getSize(1))
        assertEquals(1, mList3.getSize(2))
        assertEquals(0, mList3.getSize(3))
        assertEquals(0, mList3.getSize(4))
    }

    @Test
    fun `move all elements of a list to an empty list`() {
        mList3.moveList(1, 3)
        assertEquals(5, mList3.getSize(0))
        assertEquals(0, mList3.getSize(1))
        assertEquals(0, mList3.getSize(2))
        assertEquals(5, mList3.getSize(3))
        assertEquals(0, mList3.getSize(4))
    }

    @Test
    fun `move all elements of a list to a non empty list`() {
        mList3.moveList(0, 1)
        assertEquals(0, mList3.getSize(0))
        assertEquals(10, mList3.getSize(1))
        assertEquals(0, mList3.getSize(2))
        assertEquals(0, mList3.getSize(3))
        assertEquals(0, mList3.getSize(4))
    }
}
