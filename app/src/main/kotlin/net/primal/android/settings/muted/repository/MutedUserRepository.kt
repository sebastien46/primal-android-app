package net.primal.android.settings.muted.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import net.primal.android.db.PrimalDatabase
import net.primal.android.nostr.ext.asProfileDataPO
import net.primal.android.nostr.ext.flatMapNotNullAsMediaResourcePO
import net.primal.android.settings.api.SettingsApi
import net.primal.android.settings.muted.db.MutedUserData
import javax.inject.Inject

class MutedUserRepository @Inject constructor(
    private val database: PrimalDatabase,
    private val settingsApi: SettingsApi,
) {

    fun observeMutedUsers() = database.muted().observeMutedUsers()

    fun observeIsUserMuted(pubkey: String) = database.muted().observeIsUserMuted(pubkey = pubkey)

    suspend fun fetchAndPersistMuteList(userId: String) {
        val muteList = fetchMuteListAndPersistProfiles(userId = userId)
        persistMuteList(muteList = muteList)
    }

    suspend fun muteUserAndPersistMuteList(
        userId: String,
        mutedUserId: String,
    ) {
        val userMetadataEventId = withContext(Dispatchers.IO) {
            database.profiles().findMetadataEventId(mutedUserId)
        }
        updateAndPersistMuteList(userId = userId) {
            toMutableSet().apply {
                add(
                    MutedUserData(
                        userId = mutedUserId,
                        userMetadataEventId = userMetadataEventId,
                    )
                )
            }
        }
    }

    suspend fun unmuteUserAndPersistMuteList(
        userId: String,
        unmutedUserId: String,
    ) {
        updateAndPersistMuteList(userId = userId) {
            toMutableSet().apply {
                removeIf { it.userId == unmutedUserId }
            }
        }
    }

    private suspend fun updateAndPersistMuteList(
        userId: String,
        reducer: Set<MutedUserData>.() -> Set<MutedUserData>,
    ) {
        val remoteMuteList = fetchMuteListAndPersistProfiles(userId = userId)
        val newMuteList = remoteMuteList.reducer()
        settingsApi.setMuteList(userId = userId, muteList = newMuteList.map { it.userId }.toSet())
        persistMuteList(muteList = newMuteList)
    }

    private suspend fun fetchMuteListAndPersistProfiles(userId: String): Set<MutedUserData> {
        val response = withContext(Dispatchers.IO) { settingsApi.getMuteList(userId = userId) }
        val muteList = response.muteList?.tags?.mapToPubkeySet() ?: emptySet()
        val profileData = response.metadataEvents.map { it.asProfileDataPO() }
        val mediaResources = response.eventResources.flatMapNotNullAsMediaResourcePO()
        withContext(Dispatchers.IO) {
            database.withTransaction {
                database.profiles().upsertAll(data = profileData)
                database.mediaResources().upsertAll(data = mediaResources)
            }
        }
        return muteList
            .map { mutedUserId ->
                mutedUserId.asMutedAccountPO(
                    metadataEventId = profileData.find { mutedUserId == it.ownerId }?.eventId
                )
            }
            .toSet()
    }

    private suspend fun persistMuteList(muteList: Set<MutedUserData>) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                database.muted().deleteAll()
                database.muted().upsertAll(data = muteList)
            }
        }
    }

    private fun String.asMutedAccountPO(metadataEventId: String? = null): MutedUserData =
        MutedUserData(userId = this, userMetadataEventId = metadataEventId)

    private fun List<JsonArray>?.mapToPubkeySet(): Set<String>? {
        return this?.filter { it.size == 2 }?.map { it[1].jsonPrimitive.content }?.toSet()
    }

}