// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra


data class Permutation(val dest: List<Int>) {
    init {
        require(isBijection(dest)) { "$dest does not represent a valid permutation." }
    }

    val inverse: Permutation by lazy {
        // We use a mutable list because this reduces the efficiency to O(n).
        // Using indexOf would increase the time to O(n^2).
        val inv = MutableList(dest.size) { 0 }
        dest.forEachIndexed { index, value -> inv[value] = index }
        Permutation(inv)
    }

    val stabilizer: Set<Int> by lazy {
        IntRange(0, dest.size - 1).filter(this::isStabilized).toSet()
    }

    val order: Int by lazy {
        val ident = identity(dest.size)
        fun aux(curr: Permutation = this, n: Int = 0): Int =
            if (curr == ident) n
            else aux(curr.compose(this), n + 1)
        aux()
    }

    /**
     * The permutation represented by cycles of size > 1.
     */
    val cycles: List<List<Int>> by lazy {
        // Find the cycle that begins with startElem.
        // We use a mutable list here for efficiency: the initial implementation had a
        // tailrec function with an immutable list cycle that built up, but the copying requires
        // an O(n) operation.
        fun findCycle(startElem: Int): List<Int> {
            val cycle = mutableListOf<Int>()
            var currElem = startElem
            do {
                cycle.add(currElem)
                currElem = this[currElem]
            } while (currElem != startElem)
            return cycle
        }

        // Auxiliary function to find all cycles
        tailrec fun aux(remaining: Set<Int> = dest.indices.toSet(), cycles: List<List<Int>> = emptyList()): List<List<Int>> =
            if (remaining.isEmpty()) cycles
            else {
                val startElem: Int = remaining.first()
                val newCycle: List<Int> = findCycle(startElem)
                val newRemaining: Set<Int> = remaining - newCycle.toSet()
                val newCycles: List<List<Int>> = if (newCycle.size == 1) cycles else cycles + listOf(newCycle)
                aux(newRemaining, newCycles)
            }

        aux()
    }

    /**
     * Determine the cycle types for cycles of length > 1.
     */
    val cycleType: List<Int> by lazy {
        cycles.map { it.size }.sorted()
    }

    /**
     * Calculate the nth power of this permutation.
     * For n = 0, we simply get the identity, and for n = 1, we simply get this permutation.
     */
    fun pow(n: Int): Permutation {
        tailrec fun aux(curr: Permutation = identity(dest.size), currN: Int = n): Permutation =
            if (currN == 0) curr
            else aux(this.compose(curr), currN - 1)
        return aux()
    }

    operator fun get(x: Int): Int {
        require(x in dest.indices) { "Attempting to apply permutation $dest to $x."}
        return dest[x]
    }

    /**
     * Apply the permutation to a collection of elements.
     */
    operator fun get(xs: Collection<Int>): List<Int> =
        xs.map { get(it) }


    /**
     * This results in a permutation that performs this(inner(x)).
     */
    fun compose(inner: Permutation): Permutation =
        if (this == identity(dest.size)) inner
        else if (inner == identity(dest.size)) this
        else Permutation((0 until dest.size).map { this[inner[it]] })

    /**
     * This results in a permutation that performs outer(this(x)).
     */
    fun andThen(outer: Permutation): Permutation =
        if (this == identity(dest.size)) outer
        else if (outer == identity(dest.size)) this
        else Permutation(IntRange(0, dest.size).map { outer[this[it]] })

    /**
     * Schreier-Sims algorithms rely on conjugating permutations, e.g.
     * p^{-1} * q * p.
     * This simplifies things by allowing conjugation directly.
     */
    fun conjugateBy(other: Permutation): Permutation =
        inverse.compose(other).compose(this)

    /**
     * leftAction is used by Schreier-Sims for computing orbits and stabilizers and tracking
     * how elements move under the group's actions. This is just a convenience method that applies
     * the permutation to an element.
     */
    fun leftAction(x: Int): Int = this[x]

    /**
     * rightAction, aka coset representative decomposition.
     * When working with coset representatives, or to express an element in terms of a strong
     * generating set, we must compose from the right. This is a convenience method that
     * is equvalent to andThen(outer) applied to x].
     */
    fun rightAction(x: Int, outer: Permutation): Int = outer[this[x]]


    /**
     * Determine if a point x is stabilized by this perm.
     */
    fun isStabilized(x: Int): Boolean {
        require(x in dest.indices) { "$x cannot be stabilized by permutation $dest."}
        return dest[x] == x
    }

    /**
     * Determine the orbit of the element x in this, i.e. the set of elements that
     * x can be reached by applying this repeatedly.
     */
    fun orbit(x: Int): Set<Int> {
        require(x in dest.indices) { "$x is not in the permutation $dest." }
        tailrec fun aux(orb: Set<Int> = setOf(x), currX: Int = x): Set<Int> {
            val next = this[currX]
            if (next in orb) return orb
            else return aux(orb + next, next)
        }
        return aux(setOf(x))
    }

    /**
     * Determine if two elements are in the same orbit.
     */
    fun sameOrbit(x: Int, y: Int): Boolean {
        require(x in dest.indices) { "$x is not in the permutation $dest." }
        require(y in dest.indices) { "$y is not in the permutation $dest." }
        return orbit(x).contains(y)
    }

    /**
     * Find the orbit of a collection of elements.
     */
    fun orbit(xs: Collection<Int>): Set<Int> =
        xs.fold(emptySet()) { acc, x -> acc + orbit(x) }

    companion object {
        private fun isBijection(dest: List<Int>): Boolean =
            dest.toSet() == IntRange(0, dest.size - 1).toSet()

        private val identities: MutableMap<Int, Permutation> = mutableMapOf()
        fun identity(n: Int): Permutation {
            require(n > 0) {"Trying to take identity of permutation over $n elements."}
            return identities.getOrPut(n) { Permutation((0 until n).toList()) }
        }

        fun fromTranspositions(transpositions: Collection<Pair<Int, Int>>): Permutation {
            val n = transpositions.maxOf { maxOf(it.first, it.second) } + 1
            val dest = (0 until n).toMutableList()
            transpositions.forEach { (a, b) ->
                val tmp = dest[a]
                dest[a] = dest[b]
                dest[b] = tmp
            }
            return Permutation(dest)
        }

        fun fromTranspositions(vararg transpositions: Pair<Int, Int>): Permutation =
            fromTranspositions(transpositions.toList())
    }
}

fun main() {
    println("Hello")
}
