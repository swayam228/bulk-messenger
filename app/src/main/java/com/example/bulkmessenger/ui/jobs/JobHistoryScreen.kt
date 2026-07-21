package com.example.bulkmessenger.ui.jobs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bulkmessenger.data.BulkJob
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobMode
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.util.SimHelper
import com.example.bulkmessenger.viewmodel.DateSentCount
import com.example.bulkmessenger.viewmodel.JobsViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ModeFilter(val label: String, val mode: JobMode?) {
    ALL("All", null),
    BROADCAST("Broadcast", JobMode.SAME_MESSAGE),
    PERSONALIZED("Personalized", JobMode.PERSONALIZED)
}

@Composable
fun JobHistoryScreen(onBack: () -> Unit = {}, viewModel: JobsViewModel = viewModel()) {
    val jobsOrNull by viewModel.jobs.collectAsState()
    val totalSent by viewModel.totalSentCount.collectAsState()
    val sentByDate by viewModel.sentByDate.collectAsState()
    val sentTodayCounts by viewModel.sentTodayCounts.collectAsState()
    var expandedJobId by remember { mutableStateOf<Long?>(null) }
    var filter by remember { mutableStateOf(ModeFilter.ALL) }
    var retryPickerJobId by remember { mutableStateOf<Long?>(null) }
    // Null means "no manual choice yet" — the most recent day defaults to expanded, everything
    // older starts collapsed, so a long history doesn't turn into one giant wall of cards.
    var expandedDays by remember { mutableStateOf<Set<String>?>(null) }
    val context = LocalContext.current
    val sims = remember { SimHelper.getActiveSims(context) }

    val isLoading = jobsOrNull == null
    val jobs = jobsOrNull ?: emptyList()
    val filteredJobs = remember(jobs, filter) {
        jobs.filter { filter.mode == null || it.mode == filter.mode }
    }
    val dayGroups = remember(filteredJobs) { groupJobsByDay(filteredJobs) }
    val defaultExpandedDays = remember(dayGroups) { dayGroups.take(1).map { it.key }.toSet() }
    val currentExpandedDays = expandedDays ?: defaultExpandedDays
    val sentByDayLabel = remember(sentByDate) { sentByDate.associate { it.dateLabel to it.count } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                ModeFilter.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ModeFilter.entries.size),
                        onClick = { filter = option },
                        selected = filter == option
                    ) {
                        Text(option.label)
                    }
                }
            }

            if (isLoading) {
                LoadingState(modifier = Modifier.weight(1f))
            } else if (jobs.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else if (filteredJobs.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f), message = "No ${filter.label.lowercase()} jobs yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        SentSummaryCard(totalSent = totalSent, byDate = sentByDate)
                    }
                    dayGroups.forEach { day ->
                        val dayExpanded = day.key in currentExpandedDays
                        item(key = "day-${day.key}") {
                            DayHeader(
                                label = day.label,
                                jobCount = day.jobs.size,
                                sentCount = sentByDayLabel[day.label] ?: 0,
                                expanded = dayExpanded,
                                onClick = {
                                    val base = expandedDays ?: defaultExpandedDays
                                    expandedDays = if (dayExpanded) base - day.key else base + day.key
                                }
                            )
                        }
                        if (dayExpanded) {
                            items(day.jobs, key = { it.id }) { job ->
                                JobCard(
                                    job = job,
                                    expanded = expandedJobId == job.id,
                                    onToggleExpand = { expandedJobId = if (expandedJobId == job.id) null else job.id },
                                    viewModel = viewModel,
                                    sentTodayCounts = sentTodayCounts,
                                    onRetryClick = {
                                        if (sims.size >= 2) retryPickerJobId = job.id else viewModel.retryFailedItems(job.id, null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val pickerJobId = retryPickerJobId
    if (pickerJobId != null) {
        AlertDialog(
            onDismissRequest = { retryPickerJobId = null },
            title = { Text("Retry from which SIM?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sims.forEach { sim ->
                        TextButton(
                            onClick = {
                                viewModel.retryFailedItems(pickerJobId, sim.subscriptionId)
                                retryPickerJobId = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(sim.label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { retryPickerJobId = null }) { Text("Cancel") }
            }
        )
    }
}

private fun modeLabel(mode: JobMode): String = when (mode) {
    JobMode.SAME_MESSAGE -> "Broadcast"
    JobMode.PERSONALIZED -> "Personalized"
}

private data class DayGroup(val key: String, val label: String, val jobs: List<BulkJob>)

/** Groups by the job's creation date (local time), newest day first — same label format as [DateSentCount]. */
private fun groupJobsByDay(jobs: List<BulkJob>): List<DayGroup> {
    val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayLabelFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return jobs.groupBy { dayKeyFormat.format(Date(it.createdAt)) }
        .toSortedMap(compareByDescending { it })
        .map { (key, jobsInDay) -> DayGroup(key, dayLabelFormat.format(dayKeyFormat.parse(key)!!), jobsInDay) }
}

@Composable
private fun DayHeader(label: String, jobCount: Int, sentCount: Int, expanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "$jobCount job${if (jobCount == 1) "" else "s"} · $sentCount sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun JobCard(
    job: BulkJob,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: JobsViewModel,
    sentTodayCounts: Map<String, Int>,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${modeLabel(job.mode)} — #${job.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (job.status == JobStatus.SCHEDULED && job.scheduledAt != null) {
                        Text(
                            "Scheduled for ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(job.scheduledAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            DateFormat.getDateTimeInstance().format(Date(job.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (job.mode == JobMode.SAME_MESSAGE && !job.messagePreview.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "“${job.messagePreview}”",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(
                    label = job.status.name,
                    colors = statusColors(job.status),
                    onClick = onToggleExpand
                )
            }

            if (expanded) {
                val items by viewModel.itemsFor(job.id).collectAsState()
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                val sent = items.count { it.status == ItemStatus.SENT }
                val failed = items.count { it.status == ItemStatus.FAILED }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryStat(Icons.Filled.CheckCircle, "$sent sent", Color(0xFF2E7D32))
                    SummaryStat(Icons.Filled.Error, "$failed failed", MaterialTheme.colorScheme.error)
                    SummaryStat(Icons.Filled.History, "${items.size} total", MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                items.forEach { item ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                val todayCount = sentTodayCounts[item.phoneNumber] ?: 0
                                if (todayCount > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "$todayCount today",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                item.status.name + (item.errorReason?.let { " ($it)" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = itemStatusColor(item.status)
                            )
                        }
                        if (job.mode == JobMode.PERSONALIZED) {
                            Text(
                                item.messageBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Hidden entirely until there's actually something to retry — e.g. hit a
                // carrier's daily SMS cap partway through and the rest failed.
                if (failed > 0) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Failed ($failed)")
                    }
                }
            }
        }
    }
}

@Composable
private fun itemStatusColor(status: ItemStatus): Color = when (status) {
    ItemStatus.SENT -> Color(0xFF2E7D32)
    ItemStatus.FAILED -> MaterialTheme.colorScheme.error
    ItemStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun SentSummaryCard(totalSent: Int, byDate: List<DateSentCount>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "$totalSent",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Total messages sent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (byDate.isNotEmpty()) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide by day" else "Show by day")
                    }
                }
            }

            if (expanded && byDate.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))
                byDate.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${entry.count} sent", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

private data class ChipColors(val container: Color, val content: Color)

@Composable
private fun statusColors(status: JobStatus): ChipColors = when (status) {
    JobStatus.COMPLETED -> ChipColors(Color(0xFFD7EEDA), Color(0xFF1B5E20))
    JobStatus.FAILED -> ChipColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    JobStatus.SENDING -> ChipColors(Color(0xFFFFE9C6), Color(0xFF8A5300))
    JobStatus.QUEUED -> ChipColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    JobStatus.SCHEDULED -> ChipColors(Color(0xFFDCE3FF), Color(0xFF1A2C99))
}

@Composable
private fun StatusChip(label: String, colors: ChipColors, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (label == JobStatus.SCHEDULED.name) {
            { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.container,
            labelColor = colors.content
        )
    )
}

@Composable
private fun SummaryStat(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, message: String = "Send a broadcast or personalized batch to see it here.") {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("No jobs yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
