package com.exolve.voicedemo.core.utils

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.activity.result.ActivityResult

class Utils {
    companion object{
        val CALL_NUMBER_EXTRA = "CALL_NUMBER_EXTRA"

        fun getDisplayName(context: Context, number: String): String? {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            var name: String? = null
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

        fun getNumberFromResult(context: Context, contactResult: ActivityResult, activityName: String) : String? {
            if (contactResult.resultCode == RESULT_OK) {
                Log.d(activityName, "handleContactResult: Result is OK")
                val cursor: Cursor? = contactResult.data?.data?.let { uri ->
                    context.contentResolver.query(uri, null, null, null, null)
                }
                if (cursor != null && cursor.moveToFirst()) {
                    val numberColumnIndex: Int =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val number = cleanPhone(cursor.getString(numberColumnIndex))
                    cursor.close()
                    return number
                } else {
                    Log.d(
                        activityName, "handleContactResult: invalid cursor"
                    )
                    return null
                }
            } else {
                Log.d(activityName, "handleContactResult: Result is not OK")
                return null
            }
        }

        fun cleanPhone(raw: String) : String =
            raw.filter { it.isDigit() || it in "+,;#*" }

        private var _destination : MutableStateFlow<String> = MutableStateFlow("")
        val destination: StateFlow<String> = _destination.asStateFlow()
        fun navigate(destination: String) {
            _destination.value = destination
        }

    } // companion

}