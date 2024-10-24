// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

// Info about a row where a Permutation may belong.
private typealias PermutationInfo = Pair<Int, Permutation>

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

    private fun test(info: PermutationInfo): PermutationInfo {
        var first = info.first
        var perm = info.second

        require(first in 0 until n) { "test asked for row $first, but only $n rows exist." }
        for (i in first until n) {
            // If i is currPerm[i], then currPerm stabilizes i, so continue.
            if (i != perm[i]) {
                // Try to get a permutation mapping i to perm[i].
                // If there is none, as currPerm maps i to currPerm[i], return for insertion in row i.
                val perm2 = getPermutation(i, perm[i]) ?: return PermutationInfo(i, perm)
                perm = perm2.inverse.compose(perm)
            }
        }

        // We reached the end: there is nothing to add.
        return PermutationInfo(n, perm)
    }

    /**
     * Try to add perm to the group. If the permutation is added, return true.
     */
    fun add(perm: Permutation) =
        enter(PermutationInfo(0, perm))

    /**
     * Non-recursive enter method for the Schreier-Sims table to avoid the possibility of a stack overflow.
     */
    private fun enter(info: PermutationInfo) {
        val stack: MutableList<Permutation> = mutableListOf(info.second)

        val (first, p) = info
        stack.add(p)

        while (stack.isNotEmpty()) {
            // Get the permutation waiting to be processed. If it does not change the table, ignore it.
            val perm = stack.removeLast()
            val results = test(PermutationInfo(first, perm))

            // If there are no rows to modify, proceed to the next permutation.
            val rowIdx = results.first
            if (rowIdx == n) continue

            var currPerm = results.second
            val colIdx = currPerm[rowIdx]
            table[rowIdx][colIdx] = currPerm

            (first..rowIdx).forEach { j ->
                table[j].values.forEach {
                    stack.add(currPerm.compose(it))
                }
            }
            ((rowIdx+1) until n).forEach { j ->
                table[j].values.forEach {
                    stack.add(it.compose(currPerm))
                }
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
