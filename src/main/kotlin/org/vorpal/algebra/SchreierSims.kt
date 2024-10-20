// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

/**
 * A Schreier-Sims representation of a finite permutation group.
 * The group is a subgroup of S_n.
 */
class SchreierSims(val n: Int) {
    /**
     * Table is a table of rows of permutations that comprise a set of permutations that
     * stabilize ever-increasing number of elements in the permutation group G ≤ S_n, where
     * the elements S_n acts on are considered to be Z_n.
     * G = G_0 ⊇ G_1 ⊇ ... ⊇ G_{n-1}.
     * The elements in row i is the subgroup that stabilizes the first i elements.
     * An element at position table[i][j] stabilizes the elements {0, ... i-1} ana maps
     * the element j to table[i][j].
     */
    private val table: Array<MutableMap<Int, Permutation>> = Array(n) { mutableMapOf() }

    /**
     * Return a permutation that maps src to dst, if one exists in the stabilizer table.
     */
    fun getPermutation(src: Int, dst: Int): Permutation? {
        require(src in 0 until n) { "getPermutation asked for row $src, but only $n rows exist." }
        require(dst in 0 until n) { "getPermutation asked for col $dst, but only $n cols exist." }
        if (src == dst) return Permutation.identity(n)
        return table[src][dst]
    }

    private fun test(perm: Permutation, first: Int): Int {
        var currPerm = perm
        require(first in 0 until n) { "test asked for row $first, but only $n rows exist." }
        for (i in first until n) {
            // If i is perm[i], then we just use the identity and skip.
            if (i != perm[i]) {
                // Try to get a permutation mapping i to perm[i].
                val iPerm = getPermutation(i, perm[i])

                // If there is no such perm, then we know where the perm needs to be put.
                if (iPerm == null) return i

                // Otherwise, proceed to the next row and calculate what the next mapping
                // must be for the permutation.
                currPerm = iPerm.inverse.compose(currPerm)
            }
        }

        // We reached the end: there is nothing to add.
        return n
    }

    /**
     * Try to add perm to the group. If the permutation is added, return true.
     */
    fun add(perm: Permutation): Boolean {
        val rowIdx = test(perm, 0)
        if (rowIdx == n) return false
        enter(perm)
        return true
    }

    /**
     * Non-recursive enter method for the Schreier-Sims table to avoid the possibility of a stack overflow.
     */
    private fun enter(perm: Permutation) {
        val stack: MutableList<Pair<Int, Permutation>> = mutableListOf(0 to perm)
        while (stack.isNotEmpty()) {
            // Get the next permutation waiting to be processed. If it does not change the table, ignore it.
            val (currFirst, currPerm) = stack.removeLast()
            val rowIdx = test(currPerm, currFirst)

            // If there are no rows to modify, proceed to the next permutation.
            if (rowIdx == n) continue

            // Insert the current permutation into the table.
            table[rowIdx][currPerm[rowIdx]] = currPerm

            // Determine the permutations that we have to insert.
            (currFirst..rowIdx).forEach { j ->
                val row = table[j]
                if (!row.containsKey(currPerm[j]))
                    row.values.forEach { perm2 -> stack.add(j to currPerm.compose(perm2)) }
            }
            (rowIdx until n).forEach { j ->
                val row = table[j]
                if (!row.containsKey(currPerm[j]))
                    row.values.forEach { perm2 -> stack.add(j to perm2.compose(currPerm)) }
            }
        }
    }

    /**
     * The number of strong generators for the group.
     */
    fun numGenerators(): Int =
        (0 until n).map { table[it].size }.sum()

    /**
     * Get the strong generators of the group.
     */
    fun getStrongGenerators(): Set<Permutation> =
        table.flatMap { it.values }.toSet()

    /**
     * The total number of permutations in this subgroup of S_n.
     * We have to include the identity permutations in here, so we add 1 to each table row.
     */
    fun groupSize(): Int =
        (0 until n).fold(1) { acc, i -> acc * (table[i].size + 1) }

    /**
     * Generate all of the permutations in the group lazily using coroutines.
     */
    fun generatePermutations(): Sequence<Permutation> = sequence {
        val identity = Permutation.identity(n)

        // Use a stack to perform a depth-first search through the table.
        val stack = mutableListOf(identity)

        // Keep track of generated permutations.
        val seen: MutableSet<Permutation> = mutableSetOf()

        while (stack.isNotEmpty()) {
            val currPerm = stack.removeLast()

            // Do not return permutations that we have already seen.
            if (currPerm in seen) continue
            seen.add(currPerm)

            yield(currPerm)

            (0 until n).forEach { i ->
                table[i].values.forEach { perm ->
                    val newPerm = currPerm.compose(perm)
                    if (newPerm !in seen) stack.add(newPerm)
                }
            }
        }
    }
}
