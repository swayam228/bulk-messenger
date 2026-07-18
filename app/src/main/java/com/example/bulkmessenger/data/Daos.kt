package com.example.bulkmessenger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles")
    suspend fun getAll(): List<UserProfile>

    @Insert
    suspend fun insert(user: UserProfile): Long

    @Update
    suspend fun update(user: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getById(id: Long): UserProfile?

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAll()
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAll(userId: Long): Flow<List<Draft>>

    @Query("SELECT * FROM drafts")
    suspend fun getAll(): List<Draft>

    @Insert
    suspend fun insert(draft: Draft): Long

    @Update
    suspend fun update(draft: Draft)

    @Delete
    suspend fun delete(draft: Draft)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()
}

@Dao
interface BulkJobDao {
    @Insert
    suspend fun insertJob(job: BulkJob): Long

    @Insert
    suspend fun insertItems(items: List<BulkJobItem>): List<Long>

    @Update
    suspend fun updateJob(job: BulkJob)

    @Update
    suspend fun updateItem(item: BulkJobItem)

    @Query("SELECT * FROM bulk_jobs WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeJobs(userId: Long): Flow<List<BulkJob>>

    @Query("SELECT * FROM bulk_job_items WHERE jobId = :jobId ORDER BY id ASC")
    fun observeItems(jobId: Long): Flow<List<BulkJobItem>>

    @Query("SELECT * FROM bulk_job_items WHERE jobId = :jobId ORDER BY id ASC")
    suspend fun getItems(jobId: Long): List<BulkJobItem>

    @Query("SELECT * FROM bulk_jobs WHERE id = :jobId")
    suspend fun getJob(jobId: Long): BulkJob?

    @Query(
        """
        SELECT bulk_job_items.* FROM bulk_job_items
        INNER JOIN bulk_jobs ON bulk_job_items.jobId = bulk_jobs.id
        WHERE bulk_jobs.userId = :userId
        """
    )
    fun observeAllItems(userId: Long): Flow<List<BulkJobItem>>

    @Query("SELECT * FROM bulk_jobs")
    suspend fun getAllJobs(): List<BulkJob>

    @Query("SELECT * FROM bulk_job_items")
    suspend fun getAllItemsRaw(): List<BulkJobItem>

    @Query("DELETE FROM bulk_jobs")
    suspend fun deleteAllJobs()

    @Query("DELETE FROM bulk_job_items")
    suspend fun deleteAllItems()
}
