package com.sqz.checklist.ui.main.task

import com.sqz.checklist.database.DatabaseRepository

class TaskLayoutViewModelPreview : TaskLayoutViewModel() {
    override fun database(): DatabaseRepository {
        return DatabaseRepository(null)
    }
}

const val CardHeight = 120
