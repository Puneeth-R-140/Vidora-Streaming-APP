package com.vidora.app.util

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error
    
    val isLoading: Boolean
        get() = this is Loading
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    fun getErrorMessage(): String? = when (this) {
        is Error -> message
        else -> null
    }
}

// Extension function for easy error handling
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error("No internet connection. Please check your network.", e)
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error("Connection timeout. Please try again.", e)
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "An unexpected error occurred", e)
    }
}

// Retry logic
suspend fun <T> retryApiCall(
    times: Int = 3,
    delayMillis: Long = 1000,
    apiCall: suspend () -> NetworkResult<T>
): NetworkResult<T> {
    repeat(times - 1) { attempt ->
        val result = apiCall()
        if (result.isSuccess) return result
        
        // Wait before retry
        kotlinx.coroutines.delay(delayMillis * (attempt + 1))
    }
    return apiCall() // Last attempt
}
