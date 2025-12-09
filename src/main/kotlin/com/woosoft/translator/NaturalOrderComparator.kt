package com.woosoft.translator

import java.util.Comparator

object NaturalOrderComparator : Comparator<String> {
    private val SPLIT_REGEX = """(?<=\D)(?=\d)|(?<=\d)(?=\D)""".toRegex()

    override fun compare(a: String?, b: String?): Int {
        if (a == null || b == null) {
            return (a ?: "").compareTo(b ?: "")
        }

        val partsA = a.split(SPLIT_REGEX)
        val partsB = b.split(SPLIT_REGEX)

        val shortestLength = minOf(partsA.size, partsB.size)

        for (i in 0 until shortestLength) {
            val partA = partsA[i]
            val partB = partsB[i]

            val numA = partA.toLongOrNull()
            val numB = partB.toLongOrNull()

            if (numA != null && numB != null) {
                if (numA != numB) {
                    return numA.compareTo(numB)
                }
            } else {
                val cmp = partA.compareTo(partB)
                if (cmp != 0) {
                    return cmp
                }
            }
        }

        return partsA.size.compareTo(partsB.size)
    }
}
