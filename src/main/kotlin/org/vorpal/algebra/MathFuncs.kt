package org.vorpal.algebra

import kotlin.math.abs

fun lcm(a: Int, b: Int): Int =
    (abs(a) / gcd(a, b)) * abs(b)

tailrec fun gcd(a: Int, b: Int): Int =
    if (b == 0) abs(a) else gcd(b, a % b)

private val cachedFactorials = mutableMapOf<Int, Long>()
fun factorial(n: Int): Long {
    require(n >= 0) { "Cannot calculate $n!."}
    return cachedFactorials.getOrPut(n) {
        if (n == 0) 1 else n * factorial(n - 1)
    }
}

fun lcm(nums: List<Int>): Int {
    return nums.reduce { acc, x -> lcm(acc, x) }
}

/**
 * Generates all integer partitions of a number n.
 */
fun partitions(n: Int): List<List<Int>> {
    fun partitionHelper(n: Int, max: Int): List<List<Int>> {
        if (n == 0) return listOf(emptyList())
        val result = mutableListOf<List<Int>>()
        for (i in 1..minOf(n, max)) {
            result += partitionHelper(n - i, i).map { listOf(i) + it }
        }
        return result
    }
    return partitionHelper(n, n)
}

/**
 * Function to calculate Landau's function g(n).
 * This function returns the maximum LCM of any partition of n.
 */
fun landauFunction(n: Int): Int {
    // Get all partitions of n.
    val parts = partitions(n)

    // Calculate the LCM for each partition and return the maximum LCM.
    return parts.map { partition -> lcm(partition) }.maxOrNull() ?: 1
}