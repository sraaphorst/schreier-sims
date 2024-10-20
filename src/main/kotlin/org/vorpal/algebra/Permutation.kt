// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

import java.util.concurrent.ConcurrentHashMap


data class Permutation(val dest: List<Int>) {
    init {
        require(isBijection(dest)) { "$dest does not represent a valid permutation." }
    }

    val size: Int = dest.size

    val inverse: Permutation by lazy {
        invertWithCache(this)
    }

    val stabilizer: Set<Int> by lazy {
        IntRange(0, dest.size - 1).filter(this::isStabilized).toSet()
    }

    /**
     * The order of a permutation is the least common multiple of its cycles, or 1 if it is the identity,
     * in which case all the cycles are of length 1.
     */
    val order: Int by lazy {
        if (isIdentity) 1 else cycles.map { it.size }.reduce(::lcm)
    }

    /**
     * More efficient method to check for identity. It will short-circuit as soon as a deviation is found.
     */
    val isIdentity: Boolean by lazy {
        dest.indices.all { dest[it] == it }
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

        // Auxiliary function to find all cycles.
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
     * Extend the permutation from {0, ..., n-1} to {0, ..., m-1} for m >= n, making the elements
     * from {n, ..., m-1} the identity.
     */
    fun extend(m: Int): Permutation {
        require(m >= size) { "Cannot extend permutation of size $size to $m." }
        if (m == size) return this
        return Permutation(dest + (size until m).map { it })
    }

    /**
     * Calculate the nth power of this permutation.
     * For n = 0, we simply get the identity, and for n = 1, we simply get this permutation.
     * We use pow by squaring to improve efficiency.
     */
    fun pow(n: Int): Permutation {
        require(n >= 0) { "Cannot take permutation to power $n." }
        var result = identity(size)
        var base = this
        var exponent = n

        while (exponent > 0) {
            if (exponent % 2 == 1) {
                result = result.compose(base)  // Multiply by base when the exponent is odd
            }
            base = base.compose(base)  // Square the base
            exponent /= 2  // Divide the exponent by 2
        }

        return result
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
        composeWithCache(this, inner)

    /**
     * This results in a permutation that performs outer(this(x)).
     */
    fun andThen(outer: Permutation): Permutation =
        composeWithCache(outer, this)

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
        /**
         * Efficient short-circuited bijection check.
         */
        private fun isBijection(dest: List<Int>): Boolean {
            tailrec fun aux(seen: BooleanArray = BooleanArray(dest.size), idx: Int = 0): Boolean {
                if (idx == dest.size) return true
                val img = dest[idx]
                if (seen[img] || img !in 0 until dest.size) return false
                seen[img] = true
                return aux(seen, idx + 1)
            }
            return aux()
        }

        private val identities = mutableMapOf<Int, Permutation>()
        fun identity(n: Int): Permutation {
            require(n > 0) {"Trying to take identity of permutation over $n elements."}
            return identities.getOrPut(n) { Permutation((0 until n).toList()) }
        }

        fun fromTranspositions(transpositions: Collection<Pair<Int, Int>>): Permutation {
            // The largest value in a transposition determines the length of the permutation.
            val n = transpositions.maxOf { maxOf(it.first, it.second) } + 1
            transpositions.forEach { (a, b) ->
                require(a in 0 until n && b in 0 until n && a != b) {
                    "Illegal transposition found: ($a $b)."
                }
            }
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

        /**
         * Cached permutation inversion. We use a ConcurrentHashMap to make this operation thread-safe.
         */
        private val invertedPermCache = ConcurrentHashMap<Permutation, Permutation>()
        private fun invertWithCache(perm: Permutation): Permutation =
            invertedPermCache.computeIfAbsent(perm) {
                val inv = MutableList(perm.size) { 0 }
                perm.dest.forEachIndexed { index, value -> inv[value] = index }
                Permutation(inv)
            }

        /**
         * Cached permutation composition. We use a ConcurrentHashMap to make this operation thread-safe.
         */
        private val composedPermCache = ConcurrentHashMap<Pair<Permutation, Permutation>, Permutation>()
        private fun composeWithCache(perm1: Permutation, perm2: Permutation): Permutation =
            composedPermCache.computeIfAbsent(perm1 to perm2) { compose(perm1, perm2) }
        private fun compose(perm1: Permutation, perm2: Permutation): Permutation {
            require(perm1.size == perm2.size) { "Cannot compose permutations of different sizes ${perm1.size} and ${perm2.size}." }
            return if (perm1 == identity(perm1.size)) perm2
            else if (perm2 == identity(perm2.size)) perm1
            else Permutation((0 until perm1.size).map { perm1[perm2[it]] })
        }
    }
}
