package com.example.pagingdbremote.model

import com.google.gson.annotations.SerializedName

data class RepoSearchResponse(
    val incomplete_results: Boolean,
    @SerializedName("items")val repos: List<Repo>,
    @SerializedName("total_count")val total_count: Int
)

