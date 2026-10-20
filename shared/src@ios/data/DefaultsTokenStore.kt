package org.unesa.agc.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
class DefaultsTokenStore : TokenStore {

    override suspend fun token(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(TOKEN_KEY)

    override suspend fun save(token: String) {
        NSUserDefaults.standardUserDefaults.setObject(token, TOKEN_KEY)
    }

    override suspend fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(TOKEN_KEY)
    }

    private companion object {
        const val TOKEN_KEY = "agc.jwt"
    }
}
