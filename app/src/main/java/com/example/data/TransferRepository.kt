package com.example.data

import kotlinx.coroutines.flow.Flow

class TransferRepository(private val transferDao: TransferDao) {
    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()

    suspend fun insert(transfer: TransferEntity) {
        transferDao.insertTransfer(transfer)
    }

    suspend fun clearAll() {
        transferDao.clearHistory()
    }
}
