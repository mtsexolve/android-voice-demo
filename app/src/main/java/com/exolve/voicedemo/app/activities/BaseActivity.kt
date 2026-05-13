package com.exolve.voicedemo.app.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.exolve.voicedemo.core.permissions.RequestPermissionsHelper
import com.exolve.voicedemo.core.permissions.RequestPermissionsResult
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.exolve.voicedemo.core.uiCommons.BaseViewModel


abstract class BaseActivity : ComponentActivity() {
    private lateinit var requestPermissionsHelper: RequestPermissionsHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsHelper = RequestPermissionsHelper(this)
    }

    protected fun bindViewModelForPermissions(viewModel: BaseViewModel<* ,* ,*>) {
        requestPermissionsHelper.bind(viewModel.permissionRequests)
    }

    protected fun requestPermissions(permissions: List<String>, onResult: (state: RequestPermissionsResult) -> Unit = {}) {
        if(permissions.all { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED } ) {
            onResult(RequestPermissionsResult.GRANTED_ALL)
        } else if(permissions.any { ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED } ) {
            onResult(RequestPermissionsResult.GRANTED_ANY)
        } else {
            requestPermissionsHelper.request(permissions, onResult)
        }
    }
}