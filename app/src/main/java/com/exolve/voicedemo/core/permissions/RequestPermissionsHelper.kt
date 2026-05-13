package com.exolve.voicedemo.core.permissions

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


sealed interface PermissionState {
    data object Granted : PermissionState
    data object Denied : PermissionState
    data object PermanentlyDenied : PermissionState
}

data class PermissionRequest(
    val permissions: List<String>,
    internal val deferred: CompletableDeferred<List<PermissionState>>
)

enum class RequestPermissionsResult {GRANTED_ALL, DENIED_ALL, GRANTED_ANY}

class RequestPermissionsHelper(private val activity: ComponentActivity) {

    private val queue = mutableListOf<PermissionRequest>()
    private var currentRequest: PermissionRequest? = null

    private val launcher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val request = currentRequest ?: return@registerForActivityResult
            currentRequest = null

            val list = results.map { (perm, granted) ->
                when {
                    granted -> PermissionState.Granted
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm) ->
                        PermissionState.PermanentlyDenied
                    else -> PermissionState.Denied
                }
            }

            request.deferred.complete(list)
            activity.lifecycleScope.launch { processNext() }
        }

    fun bind(requests: Flow<PermissionRequest>) {
        activity.lifecycleScope.launch {
            requests
                .flowWithLifecycle(activity.lifecycle, Lifecycle.State.STARTED)
                .collect { enqueue(it) }
        }
    }

    fun request(
        permissions: List<String>,
        onResult: (RequestPermissionsResult) -> Unit,
    ) {
        activity.lifecycleScope.launch {
            val deferred = CompletableDeferred<List<PermissionState>>()
            enqueue(PermissionRequest(permissions, deferred))

            val resultList = deferred.await()

            val granted = resultList.count { it is PermissionState.Granted }
            val requestPermissionResult = when {
                granted == permissions.size -> RequestPermissionsResult.GRANTED_ALL
                granted == 0 -> RequestPermissionsResult.DENIED_ALL
                else -> RequestPermissionsResult.GRANTED_ANY
            }
            onResult(requestPermissionResult)
        }
    }

    private suspend fun enqueue(request: PermissionRequest) {
        queue.add(request)
        // If queue is empty, start processing the next permission request
        if (currentRequest == null) processNext()
    }

    private suspend fun processNext() {
        val next = queue.removeFirstOrNull() ?: return
        currentRequest = next
        launcher.launch(next.permissions.toTypedArray())
    }
}
