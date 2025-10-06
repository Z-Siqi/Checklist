package com.sqz.checklist.ui.main.task

import com.sqz.checklist.database.DatabaseRepository

/**
 * Override for preview work
 */
class TaskLayoutViewModelPreview : TaskLayoutViewModel() {
    override fun database(): DatabaseRepository {
        return DatabaseRepository(null)
    }
}

/**
 * Default task card height in (dp)
 */
const val CardHeight = 100
