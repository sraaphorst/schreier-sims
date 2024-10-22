// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map

object GroupGenerators {
    private fun cycle(n: Int): Permutation =
        Permutation((0 until n).map { (it + 1) % n })

    fun arbSn(): Arb<SchreierSims> = Arb.int(1..10).map { n ->
        val g = SchreierSims(n)
        g.add(cycle(n))
        if (n > 1) g.add(Permutation.fromTranspositions(0 to 1).extend(n))
        g
    }

    fun arbAn(): Arb<SchreierSims> = Arb.int(2..10).map { n ->
        val g = SchreierSims(n)
        for (i in 2 until n) {
            val perm = (0 until n).toMutableList()
            perm[0] = 1
            perm[1] = i
            perm[i] = 0
            g.add(Permutation(perm))
        }
        g
    }

    fun arbCn(): Arb<SchreierSims> = Arb.int(2..10).map { n ->
        val g = SchreierSims(n)
        g.add(cycle(n))
        g
    }
}
