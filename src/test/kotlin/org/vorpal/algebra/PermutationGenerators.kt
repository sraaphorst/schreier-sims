// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.shuffle

object PermutationGenerators {
    fun arbPermutation(n: Int): Arb<Permutation> = Arb.shuffle((0 until n).toList()).map { shuffledList ->
        Permutation(shuffledList)
    }
}