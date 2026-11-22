package com.yg.pvtodo.papers

data class Paper(
    val title: String,
    val authors: String,
    val abstract: String,
    val published: String,
    val doi: String?,
    val link: String,
    val source: String,
    val journal: String?,
    val tier: Tier = Tier.OTHER
) {
    enum class Tier { Q1, PREPRINT, OTHER }
}
