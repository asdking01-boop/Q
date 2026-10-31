package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {
    // Accounts
    @Query("SELECT * FROM iptv_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<IptvAccountEntity>>

    @Query("SELECT * FROM iptv_accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): IptvAccountEntity?

    @Query("SELECT * FROM iptv_accounts WHERE isActive = 1 LIMIT 1")
    fun observeActiveAccount(): Flow<IptvAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: IptvAccountEntity): Long

    @Update
    suspend fun updateAccount(account: IptvAccountEntity)

    @Query("UPDATE iptv_accounts SET isActive = 0")
    suspend fun deactivateAllAccounts()

    @Query("UPDATE iptv_accounts SET isActive = 1 WHERE id = :accountId")
    suspend fun setActiveAccount(accountId: Long)

    @Query("DELETE FROM iptv_accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: Long)

    // Favorites
    @Query("SELECT * FROM favorite_channels WHERE accountId = :accountId ORDER BY savedAt DESC")
    fun getFavorites(accountId: Long): Flow<List<FavoriteChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    // Recents
    @Query("SELECT * FROM recent_channels WHERE accountId = :accountId ORDER BY watchedAt DESC LIMIT 20")
    fun getRecentChannels(accountId: Long): Flow<List<RecentChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentChannelEntity)

    @Query("DELETE FROM recent_channels WHERE accountId = :accountId")
    suspend fun clearRecents(accountId: Long)
}
