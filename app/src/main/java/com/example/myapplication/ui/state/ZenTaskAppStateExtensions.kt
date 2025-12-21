package com.example.myapplication.ui.state

import android.database.SQLException
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.example.myapplication.data.local.db.entity.Task as DbTask
import com.example.myapplication.data.local.db.entity.TaskGroup as DbTaskGroup
import com.example.myapplication.ui.screens.home.kanban.KanbanColumn
import com.example.myapplication.utils.showToast
import com.example.myapplication.model.toEpochMillis
import com.example.myapplication.model.toDbPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ZenTaskAppExt"

fun ZenTaskAppState.addTask(request: AddTaskRequest) {
    coroutineScope.launch {
        try {
            val targetCollectionId = request.collectionId ?: selectedCollection?.id
            val (validatedGroupId, wasInvalidGroup) = withContext(Dispatchers.IO) {
                if (targetCollectionId == null) {
                    null to false
                } else {
                    val exists = taskGroupDao.getById(targetCollectionId) != null
                    if (exists) targetCollectionId to false else null to true
                }
            }

            if (wasInvalidGroup) {
                Log.w(TAG, "Add task: groupId=$targetCollectionId not found; saving as uncategorized")
                context.showToast("Danh mục không còn tồn tại, sẽ lưu vào Không phân loại")
            }

            withContext(Dispatchers.IO) {
                taskDao.upsert(
                    DbTask(
                        title = request.title,
                        description = request.description,
                        progressNotes = null,
                        dueDate = request.dueDate?.toEpochMillis(),
                        urlLink = null,
                        imagePath = request.imageUri,
                        status = 0,
                        priority = request.priority.toDbPriority(),
                        groupId = validatedGroupId
                    )
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SQLException) {
            Log.e(TAG, "Add task failed", e)
            context.showToast("Lỗi khi thêm công việc")
        }
    }
}

fun ZenTaskAppState.addCollection(name: String, color: androidx.compose.ui.graphics.Color) {
    coroutineScope.launch {
        try {
            withContext(Dispatchers.IO) {
                taskGroupDao.upsert(
                    DbTaskGroup(
                        groupName = name,
                        groupColor = color.toArgb(),
                        description = null
                    )
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SQLException) {
            Log.e(TAG, "Add collection failed", e)
            context.showToast("Lỗi khi tạo danh mục")
        }
    }
}

fun ZenTaskAppState.toggleTask(id: Int) {
    coroutineScope.launch {
        try {
            val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
            if (current == null) {
                Log.w(TAG, "Toggle task failed: taskId=$id not found")
                context.showToast("Không tìm thấy công việc")
            } else {
                val newStatus = if (current.status == 1) {
                    lastNonCompletedStatus.remove(id) ?: 0
                } else {
                    lastNonCompletedStatus[id] = current.status
                    1
                }

                withContext(Dispatchers.IO) {
                    taskDao.update(current.copy(status = newStatus))
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SQLException) {
            Log.e(TAG, "Toggle task failed: taskId=$id", e)
            context.showToast("Lỗi khi cập nhật công việc")
        }
    }
}

fun ZenTaskAppState.deleteTask(id: Int) {
    coroutineScope.launch {
        try {
            lastNonCompletedStatus.remove(id)
            withContext(Dispatchers.IO) { taskDao.deleteById(id) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SQLException) {
            Log.e(TAG, "Delete task failed: taskId=$id", e)
            context.showToast("Lỗi khi xoá công việc")
        }
    }
}

fun ZenTaskAppState.changeTaskStatus(id: Int, column: KanbanColumn) {
    coroutineScope.launch {
        try {
            processStatusChange(id, column)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SQLException) {
            Log.e(TAG, "Change status failed: taskId=$id", e)
            context.showToast("Lỗi khi cập nhật trạng thái")
        }
    }
}

private suspend fun ZenTaskAppState.processStatusChange(id: Int, column: KanbanColumn) {
    val current = withContext(Dispatchers.IO) { taskDao.getById(id) } ?: return
    
    val newStatus = when (column) {
        KanbanColumn.COMPLETED -> 1
        KanbanColumn.IN_PROGRESS -> 2
        KanbanColumn.UNCOMPLETED -> 0
    }
    
    withContext(Dispatchers.IO) {
        taskDao.update(current.copy(status = newStatus))
    }
    
    updateNonCompletedStatus(id, current.status, newStatus)
}

private fun ZenTaskAppState.updateNonCompletedStatus(id: Int, oldStatus: Int, newStatus: Int) {
    if (newStatus == 1 && oldStatus != 1) {
        lastNonCompletedStatus[id] = oldStatus
    } else if (newStatus != 1) {
        lastNonCompletedStatus[id] = newStatus
    }
}
