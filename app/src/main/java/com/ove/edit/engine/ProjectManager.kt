package com.ove.edit.engine

import android.content.Context
import com.google.gson.Gson
import com.ove.edit.models.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectManager(private val context: Context) {
    private val gson = Gson()
    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    suspend fun saveProject(project: Project) {
        withContext(Dispatchers.IO) {
            project.lastSavedAt = System.currentTimeMillis()
            val file = File(projectsDir, "${project.id}.proj")
            val json = gson.toJson(project)
            file.writeText(json)
        }
    }

    suspend fun loadProject(id: String): Project? {
        return withContext(Dispatchers.IO) {
            val file = File(projectsDir, "$id.proj")
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, Project::class.java)
            } else {
                null
            }
        }
    }

    suspend fun getAllProjects(): List<Project> {
        return withContext(Dispatchers.IO) {
            projectsDir.listFiles { _, name -> name.endsWith(".proj") }
                ?.mapNotNull { file ->
                    try {
                        gson.fromJson(file.readText(), Project::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.lastSavedAt } ?: emptyList()
        }
    }

    suspend fun deleteProject(id: String) {
        withContext(Dispatchers.IO) {
            val file = File(projectsDir, "$id.proj")
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
