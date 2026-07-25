package app.submissionultra.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    /** 未完了のみ。並び順は期限昇順（最後の開始時刻順の並べ替えは UI 側で余裕時間と併せて行う）。 */
    @Query("SELECT * FROM assignments WHERE isCompleted = 0 ORDER BY deadlineEpochMillis ASC")
    fun observeActive(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments ORDER BY deadlineEpochMillis ASC")
    fun observeAll(): Flow<List<Assignment>>

    /** 完了済みのみ。完了日時の新しい順（未記録は末尾）。 */
    @Query("SELECT * FROM assignments WHERE isCompleted = 1 ORDER BY completedAtEpochMillis DESC, deadlineEpochMillis DESC")
    fun observeCompleted(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE isCompleted = 0")
    suspend fun getActive(): List<Assignment>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getById(id: Long): Assignment?

    @Insert
    suspend fun insert(assignment: Assignment): Long

    @Update
    suspend fun update(assignment: Assignment)

    @Delete
    suspend fun delete(assignment: Assignment)
}
