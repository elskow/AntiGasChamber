package org.unesa.agc.data

interface TokenStore {
    suspend fun token(): String?
    suspend fun save(token: String)
    suspend fun clear()
}
