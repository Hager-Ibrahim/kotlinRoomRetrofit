package com.example.pagingdbremote

import android.content.Context
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import com.example.pagingdbremote.db.RepoDatabase
import com.example.pagingdbremote.model.Repo
import com.example.pagingdbremote.model.RepoSearchResult

class GithubRepository(private val context: Context) {

    private val database = RepoDatabase.getInstance(context)
    companion object {
        private const val DATABASE_PAGE_SIZE = 50
    }

    fun search(query: String) : RepoSearchResult{
        val dataSourceFactory = reposByName(query)
        val gitBoundaryCallback = RepoBoundaryCallback(context,query)
        val networkErrors = gitBoundaryCallback.networkErrors

        // Get the paged list
        val data = LivePagedListBuilder(dataSourceFactory, DATABASE_PAGE_SIZE)
            .setBoundaryCallback(gitBoundaryCallback)
            .build()

        return RepoSearchResult(data, networkErrors)
    }
    private fun reposByName(name: String): DataSource.Factory<Int, Repo> {
        // appending '%' so we can allow other characters to be before and after the query string
        val query = "%${name.replace(' ', '%')}%"
        return database.reposDao().reposByName(query)
    }
}