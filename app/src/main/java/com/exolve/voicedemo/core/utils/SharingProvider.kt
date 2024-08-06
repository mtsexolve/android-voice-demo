package com.exolve.voicedemo.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.exolve.voicedemo.BuildConfig
import java.io.File

private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
private const val SHARING_PROVIDER = "SharingProvider"

class SharingProvider(private val context: Context) {

    fun share(title: String) {
        Log.d(SHARING_PROVIDER, "share: filesDir = ${context.filesDir}")
        cleanup()
        copyFiles()
        val files = urisOfLogFiles()
        if (files.isEmpty()) {
            return
        }

        Intent()
            .apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    files
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "text/plain"
            }
            .also {
                context.startActivity(
                    Intent.createChooser(it, title)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    fun removeOldFiles() {
        for (prefix in arrayOf<String>("sdk", "voip")) {
            File(context.filesDir, "logs").listFiles { file -> file.name.startsWith(prefix) }
            ?.sorted()?.dropLast(1)?.forEach() { it.delete() }
        }
    }

    private fun copyFiles() {
        val files = File(context.filesDir, "logs").listFiles { file ->
            file.length() > 0 && (file.name.startsWith("sdk") || file.name.startsWith("voip"))
        }

        if (files != null) {
            val tempDir = File(context.cacheDir, "temp")
            for (file in files) {
                file.copyTo(File(tempDir, file.name))
            }
        }
    }

    private fun cleanup() {
        val tempDir = File(context.cacheDir, "temp")
        tempDir.deleteRecursively()
    }

    private fun urisOfLogFiles(): ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        File(context.cacheDir, "temp").listFiles { file ->
            file.length() > 0 && (file.name.startsWith("sdk") || file.name.startsWith("voip"))
        }?.sorted()?.forEach { file ->
            uris.add(FileProvider.getUriForFile(context, AUTHORITY, file))
        }
        return uris
    }

}
