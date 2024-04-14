package com.sqz.checklist.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo val description: String,
    @ColumnInfo val createDate: LocalDate,
    @ColumnInfo val detail: String? = null, // not implemented
    @ColumnInfo val reminder: String? = null,
    @ColumnInfo val isPin: Boolean = false,
    @ColumnInfo val pinToTop: Boolean = false, // not implemented
    @ColumnInfo val isHistory: Boolean = false,
    @ColumnInfo val isHistoryId: Int = 0,
)
