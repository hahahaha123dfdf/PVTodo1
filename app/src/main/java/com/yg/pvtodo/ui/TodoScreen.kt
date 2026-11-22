package com.yg.pvtodo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yg.pvtodo.task.TaskRepository
import kotlinx.coroutines.launch

@Composable
fun TodoScreen(repo: TaskRepository) {
    val scope = rememberCoroutineScope()
    val tasks by repo.observeAll().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) Text("还没有待办,点右下角添加", Modifier.align(Alignment.Center))
        LazyColumn(Modifier.fillMaxSize()) {
            items(tasks, key = { it.id }) { t ->
                ListItem(
                    headlineContent = { Text(t.title, style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { if (t.notes.isNotBlank()) Text(t.notes) },
                    leadingContent = { Checkbox(checked = t.done, onCheckedChange = { scope.launch { repo.toggle(t) } }) },
                    trailingContent = { Text("P${t.priority}") }
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Text("+") }
    }

    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var prio by remember { mutableStateOf(1f) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("新建待办") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注(可选)") })
                    Text("优先级: ${prio.toInt()}")
                    Slider(value = prio, onValueChange = { prio = it }, valueRange = 1f..3f, steps = 1)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) scope.launch { repo.add(title.trim(), notes.trim(), null, prio.toInt()) }
                    title = ""; notes = ""; showDialog = false
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } }
        )
    }
}
