package com.example.pagingdbremote

data class t(
    val incomplete_results: Boolean,
    val items: List<Item>,
    val total_count: Int
)