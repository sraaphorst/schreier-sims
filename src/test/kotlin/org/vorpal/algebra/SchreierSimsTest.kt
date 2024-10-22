// By Sebastian Raaphorst, 2024.

package org.vorpal.algebra

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import org.vorpal.algebra.GroupGenerators.arbAn
import org.vorpal.algebra.GroupGenerators.arbCn
import org.vorpal.algebra.GroupGenerators.arbSn

class SchreierSimsTest : StringSpec({
    "Sn should contain n! permutations" {
        checkAll(arbSn()) { gp ->
            gp.groupSize() shouldBe factorial(gp.n)
        }
    }

    "An should contain n!/2 permutations" {
         checkAll(arbAn()) { gp ->
             gp.groupSize() shouldBe factorial(gp.n) / 2
         }
    }

    "Cn should contain n permutations" {
        checkAll(arbCn()) { gp ->
            gp.groupSize() shouldBe gp.n
        }
    }
})
