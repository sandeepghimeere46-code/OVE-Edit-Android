package com.ove.edit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ove.edit.engine.ProjectManager
import com.ove.edit.models.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val projectManager = ProjectManager(application)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projects.value = projectManager.getAllProjects()
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            projectManager.deleteProject(id)
            loadProjects() // Refresh
        }
    }
}
