// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import org.vorpal.algebra.PermutationGenerators.arbPermutation


class PermutationTest : StringSpec({
    "should generate valid permutations" {
        checkAll(arbPermutation(5)) { perm ->
            perm.dest.toSet() shouldBe (0 until 5).toSet()
        }
    }

    "should be invertible" {
        checkAll(arbPermutation(5)) { perm ->
            perm.compose(perm.inverse) shouldBe Permutation.identity(5)
        }
    }

    "inverse of inverse should be original permutation" {
        checkAll(arbPermutation(5)) { perm ->
            perm.inverse.inverse shouldBe perm
        }
    }

    "order permutations of order 5" {
        checkAll(arbPermutation(5)) { perm ->
            // The maximum order of a permutation in S_5 is 6, a 2-cycle and a 3-cycle.
            // Example: (0 1)(2 3 4)
            // Confirmed by Landau's sequence.
            perm.order shouldBeGreaterThanOrEqualTo 1
            perm.order shouldBeLessThanOrEqualTo 6
        }
    }

    // These tests are dependent on results from Landau's sequence:
    // https://oeis.org/A000793

    "order of permutations of order 10" {
        // The maximum order of a permutation in S_10 is 60, a 2-cycle, 3-cycle, and 5-cycle.
        // Confirmed by Landau's sequence.
        val landau10 = landauFunction(10)
        checkAll(arbPermutation(10)) { perm ->
            perm.order shouldBeGreaterThanOrEqualTo 1
            perm.order shouldBeLessThanOrEqualTo landau10
        }
    }

    "order of permutations of order 20" {
        // Landau's function gives 420. If we set the upper bound to 419, the test fails.
        val landau20 = landauFunction(20)
        checkAll(arbPermutation(20)) { perm ->
            perm.order shouldBeGreaterThanOrEqualTo 1
            perm.order shouldBeLessThanOrEqualTo landau20
        }
    }
})
