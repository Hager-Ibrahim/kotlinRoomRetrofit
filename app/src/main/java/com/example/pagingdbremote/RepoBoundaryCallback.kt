package com.example.pagingdbremote

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.example.pagingdbremote.db.RepoDatabase
import com.example.pagingdbremote.model.Repo
import com.example.pagingdbremote.model.RepoSearchResponse
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepoBoundaryCallback(private val context: Context,
                           private val query: String) : PagedList.BoundaryCallback<Repo>() {

    private var isRequestInProgress = false
    private var lastRequestedPage = 1
    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
    val database = RepoDatabase.getInstance(context)
    private val _networkErrors = MutableLiveData<String>()
    val networkErrors: LiveData<String>
        get() = _networkErrors
    private  val TAG = "GithubService"
    private  val IN_QUALIFIER = "in:name,description"

    override fun onZeroItemsLoaded() {
       requestAndSaveData(query)
    }

    override fun onItemAtEndLoaded(itemAtEnd: Repo) {
        requestAndSaveData(query)
    }

    private fun requestAndSaveData(query: String){
        if(isRequestInProgress) return

        isRequestInProgress = true
        searchRepos(GithubService.create(),query,lastRequestedPage,NETWORK_PAGE_SIZE,{
            Log.i("oooooo", "requestAndSaveData: " + it.size)
            insert(it){
                lastRequestedPage++
                isRequestInProgress = false
            }
        },{
            isRequestInProgress = false
            _networkErrors.postValue(it)
        })

    }


    fun searchRepos(
        service: GithubService,
        query: String,
        page: Int,
        itemsPerPage: Int,
        onSuccess: (repos: List<Repo>) -> Unit,
        onError: (error: String) -> Unit
    ) {
        Log.d(TAG, "query: $query, page: $page, itemsPerPage: $itemsPerPage")

        val apiQuery = query + IN_QUALIFIER

        service.searchRepos(apiQuery, page, itemsPerPage).enqueue(
            object : Callback<RepoSearchResponse> {
                override fun onFailure(call: Call<RepoSearchResponse>?, t: Throwable) {
                    Log.d(TAG, "fail to get data")
                    onError(t.message ?: "unknown error")
                }

                override fun onResponse(
                    call: Call<RepoSearchResponse>?,
                    response: Response<RepoSearchResponse>
                ) {
                    Log.e("99999999999", "onResponse: " + response.body().toString())

                    if (response.isSuccessful) {
                        val repos = response.body()?.repos ?: emptyList()
                        onSuccess(repos)
                    } else {
                        onError(response.errorBody()?.string() ?: "Unknown error")
                    }


                }
            }
        )
    }

    /*
    private fun searchRepos(query: String,
                            page: Int,
                            perPage: Int,
                            onSuccess: (repos: List<Repo>) -> Unit,
                            onError: (error: String) -> Unit){

        val apiQuery = query + "in:name,description"

        GithubService.create().searchRepos(apiQuery,page,perPage).enqueue(object :Callback<RepoSearchResponse>{
            override fun onFailure(call: Call<RepoSearchResponse>, t: Throwable) {
                t.message?.let {
                    onError(it)
                }
            }

            override fun onResponse(
                call: Call<RepoSearchResponse>,
                response: Response<RepoSearchResponse>) {
                Log.e("99999999999", "onResponse: " + response.body().toString())
                Log.i("response", "onResponse: " + Gson().toJson(response.body()))

                if (response.isSuccessful){
                    //response.body()?.repos
                    response.body()?.repos?.let {
                        onSuccess(it) }
                }
                else{
                    response.errorBody()?.string()?.let {
                        onError(it) }
                }
            }

        })
    }


     */
    fun insert(repos: List<Repo>, insertFinished: () -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("GithubLocalCache", "inserting ${repos.size} repos")
            database.reposDao().insert(repos)
            insertFinished()
        }

    }
}