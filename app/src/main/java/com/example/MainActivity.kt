package com.example

import android.content.ClipData
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.JobTrackerViewModel
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: JobTrackerViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val meetings by viewModel.meetings.collectAsStateWithLifecycle()
    val calendarNotes by viewModel.calendarNotes.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentCategoryFilter by viewModel.currentCategoryFilter.collectAsStateWithLifecycle()
    val currentMonthFilter by viewModel.currentMonthFilter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("tasks") }
    var showTaskDialog by remember { mutableStateOf<Task?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isCreateMode by remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Observe error message and show toasts
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            WorkspaceHeader(
                teamMembers = teamMembers,
                currentUser = currentUser,
                onUserSelected = { viewModel.setCurrentUser(it) },
                onSettingsClicked = { showSettingsDialog = true }
            )
        },
        bottomBar = {
            WorkspaceBottomNav(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        },
        floatingActionButton = {
            if (activeTab == "tasks") {
                FloatingActionButton(
                    onClick = {
                        isCreateMode = true
                        showTaskDialog = Task(
                            id = System.currentTimeMillis(),
                            name = "",
                            category = categories.firstOrNull() ?: "Rapat Internal",
                            status = "Belum Dimulai",
                            assignee = currentUser?.name ?: "",
                            deadline = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
                        )
                    },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Pekerjaan")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (activeTab) {
                "tasks" -> TasksTab(
                    tasks = tasks,
                    categories = categories,
                    currentUser = currentUser,
                    currentCategoryFilter = currentCategoryFilter,
                    currentMonthFilter = currentMonthFilter,
                    onCategorySelected = { viewModel.setCategoryFilter(it) },
                    onMonthSelected = { viewModel.setMonthFilter(it) },
                    onTaskEdit = { task ->
                        isCreateMode = false
                        showTaskDialog = task
                    },
                    onTaskStatusChange = { task, status ->
                        viewModel.updateTaskStatus(task, status)
                    },
                    onTaskDelete = { id ->
                        viewModel.deleteTask(id)
                    },
                    onSubtaskToggle = { task, index ->
                        viewModel.toggleSubtask(task, index)
                    }
                )
                "calendar" -> CalendarTab(
                    tasks = tasks,
                    meetings = meetings,
                    calendarNotes = calendarNotes,
                    onSaveNote = { date, note -> viewModel.saveCalendarNote(date, note) }
                )
                "meeting" -> MeetingTab(
                    meetings = meetings,
                    teamMembers = teamMembers,
                    onSaveMeeting = { date, meeting -> viewModel.saveMeeting(date, meeting) }
                )
                "dashboard" -> DashboardTab(
                    tasks = tasks
                )
                "profile" -> ProfileTab(
                    currentUser = currentUser,
                    tasks = tasks,
                    onProfileSave = { ttl, contact, role, hobby, avatar ->
                        viewModel.updateProfile(ttl, contact, role, hobby, avatar)
                    }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5))
                }
            }
        }
    }

    // Task Create / Edit Dialog
    showTaskDialog?.let { task ->
        TaskEditDialog(
            task = task,
            teamMembers = teamMembers,
            categories = categories,
            isCreateMode = isCreateMode,
            onDismiss = { showTaskDialog = null },
            onSave = { updatedTask ->
                viewModel.saveTask(updatedTask)
                showTaskDialog = null
            }
        )
    }

    // Settings Workspace Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            teamMembers = teamMembers,
            categories = categories,
            onDismiss = { showSettingsDialog = false },
            onApply = { updatedMembers, updatedCategories ->
                viewModel.applySettings(updatedMembers, updatedCategories)
                showSettingsDialog = false
            }
        )
    }
}

// ==========================================
// HEADER COMPONENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceHeader(
    teamMembers: List<TeamMember>,
    currentUser: TeamMember?,
    onUserSelected: (String) -> Unit,
    onSettingsClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4F46E5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Logo App",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "CashlessTeam-JobTracker",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Monitoring Kerja Online",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClicked,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Workspace Settings",
                    tint = Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Icon",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = currentUser?.nick ?: "Pilih User",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    teamMembers.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member.name) },
                            onClick = {
                                onUserSelected(member.name)
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A)
        ),
        modifier = Modifier.testTag("app_bar")
    )
}

// ==========================================
// BOTTOM NAVIGATION
// ==========================================
@Composable
fun WorkspaceBottomNav(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav")
    ) {
        val items = listOf(
            Triple("tasks", "Daftar Task", Icons.Default.List),
            Triple("calendar", "Kalender", Icons.Default.CalendarMonth),
            Triple("meeting", "Meeting", Icons.Default.Handshake),
            Triple("dashboard", "Analitik", Icons.Default.PieChart),
            Triple("profile", "Profil", Icons.Default.Person)
        )

        items.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(text = label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4F46E5),
                    selectedTextColor = Color(0xFF4F46E5),
                    indicatorColor = Color(0xFFEEF2F6),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}

// ==========================================
// DAFTAR TASK TAB (KANBAN)
// ==========================================
@Composable
fun TasksTab(
    tasks: List<Task>,
    categories: List<String>,
    currentUser: TeamMember?,
    currentCategoryFilter: String,
    currentMonthFilter: String,
    onCategorySelected: (String) -> Unit,
    onMonthSelected: (String) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskStatusChange: (Task, String) -> Unit,
    onTaskDelete: (Long) -> Unit,
    onSubtaskToggle: (Task, Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val filteredTasks = remember(tasks, currentCategoryFilter, currentMonthFilter) {
        tasks.filter { task ->
            val catMatch = currentCategoryFilter == "Semua" || task.category == currentCategoryFilter
            val monthMatch = if (currentMonthFilter == "All") true else {
                task.deadline.length >= 7 && task.deadline.substring(5, 7) == currentMonthFilter
            }
            catMatch && monthMatch
        }
    }

    val taskStatuses = listOf("Belum Dimulai", "On Progress", "Selesai")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasks_tab")
    ) {
        // Month & Category Filter Ribbon
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month Filter Dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DEADLINE BULAN",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF64748B)
                        )
                    }

                    var monthExpanded by remember { mutableStateOf(false) }
                    val months = listOf(
                        "All" to "Semua Bulan",
                        "01" to "Januari", "02" to "Februari", "03" to "Maret",
                        "04" to "April", "05" to "Mei", "06" to "Juni",
                        "07" to "Juli", "08" to "Agustus", "09" to "September",
                        "10" to "Oktober", "11" to "November", "12" to "Desember"
                    )
                    val currentMonthLabel = months.find { it.first == currentMonthFilter }?.second ?: "Semua Bulan"

                    Box {
                        OutlinedButton(
                            onClick = { monthExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155))
                        ) {
                            Text(text = currentMonthLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }

                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 12.sp) },
                                    onClick = {
                                        onMonthSelected(code)
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Horizontal Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        CategoryChip(
                            label = "Semua",
                            selected = currentCategoryFilter == "Semua",
                            onClick = { onCategorySelected("Semua") }
                        )
                    }
                    items(categories) { cat ->
                        CategoryChip(
                            label = cat,
                            selected = currentCategoryFilter == cat,
                            onClick = { onCategorySelected(cat) }
                        )
                    }
                }
            }
        }

        // Adaptive Kanban Layout
        if (isTablet) {
            // Three Columns side-by-side for tablet/landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                taskStatuses.forEach { status ->
                    val statusTasks = filteredTasks.filter { it.status == status }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (status) {
                                    "Belum Dimulai" -> Color(0xFFF1F5F9)
                                    "On Progress" -> Color(0xFFEEF2F6)
                                    else -> Color(0xFFECFDF5)
                                }
                            )
                            .padding(12.dp)
                    ) {
                        // Column Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val indicatorColor = when (status) {
                                    "Belum Dimulai" -> Color(0xFF94A3B8)
                                    "On Progress" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(indicatorColor)
                                )
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF334155)
                                )
                            }
                            Badge(
                                containerColor = Color.White,
                                contentColor = Color(0xFF475569),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = statusTasks.size.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(statusTasks) { task ->
                                TaskCard(
                                    task = task,
                                    isMine = currentUser?.name == task.assignee,
                                    onEdit = { onTaskEdit(task) },
                                    onStatusChange = { onTaskStatusChange(task, it) },
                                    onDelete = { onTaskDelete(task.id) },
                                    onSubtaskToggle = { onSubtaskToggle(task, it) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Tab system for mobile
            var selectedStatusTab by remember { mutableIntStateOf(0) }

            TabRow(
                selectedTabIndex = selectedStatusTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF4F46E5),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedStatusTab]),
                        color = Color(0xFF4F46E5)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                taskStatuses.forEachIndexed { index, status ->
                    val count = filteredTasks.count { it.status == status }
                    Tab(
                        selected = selectedStatusTab == index,
                        onClick = { selectedStatusTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Badge(
                                    containerColor = if (selectedStatusTab == index) Color(0xFF4F46E5) else Color(0xFFE2E8F0),
                                    contentColor = if (selectedStatusTab == index) Color.White else Color(0xFF475569)
                                ) {
                                    Text(text = count.toString(), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 2.dp))
                                }
                            }
                        }
                    )
                }
            }

            val activeStatus = taskStatuses[selectedStatusTab]
            val statusTasks = filteredTasks.filter { it.status == activeStatus }

            if (statusTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Assignment,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tidak Ada Pekerjaan",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Belum ada task di kategori atau bulan ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(statusTasks) { task ->
                        TaskCard(
                            task = task,
                            isMine = currentUser?.name == task.assignee,
                            onEdit = { onTaskEdit(task) },
                            onStatusChange = { onTaskStatusChange(task, it) },
                            onDelete = { onTaskDelete(task.id) },
                            onSubtaskToggle = { onSubtaskToggle(task, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) Color(0xFF4F46E5) else Color.White
    val contentColor = if (selected) Color.White else Color(0xFF64748B)
    val borderStroke = if (selected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))

    Surface(
        onClick = onClick,
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
        border = borderStroke,
        modifier = Modifier.testTag("category_chip_$label")
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    isMine: Boolean,
    onEdit: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit,
    onSubtaskToggle: (Int) -> Unit
) {
    val totalSubtasks = task.subtasks?.size ?: 0
    val doneSubtasks = task.subtasks?.count { it.done } ?: 0
    val percentage = if (totalSubtasks > 0) (doneSubtasks.toFloat() / totalSubtasks.toFloat()) else (if (task.status == "Selesai") 1f else 0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isMine) 1.5.dp else 1.dp,
            color = if (isMine) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card Top Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (task.category) {
                                "Rapat Internal" -> Color(0xFFFEE2E2)
                                "Event Akbar" -> Color(0xFFFEF3C7)
                                "Keuangan & Logistik" -> Color(0xFFD1FAE5)
                                "Publikasi & Humas" -> Color(0xFFDBEAFE)
                                else -> Color(0xFFF3E8FF)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (task.category) {
                            "Rapat Internal" -> Color(0xFFEF4444)
                            "Event Akbar" -> Color(0xFFD97706)
                            "Keuangan & Logistik" -> Color(0xFF059669)
                            "Publikasi & Humas" -> Color(0xFF2563EB)
                            else -> Color(0xFF7C3AED)
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isMine) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEEF2F6))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Milik Anda",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("task_edit_${task.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Task Name
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B)
            )

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { percentage },
                    color = Color(0xFF4F46E5),
                    trackColor = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
            }

            // Subtasks checklist list
            if (totalSubtasks > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    task.subtasks?.forEachIndexed { idx, sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubtaskToggle(idx) }
                        ) {
                            Checkbox(
                                checked = sub.done,
                                onCheckedChange = { onSubtaskToggle(idx) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = sub.text,
                                fontSize = 11.sp,
                                textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                                color = if (sub.done) Color(0xFF94A3B8) else Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Assignee & Deadline Info
            Divider(color = Color(0xFFF1F5F9))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                    Text(text = task.assignee, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.AccessTime, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                    val formattedDl = remember(task.deadline) {
                        try {
                            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(task.deadline)
                            parsed?.let { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(it) } ?: task.deadline
                        } catch (e: Exception) {
                            task.deadline
                        }
                    }
                    Text(text = "DL: $formattedDl", fontSize = 10.sp, color = Color(0xFF64748B))
                }
            }

            Divider(color = Color(0xFFF1F5F9))

            // Change Status & Delete Bottom section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                var statusExpanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(
                        onClick = { statusExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(text = task.status, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                    }

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        listOf("Belum Dimulai", "On Progress", "Selesai").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st, fontSize = 11.sp) },
                                onClick = {
                                    onStatusChange(st)
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("task_delete_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Hapus Pekerjaan",
                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// KALENDER TAB
// ==========================================
@Composable
fun CalendarTab(
    tasks: List<Task>,
    meetings: Map<String, Meeting>,
    calendarNotes: Map<String, String>,
    onSaveNote: (String, String) -> Unit
) {
    val currentCal = remember { Calendar.getInstance().apply { set(Calendar.YEAR, 2026); set(Calendar.MONTH, Calendar.JULY); set(Calendar.DAY_OF_MONTH, 1) } }
    var calendarMonthYear by remember { mutableStateOf("") }
    var calendarDays by remember { mutableStateOf(listOf<Date?>()) }
    var selectedDateStr by remember { mutableStateOf("") }
    var customNoteInput by remember { mutableStateOf("") }

    val formatMonthYear = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")) }
    val formatDayString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    fun updateCalendarData() {
        calendarMonthYear = formatMonthYear.format(currentCal.time)
        val daysList = mutableListOf<Date?>()

        val tempCal = currentCal.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed sunday

        for (i in 0 until firstDayOfWeek) {
            daysList.add(null)
        }

        val totalDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..totalDays) {
            tempCal.set(Calendar.DAY_OF_MONTH, i)
            daysList.add(tempCal.time)
        }
        calendarDays = daysList
    }

    LaunchedEffect(Unit) {
        updateCalendarData()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_tab")
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month controller header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kalender Kerja",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                currentCal.add(Calendar.MONTH, -1)
                                updateCalendarData()
                            }) {
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Sebelumnya")
                            }
                            Text(
                                text = calendarMonthYear,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF475569),
                                modifier = Modifier.widthIn(min = 100.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = {
                                currentCal.add(Calendar.MONTH, 1)
                                updateCalendarData()
                            }) {
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Berikutnya")
                            }
                        }
                    }

                    // Days label row
                    val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayNames.forEach { name ->
                            Text(
                                text = name,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Days grid
                    val chunkedDays = calendarDays.chunked(7)
                    chunkedDays.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { dateObj ->
                                if (dateObj == null) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val dateStr = formatDayString.format(dateObj)
                                    val dayTasks = tasks.filter { it.deadline.startsWith(dateStr) }
                                    val hasMeeting = meetings.containsKey(dateStr) || dateObj.run {
                                        val c = Calendar.getInstance()
                                        c.time = this
                                        val dow = c.get(Calendar.DAY_OF_WEEK)
                                        dow == Calendar.SUNDAY || dow == Calendar.TUESDAY || dow == Calendar.FRIDAY
                                    }
                                    val hasNote = calendarNotes.containsKey(dateStr)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.8f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selectedDateStr == dateStr) Color(0xFFEEF2F6) else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (selectedDateStr == dateStr) Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                                            )
                                            .clickable {
                                                selectedDateStr = dateStr
                                                customNoteInput = calendarNotes[dateStr] ?: ""
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            val c = Calendar.getInstance().apply { time = dateObj }
                                            Text(
                                                text = c.get(Calendar.DAY_OF_MONTH).toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedDateStr == dateStr) Color(0xFF4F46E5) else Color(0xFF475569)
                                            )

                                            // Small indicators
                                            if (dayTasks.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF4F46E5))
                                                )
                                            }
                                            if (hasMeeting) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444))
                                                )
                                            }
                                            if (hasNote) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFF59E0B))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Note input card
        if (selectedDateStr.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Catatan Tanggal: $selectedDateStr",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4F46E5)
                        )

                        // Shows deadline task details if any on that day
                        val dayTasks = tasks.filter { it.deadline.startsWith(selectedDateStr) }
                        if (dayTasks.isNotEmpty()) {
                            Text(
                                text = "Pekerjaan Deadline Hari Ini:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF334155)
                            )
                            dayTasks.forEach { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4F46E5)))
                                    Text(
                                        text = "${task.name} (${task.assignee})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customNoteInput,
                            onValueChange = { customNoteInput = it },
                            label = { Text("Kesibukan Tim / Keterangan Kustom") },
                            placeholder = { Text("Contoh: Budi ada responsi praktikum kuliah jam 13:00...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            textStyle = TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = { onSaveNote(selectedDateStr, customNoteInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Simpan Catatan Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MEETING TAB
// ==========================================
@Composable
fun MeetingTab(
    meetings: Map<String, Meeting>,
    teamMembers: List<TeamMember>,
    onSaveMeeting: (String, Meeting) -> Unit
) {
    val currentCal = remember { Calendar.getInstance().apply { set(Calendar.YEAR, 2026); set(Calendar.MONTH, Calendar.JULY); set(Calendar.DAY_OF_MONTH, 1) } }
    var calendarMonthYear by remember { mutableStateOf("") }
    var calendarDays by remember { mutableStateOf(listOf<Date?>()) }
    var selectedDateStr by remember { mutableStateOf("") }

    val formatMonthYear = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")) }
    val formatDayString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var meetingEpisode by remember { mutableStateOf("1") }
    var meetingTime by remember { mutableStateOf("19:00") }
    var meetingPlace by remember { mutableStateOf("Sekretariat") }
    var meetingPresenter by remember { mutableStateOf("") }
    var meetingTopic by remember { mutableStateOf("") }
    var meetingNotify by remember { mutableStateOf(false) }

    var showNotepadModal by remember { mutableStateOf(false) }
    var notepadText by remember { mutableStateOf("") }

    val context = LocalContext.current

    fun updateCalendarData() {
        calendarMonthYear = formatMonthYear.format(currentCal.time)
        val daysList = mutableListOf<Date?>()

        val tempCal = currentCal.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1

        for (i in 0 until firstDayOfWeek) {
            daysList.add(null)
        }

        val totalDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..totalDays) {
            tempCal.set(Calendar.DAY_OF_MONTH, i)
            daysList.add(tempCal.time)
        }
        calendarDays = daysList
    }

    LaunchedEffect(Unit) {
        updateCalendarData()
    }

    // Prefill fields when a date is selected
    LaunchedEffect(selectedDateStr) {
        if (selectedDateStr.isNotEmpty()) {
            val mtg = meetings[selectedDateStr] ?: Meeting(
                episode = "1",
                time = "19:00",
                place = "Sekretariat",
                presenter = teamMembers.firstOrNull()?.name ?: "",
                topic = "",
                notes = "",
                notify = false
            )
            meetingEpisode = mtg.episode ?: "1"
            meetingTime = mtg.time ?: "19:00"
            meetingPlace = mtg.place ?: "Sekretariat"
            meetingPresenter = mtg.presenter ?: (teamMembers.firstOrNull()?.name ?: "")
            meetingTopic = mtg.topic ?: ""
            meetingNotify = mtg.notify ?: false
            notepadText = mtg.notes ?: ""
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("meeting_tab")
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Hari & Jadwal Meeting",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Wajib Kumpul: Selasa, Jumat & Ahad",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = {
                                currentCal.add(Calendar.MONTH, -1)
                                updateCalendarData()
                            }) {
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Sebelumnya")
                            }
                            Text(
                                text = calendarMonthYear,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF475569),
                                modifier = Modifier.widthIn(min = 90.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = {
                                currentCal.add(Calendar.MONTH, 1)
                                updateCalendarData()
                            }) {
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Berikutnya")
                            }
                        }
                    }

                    // Calendar days label row
                    val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayNames.forEach { name ->
                            Text(
                                text = name,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Meeting calendar grid
                    val chunkedDays = calendarDays.chunked(7)
                    chunkedDays.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { dateObj ->
                                if (dateObj == null) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val dateStr = formatDayString.format(dateObj)
                                    val c = Calendar.getInstance().apply { time = dateObj }
                                    val dow = c.get(Calendar.DAY_OF_WEEK)
                                    val isWajibKumpul = dow == Calendar.SUNDAY || dow == Calendar.TUESDAY || dow == Calendar.FRIDAY

                                    val isSelected = selectedDateStr == dateStr
                                    val hasCloudMeeting = meetings.containsKey(dateStr)

                                    val cellBgColor = when {
                                        isSelected -> Color(0xFFEEF2F6)
                                        isWajibKumpul -> Color(0xFFFEF2F2)
                                        else -> Color.Transparent
                                    }

                                    val cellBorderColor = when {
                                        isSelected -> Color(0xFF4F46E5)
                                        isWajibKumpul -> Color(0xFFFCA5A5)
                                        else -> Color(0xFFF1F5F9)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellBgColor)
                                            .border(1.dp, cellBorderColor)
                                            .clickable { selectedDateStr = dateStr }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = c.get(Calendar.DAY_OF_MONTH).toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isWajibKumpul) Color(0xFFB91C1C) else Color(0xFF475569)
                                            )
                                            if (hasCloudMeeting || isWajibKumpul) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isWajibKumpul) Color(0xFFEF4444) else Color(0xFF4F46E5))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected meeting scheduling details
        if (selectedDateStr.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Agenda Kumpul: $selectedDateStr",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5)
                            )

                            Button(
                                onClick = { showNotepadModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Notulensi", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = meetingEpisode,
                                onValueChange = { meetingEpisode = it },
                                label = { Text("Episode") },
                                textStyle = TextStyle(fontSize = 11.sp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = meetingTime,
                                onValueChange = { meetingTime = it },
                                label = { Text("Waktu") },
                                textStyle = TextStyle(fontSize = 11.sp),
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = meetingPlace,
                            onValueChange = { meetingPlace = it },
                            label = { Text("Tempat Pertemuan") },
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Presenter selector dropdown
                        var presenterExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = meetingPresenter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pemateri / Presenter") },
                                textStyle = TextStyle(fontSize = 11.sp),
                                trailingIcon = {
                                    IconButton(onClick = { presenterExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { presenterExpanded = true },
                                shape = RoundedCornerShape(8.dp)
                            )

                            DropdownMenu(
                                expanded = presenterExpanded,
                                onDismissRequest = { presenterExpanded = false }
                            ) {
                                teamMembers.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name, fontSize = 12.sp) },
                                        onClick = {
                                            meetingPresenter = member.name
                                            presenterExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = meetingTopic,
                            onValueChange = { meetingTopic = it },
                            label = { Text("Susunan Acara Perkumpulan") },
                            placeholder = { Text("1. Pembukaan\n2. Penyampaian laporan divisi...") },
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = meetingNotify,
                                onCheckedChange = { meetingNotify = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5))
                            )
                            Text(
                                text = "Aktifkan Pengingat H-1 Tim",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }

                        Button(
                            onClick = {
                                val m = Meeting(
                                    episode = meetingEpisode,
                                    time = meetingTime,
                                    place = meetingPlace,
                                    presenter = meetingPresenter,
                                    topic = meetingTopic,
                                    notes = notepadText,
                                    notify = meetingNotify
                                )
                                onSaveMeeting(selectedDateStr, m)
                                Toast.makeText(context, "Agenda kumpul berhasil diunggah ke database cloud!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Simpan Data ke Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val textWA = "✨KUMPUL 2 HARIAN TIM CASHLESS & UNIT USAHA✨\n\n" +
                                            "Episode $meetingEpisode\n" +
                                            "📍 Place : $meetingPlace\n" +
                                            "🕰️ Time  : $meetingTime\n\n" +
                                            "*Susunan Acara Perkumpulan:*\n" +
                                            meetingTopic

                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Jadwal Kumpul", textWA)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Teks susunan acara disalin! Siapkan paste di WA.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin Teks WA", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "✨KUMPUL 2 HARIAN TIM CASHLESS & UNIT USAHA✨\nEpisode: $meetingEpisode\nWaktu: $meetingTime\nTempat: $meetingPlace")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cetak Gambar", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Yellow Notepad Modal
    if (showNotepadModal) {
        Dialog(onDismissRequest = { showNotepadModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // Lined yellow notepad background
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF08A))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Note, contentDescription = null, tint = Color(0xFF78350F))
                            Text(
                                text = "NOTULENSI DIGITAL",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF78350F)
                            )
                        }
                        IconButton(onClick = { showNotepadModal = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF78350F))
                        }
                    }

                    // Content Editor with notebook paper feel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        TextField(
                            value = notepadText,
                            onValueChange = {
                                notepadText = it
                                // Save automatically to cache, real upload on Cloud Save button
                                val currentMtg = meetings[selectedDateStr] ?: Meeting(episode = meetingEpisode, time = meetingTime, place = meetingPlace, presenter = meetingPresenter, topic = meetingTopic)
                                onSaveMeeting(selectedDateStr, currentMtg.copy(notes = it))
                            },
                            placeholder = {
                                Text(
                                    text = "Tulis ringkasan inti rapat atau draf keputusan di lembar notulensi digital ini...",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309).copy(alpha = 0.4f)
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF78350F),
                                lineHeight = 24.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFFB45309)
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Notepad Footer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7))
                            .border(BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.8f)))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Tanggal: $selectedDateStr",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { showNotepadModal = false }) {
                                Text("Tutup", fontSize = 11.sp, color = Color(0xFF78350F), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    // Native PDF Export
                                    exportNotepadToPDF(context, selectedDateStr, meetingPlace, notepadText)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Function to print notepad content to PDF Document natively on Android device storage
fun exportNotepadToPDF(context: Context, date: String, place: String, notes: String) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Format
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()

    paint.textSize = 18f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.rgb(30, 27, 75)
    canvas.drawText("NOTULENSI INTI HASIL KUMPUL TIM", 50f, 50f, paint)

    paint.textSize = 11f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.rgb(71, 85, 105)
    canvas.drawText("Tanggal Rapat: $date | Tempat: $place", 50f, 80f, paint)

    paint.color = android.graphics.Color.rgb(226, 232, 240)
    canvas.drawLine(50f, 100f, 545f, 100f, paint)

    paint.textSize = 13f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.rgb(15, 23, 42)
    canvas.drawText("POIN-POIN INTI & KEPUTUSAN HASIL RAPAT:", 50f, 130f, paint)

    paint.textSize = 10f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.rgb(120, 53, 15) // amber tone
    val lines = notes.split("\n")
    var y = 160f
    for (line in lines) {
        if (y > 800f) {
            break
        }
        canvas.drawText(line, 50f, y, paint)
        y += 20f
    }

    pdfDocument.finishPage(page)
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notulensi_Kumpul_$date.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
        Toast.makeText(context, "PDF disimpan di: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

// ==========================================
// PERSENTASE & CHART (DASHBOARD) TAB
// ==========================================
@Composable
fun DashboardTab(tasks: List<Task>) {
    val total = tasks.size
    val belum = tasks.count { it.status == "Belum Dimulai" }
    val progress = tasks.count { it.status == "On Progress" }
    val selesai = tasks.count { it.status == "Selesai" }
    val percentage = if (total > 0) ((selesai.toFloat() / total.toFloat()) * 100).toInt() else 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_tab")
    ) {
        item {
            // Hero Gradient Banner card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Total Progress Tim",
                            fontSize = 12.sp,
                            color = Color(0xFFE0E7FF),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$percentage%",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { percentage.toFloat() / 100f },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        item {
            // Chart Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Statistik Distribusi Pekerjaan",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    )

                    CustomDoughnutChart(
                        values = listOf(belum.toFloat(), progress.toFloat(), selesai.toFloat()),
                        colors = listOf(Color(0xFF94A3B8), Color(0xFFF59E0B), Color(0xFF10B981)),
                        labels = listOf("Belum Dimulai", "On Progress", "Selesai"),
                        centerText = "$total Task",
                        modifier = Modifier.size(200.dp)
                    )

                    // Legend indicators row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = Color(0xFF94A3B8), label = "Belum")
                        LegendItem(color = Color(0xFFF59E0B), label = "Progress")
                        LegendItem(color = Color(0xFF10B981), label = "Selesai")
                    }
                }
            }
        }

        item {
            // Detailed statistics count block
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatGridBlock(title = "Belum Mulai", count = belum, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                StatGridBlock(title = "On Progress", count = progress, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                StatGridBlock(title = "Selesai", count = selesai, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CustomDoughnutChart(
    values: List<Float>,
    colors: List<Color>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    centerText: String? = null
) {
    val total = values.sum()
    if (total == 0f) {
        Box(contentAlignment = Alignment.Center, modifier = modifier) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.LightGray.copy(alpha = 0.2f), style = Stroke(width = 30f))
            }
            Text("Tidak ada data", fontSize = 12.sp, color = Color.Gray)
        }
        return
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            values.forEachIndexed { idx, value ->
                val sweepAngle = (value / total) * 360f
                if (sweepAngle > 0f) {
                    drawArc(
                        color = colors.getOrElse(idx) { Color.Gray },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30f, cap = StrokeCap.Round)
                    )
                }
                startAngle += sweepAngle
            }
        }
        if (centerText != null) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF334155)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
    }
}

@Composable
fun StatGridBlock(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            Text(text = count.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

// ==========================================
// PROFILE TAB
// ==========================================
@Composable
fun ProfileTab(
    currentUser: TeamMember?,
    tasks: List<Task>,
    onProfileSave: (String, String, String, String, String?) -> Unit
) {
    val member = currentUser ?: return

    var ttl by remember { mutableStateOf(member.ttl) }
    var contact by remember { mutableStateOf(member.contact) }
    var role by remember { mutableStateOf(member.role) }
    var hobby by remember { mutableStateOf(member.hobby) }
    var avatarBase64 by remember { mutableStateOf<String?>(member.avatar) }

    val context = LocalContext.current

    // Observe changes if user selected changes
    LaunchedEffect(member) {
        ttl = member.ttl
        contact = member.contact
        role = member.role
        hobby = member.hobby
        avatarBase64 = member.avatar
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    avatarBase64 = encoded
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Compute stats for current user
    val userTasks = tasks.filter { it.assignee == member.name }
    val total = userTasks.size
    val belum = userTasks.count { it.status == "Belum Dimulai" }
    val progress = userTasks.count { it.status == "On Progress" }
    val selesai = userTasks.count { it.status == "Selesai" }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_tab")
    ) {
        // Upper Card: Avatar & Personal Biodata Editing
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile picture with edit hover effect simulation
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Base64ProfileImage(avatarBase64Str = avatarBase64, modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Edit Foto", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = member.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(text = "Panggilan: ${member.nick}", fontSize = 12.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color(0xFFF1F5F9))

                    // Bio forms
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ttl,
                            onValueChange = { ttl = it },
                            label = { Text("Tempat, Tanggal Lahir") },
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = { Text("Kontak / No. HP") },
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Tanggung Jawab / Bagian") },
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = hobby,
                            onValueChange = { hobby = it },
                            label = { Text("Hobi / Minat Pribadi") },
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onProfileSave(ttl, contact, role, hobby, avatarBase64)
                            Toast.makeText(context, "Biodata profil cloud berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Simpan Seluruh Biodata", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Stats card: circular progress indicators
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Statistik Tugas Individu",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserStatDonut(title = "Belum", value = belum, total = total, color = Color(0xFF94A3B8))
                        UserStatDonut(title = "On Progress", value = progress, total = total, color = Color(0xFFF59E0B))
                        UserStatDonut(title = "Selesai", value = selesai, total = total, color = Color(0xFF10B981))
                    }
                }
            }
        }

        // Heatmap Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Annual Heatmap Kontribusi (Per Bulan)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Aktivitas pengerjaan di tahun 2026",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Render simulated heatmap months
                    val monthsList = listOf(
                        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                    )

                    monthsList.forEachIndexed { mIdx, name ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(8.dp)
                        ) {
                            Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (d in 1..28) {
                                    val level = remember(mIdx, d) {
                                        if ((d + mIdx) % 5 == 0) 2 else if ((d * mIdx) % 7 == 0) 3 else 0
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(
                                                when (level) {
                                                    1 -> Color(0xFFC7D2FE)
                                                    2 -> Color(0xFF818CF8)
                                                    3 -> Color(0xFF4F46E5)
                                                    else -> Color(0xFFE2E8F0)
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatDonut(title: String, value: Int, total: Int, color: Color) {
    val percentage = if (total > 0) (value.toFloat() / total.toFloat()) else 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFE2E8F0), style = Stroke(width = 8f))
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = percentage * 360f,
                    useCenter = false,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }
            Text(text = value.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }
        Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
    }
}

@Composable
fun Base64ProfileImage(avatarBase64Str: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(avatarBase64Str) {
        if (avatarBase64Str.isNullOrEmpty()) null else {
            try {
                val decoded = android.util.Base64.decode(avatarBase64Str, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFFEEF2F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.fillMaxSize(0.6f))
        }
    }
}

// ==========================================
// TASK DIALOG COMPONENT
// ==========================================
@Composable
fun TaskEditDialog(
    task: Task,
    teamMembers: List<TeamMember>,
    categories: List<String>,
    isCreateMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var selectedCategory by remember { mutableStateOf(task.category) }
    var selectedAssignee by remember { mutableStateOf(task.assignee) }
    var deadline by remember { mutableStateOf(task.deadline) }

    var subtasksList = remember { mutableStateListOf<Subtask>().apply { addAll(task.subtasks ?: emptyList()) } }
    var subtaskTextInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = if (isCreateMode) "Buat Task Baru" else "Edit Task",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Task *") },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Category dropdown
                item {
                    var catExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Program Kerja *") },
                            textStyle = TextStyle(fontSize = 12.sp),
                            trailingIcon = {
                                IconButton(onClick = { catExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { catExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        )

                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontSize = 12.sp) },
                                    onClick = {
                                        selectedCategory = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Assignee dropdown
                item {
                    var assigneeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedAssignee,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Penanggung Jawab *") },
                            textStyle = TextStyle(fontSize = 12.sp),
                            trailingIcon = {
                                IconButton(onClick = { assigneeExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { assigneeExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        )

                        DropdownMenu(
                            expanded = assigneeExpanded,
                            onDismissRequest = { assigneeExpanded = false }
                        ) {
                            teamMembers.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedAssignee = member.name
                                        assigneeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Simple Deadline date string input fields
                item {
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline (YYYY-MM-DDTHH:MM) *") },
                        placeholder = { Text("Contoh: 2026-07-15T19:00") },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Subtask form lists
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Subtask", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = subtaskTextInput,
                                onValueChange = { subtaskTextInput = it },
                                placeholder = { Text("Tambah langkah kerja...", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 11.sp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (subtaskTextInput.trim().isNotEmpty()) {
                                        subtasksList.add(Subtask(text = subtaskTextInput.trim(), done = false))
                                        subtaskTextInput = ""
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Render subtasks
                        if (subtasksList.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                subtasksList.forEachIndexed { sIdx, sub ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "• ${sub.text}", fontSize = 11.sp, color = Color(0xFF475569))
                                        IconButton(
                                            onClick = { subtasksList.removeAt(sIdx) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom actions buttons
                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.trim().isEmpty() || deadline.trim().isEmpty()) {
                                    Toast.makeText(context, "Harap lengkapi semua isian bertanda *", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val updated = task.copy(
                                    name = name.trim(),
                                    category = selectedCategory,
                                    assignee = selectedAssignee,
                                    deadline = deadline.trim(),
                                    subtasks = subtasksList.toList()
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Task", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SETTINGS DIALOG COMPONENT
// ==========================================
@Composable
fun SettingsDialog(
    teamMembers: List<TeamMember>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<TeamMember>, List<String>) -> Unit
) {
    var tempMembers = remember { mutableStateListOf<TeamMember>().apply { addAll(teamMembers) } }
    var tempCategories = remember { mutableStateListOf<String>().apply { addAll(categories) } }

    var newMemberNameInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Pengaturan Workspace",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                }

                // Team Members section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Manajemen Anggota Tim", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(6.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tempMembers.forEachIndexed { index, member ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = member.name, fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { tempMembers.removeAt(index) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newMemberNameInput,
                                onValueChange = { newMemberNameInput = it },
                                placeholder = { Text("Nama Lengkap...", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 11.sp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (newMemberNameInput.trim().isNotEmpty()) {
                                        val nm = newMemberNameInput.trim()
                                        tempMembers.add(
                                            TeamMember(
                                                name = nm,
                                                nick = nm.split(" ").firstOrNull() ?: nm,
                                                ttl = "",
                                                contact = "",
                                                role = "Anggota",
                                                hobby = ""
                                            )
                                        )
                                        newMemberNameInput = ""
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Tambah", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Categories management
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Manajemen Kategori Proker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(6.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tempCategories.forEachIndexed { index, cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = cat, fontSize = 11.sp, color = Color(0xFF334155))
                                    IconButton(
                                        onClick = { tempCategories.removeAt(index) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCategoryInput,
                                onValueChange = { newCategoryInput = it },
                                placeholder = { Text("Nama Kategori...", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 11.sp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (newCategoryInput.trim().isNotEmpty()) {
                                        tempCategories.add(newCategoryInput.trim())
                                        newCategoryInput = ""
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Tambah", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Actions buttons
                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempMembers.isEmpty() || tempCategories.isEmpty()) {
                                    Toast.makeText(context, "Kategori dan anggota tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onApply(tempMembers.toList(), tempCategories.toList())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
