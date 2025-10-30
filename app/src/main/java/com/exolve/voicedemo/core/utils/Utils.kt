package com.exolve.voicedemo.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Utils {
    companion object{
        val CALL_NUMBER_EXTRA = "CALL_NUMBER_EXTRA"

        fun getDisplayName(context: Context, number: String): String {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return number
            }
            var name = number
            val uri = Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                Uri.encode("+$number")
            )
            val cursor = context.contentResolver.query(
                uri, arrayOf<String>(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    name = cursor.getString(idx)
                }
                cursor.close()
            }
            return name
        }

        private var _destination : MutableStateFlow<String> = MutableStateFlow("")
        val destination: StateFlow<String> = _destination.asStateFlow()
        fun navigate(destination: String) {
            _destination.value = destination
        }

    } // companion

}