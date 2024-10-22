// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map

object GroupGenerators {
    fun arbSn(): Arb<SchreierSims> = Arb.int(1..4).map { n ->
        val g = SchreierSims(n)

        val cycle = Permutation((0 until n).map { (it + 1) % n })
        val transposition = Permutation.fromTranspositions(0 to 1).extend(n)
        g.add(cycle)
        g.add(transposition)
        g
    }
}
