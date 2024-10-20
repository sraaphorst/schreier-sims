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

    "order of permutation should be within size of domain" {
        checkAll(arbPermutation(5)) { perm ->
            perm.order shouldBeGreaterThanOrEqualTo 0
            perm.order shouldBeLessThanOrEqualTo 5
        }
    }
})
