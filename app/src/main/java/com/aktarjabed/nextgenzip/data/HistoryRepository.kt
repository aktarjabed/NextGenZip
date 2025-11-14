package com.aktarjabed.nextgenzip.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.aktarjabed.nextgenzip.data.models.ArchiveHistoryEntry
import com.aktarjabed.nextgenzip.data.models.ArchiveHistoryEntryList
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

object ArchiveHistorySerializer : Serializer<ArchiveHistoryEntryList> {
    override val defaultValue: ArchiveHistoryEntryList = ArchiveHistoryEntryList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ArchiveHistoryEntryList {
        try {
            return ArchiveHistoryEntryList.parseFrom(input)
        } catch (exception: com.google.protobuf.InvalidProtocolBufferException) {
            throw androidx.datastore.core.CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ArchiveHistoryEntryList, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.archiveHistoryDataStore: DataStore<ArchiveHistoryEntryList> by dataStore(
    fileName = "archive_history.pb",
    serializer = ArchiveHistorySerializer
)

class HistoryRepository(private val context: Context) {

    val archiveHistory: Flow<List<ArchiveHistoryEntry>> =
        context.archiveHistoryDataStore.data.transform { it.entriesList }

    suspend fun addEntry(entry: ArchiveHistoryEntry) {
        context.archiveHistoryDataStore.updateData { list ->
            list.toBuilder().addEntries(entry).build()
        }
    }
}
