package com.exolve.voicedemo.core.telecom

import com.exolve.voicedemo.core.repositories.SettingsRepository
import com.exolve.voicesdk.CredentialsConsumer
import com.exolve.voicesdk.ICredentialsProvider
import com.exolve.voicesdk.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TAG = CredentialsProvider::class.java.simpleName

class CredentialsProvider(
    private val settingsRepository: SettingsRepository
) : ICredentialsProvider {
    override fun requestPasswordForAccount(
        account: String,
        consumer: CredentialsConsumer
    ) {
        Log.i(TAG, "got password request for account $account")

        CoroutineScope(Dispatchers.IO).launch {
            delay((500..1500).random().toLong())
            val accountDetails = settingsRepository.fetchAccountDetails()
            if (accountDetails != null && accountDetails.number == account) {
                Log.i(TAG, "completing password request for account $account")
                consumer.providePassword(accountDetails.password)
            } else {
                Log.i(TAG, "account mismatch: expected \"$account\", found \"${accountDetails?.number}\"")
                consumer.cancel()
            }
        }
    }
}