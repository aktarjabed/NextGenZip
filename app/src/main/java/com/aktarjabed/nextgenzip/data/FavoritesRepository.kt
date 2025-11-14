package com.aktarjabed.nextgenzip.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.aktarjabed.nextgenzip.data.models.FavoriteArchives
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

object FavoriteArchivesSerializer : Serializer<FavoriteArchives> {
    override val defaultValue: FavoriteArchives = FavoriteArchives.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FavoriteArchives {
        try {
            return FavoriteArchives.parseFrom(input)
        } catch (exception: com.google.protobuf.InvalidProtocolBufferException) {
            throw androidx.datastore.core.CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: FavoriteArchives, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.favoriteArchivesDataStore: DataStore<FavoriteArchives> by dataStore(
    fileName = "favorite_archives.pb",
    serializer = FavoriteArchivesSerializer
)

class FavoritesRepository(private val context: Context) {

    val favoriteArchives: Flow<List<String>> =
        context.favoriteArchivesDataStore.data.transform { it.pathsList }

    suspend fun addFavorite(path: String) {
        context.favoriteArchivesDataStore.updateData { list ->
            list.toBuilder().addPaths(path).build()
        }
    }

    suspend fun removeFavorite(path: String) {
        context.favoriteArchivesDataStore.updateData { list ->
            val currentPaths = list.pathsList.toMutableList()
            currentPaths.remove(path)
            list.toBuilder().clearPaths().addAllPaths(currentPaths).build()
        }
    }
}
