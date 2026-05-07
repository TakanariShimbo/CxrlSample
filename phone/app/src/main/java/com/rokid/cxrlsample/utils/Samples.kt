package com.rokid.cxrlsample.utils

/**
 * Collection of algorithm samples (decoupled from main business flow).
 */
class Samples {

    /**
     * Finds index pairs whose values sum to `sum`.
     */
    fun findOutSum(byteArray: IntArray, sum: Int = 0): List<Pair<Int, Int>>{
        if (byteArray.size < 2) return emptyList()

        val hashMap = HashMap<Int, MutableList<Int>>()
        val result = ArrayList<Pair<Int, Int>>()
        for (i in byteArray.indices) {
            val complement = sum - byteArray[i]

            hashMap[complement]?.forEach {
                result.add(Pair(it, i))
            }
            hashMap.getOrPut(byteArray[i]){mutableListOf()}.add(i)
        }
        return result
    }

    /**
     * Finds longest length of consecutive values that can be formed from array.
     * Numbers may contain duplicates.
     * Input array can be unordered.
     *
     */
    fun findLongestContinuousArray(byteArray: IntArray): Int{
        if (byteArray.isEmpty()) return 0
        var maxLength = 1

        val numSet = byteArray.toSet()
        for (num in numSet){
            if (numSet.contains(num - 1)) continue
            var currentLength = 1
            while (numSet.contains(num + currentLength)){
                currentLength ++
            }
            maxLength = maxOf(maxLength, currentLength)
        }
        return maxLength
    }

}