package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobTrackerViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _meetings = MutableStateFlow<Map<String, Meeting>>(emptyMap())
    val meetings: StateFlow<Map<String, Meeting>> = _meetings.asStateFlow()

    private val _calendarNotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val calendarNotes: StateFlow<Map<String, String>> = _calendarNotes.asStateFlow()

    private val _currentUser = MutableStateFlow<TeamMember?>(null)
    val currentUser: StateFlow<TeamMember?> = _currentUser.asStateFlow()

    private val _currentCategoryFilter = MutableStateFlow("Semua")
    val currentCategoryFilter: StateFlow<String> = _currentCategoryFilter.asStateFlow()

    private val _currentMonthFilter = MutableStateFlow("All")
    val currentMonthFilter: StateFlow<String> = _currentMonthFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null

    val defaultMembers = listOf(
        TeamMember("Budi Sudrajat", "Budi", "Jakarta, 12 Agustus 2002", "081299881122", "Ketua Organisasi", "Fotografi & Desain"),
        TeamMember("Siti Aminah", "Siti", "Bandung, 24 April 2003", "085711223344", "Sekretaris Utama", "Menulis & Membaca"),
        TeamMember("Ahmad Dhani", "Dhani", "Surabaya, 05 November 2001", "081344556677", "Div. Publikasi & Humas", "Musik & Videografi")
    )

    val defaultCategories = listOf("Rapat Internal", "Event Akbar", "Keuangan & Logistik", "Publikasi & Humas")

    init {
        startSyncing()
    }

    fun startSyncing() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _isLoading.value = true
            var isFirstLoad = true
            while (true) {
                try {
                    fetchData()
                    _errorMessage.value = null
                } catch (e: Exception) {
                    _errorMessage.value = "Koneksi Bermasalah: ${e.localizedMessage}"
                } finally {
                    if (isFirstLoad) {
                        _isLoading.value = false
                        isFirstLoad = false
                    }
                }
                // Poll every 8 seconds for fast real-time response
                delay(8000)
            }
        }
    }

    private suspend fun fetchData() {
        // Fetch Tasks
        val fetchedTasksMap = FirebaseService.api.getTasks()
        _tasks.value = fetchedTasksMap?.values?.toList() ?: emptyList()

        // Fetch Team Members
        var fetchedMembers = FirebaseService.api.getTeamMembers()
        if (fetchedMembers.isNullOrEmpty()) {
            FirebaseService.api.putTeamMembers(defaultMembers)
            fetchedMembers = defaultMembers
        }
        _teamMembers.value = fetchedMembers
        if (_currentUser.value == null && fetchedMembers.isNotEmpty()) {
            _currentUser.value = fetchedMembers.first()
        } else if (_currentUser.value != null) {
            // Refresh current user data if changed on cloud
            val updatedUser = fetchedMembers.find { it.name == _currentUser.value?.name }
            if (updatedUser != null) {
                _currentUser.value = updatedUser
            }
        }

        // Fetch Categories
        var fetchedCategories = FirebaseService.api.getCategories()
        if (fetchedCategories.isNullOrEmpty()) {
            FirebaseService.api.putCategories(defaultCategories)
            fetchedCategories = defaultCategories
        }
        _categories.value = fetchedCategories

        // Fetch Meetings
        val fetchedMeetings = FirebaseService.api.getMeetings()
        _meetings.value = fetchedMeetings ?: emptyMap()

        // Fetch Calendar Notes
        val fetchedNotes = FirebaseService.api.getCalendarNotes()
        _calendarNotes.value = fetchedNotes ?: emptyMap()
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                fetchData()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat ulang data dari Cloud: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCurrentUser(name: String) {
        val member = _teamMembers.value.find { it.name == name }
        if (member != null) {
            _currentUser.value = member
        }
    }

    fun setCategoryFilter(category: String) {
        _currentCategoryFilter.value = category
    }

    fun setMonthFilter(month: String) {
        _currentMonthFilter.value = month
    }

    // Task Actions
    fun saveTask(task: Task) {
        viewModelScope.launch {
            try {
                FirebaseService.api.putTask(task.id.toString(), task)
                fetchData()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menyimpan task: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            try {
                FirebaseService.api.deleteTask(id.toString())
                fetchData()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus task: ${e.localizedMessage}"
            }
        }
    }

    fun updateTaskStatus(task: Task, newStatus: String) {
        val updated = task.copy(status = newStatus)
        saveTask(updated)
    }

    fun toggleSubtask(task: Task, subtaskIndex: Int) {
        val subs = task.subtasks?.toMutableList() ?: mutableListOf()
        if (subtaskIndex in subs.indices) {
            val sub = subs[subtaskIndex]
            subs[subtaskIndex] = sub.copy(done = !sub.done)
        }
        val isAllCompleted = subs.isNotEmpty() && subs.all { it.done }
        val newStatus = if (isAllCompleted) "Selesai" else if (task.status == "Selesai") "On Progress" else task.status
        saveTask(task.copy(subtasks = subs, status = newStatus))
    }

    // Profile Actions
    fun updateProfile(ttl: String, contact: String, role: String, hobby: String, avatarBase64: String?) {
        val current = _currentUser.value ?: return
        val updated = current.copy(ttl = ttl, contact = contact, role = role, hobby = hobby, avatar = avatarBase64 ?: current.avatar)
        val list = _teamMembers.value.map { if (it.name == current.name) updated else it }
        viewModelScope.launch {
            try {
                FirebaseService.api.putTeamMembers(list)
                _currentUser.value = updated
                _teamMembers.value = list
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memperbarui profil: ${e.localizedMessage}"
            }
        }
    }

    // Meeting Actions
    fun saveMeeting(date: String, meeting: Meeting) {
        viewModelScope.launch {
            try {
                FirebaseService.api.putMeeting(date, meeting)
                fetchData()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menyimpan jadwal meeting: ${e.localizedMessage}"
            }
        }
    }

    // Custom Calendar Note Actions
    fun saveCalendarNote(date: String, note: String) {
        val updatedNotes = _calendarNotes.value.toMutableMap()
        if (note.trim().isEmpty()) {
            updatedNotes.remove(date)
        } else {
            updatedNotes[date] = note
        }
        viewModelScope.launch {
            try {
                FirebaseService.api.putCalendarNotes(updatedNotes)
                _calendarNotes.value = updatedNotes
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menyimpan catatan kalender kustom: ${e.localizedMessage}"
            }
        }
    }

    // Workspace Settings Actions
    fun applySettings(newMembers: List<TeamMember>, newCategories: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                FirebaseService.api.putTeamMembers(newMembers)
                FirebaseService.api.putCategories(newCategories)
                fetchData()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menyimpan pengaturan workspace: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
