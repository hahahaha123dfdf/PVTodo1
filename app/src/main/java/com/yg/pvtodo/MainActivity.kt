package com.yg.pvtodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.yg.pvtodo.db.AppDatabase
import com.yg.pvtodo.llm.LlmConfigStore
import com.yg.pvtodo.papers.*
import com.yg.pvtodo.task.TaskRepository
import com.yg.pvtodo.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(applicationContext)
        val taskRepo = TaskRepository(db.taskDao())
        val paperRepo = PaperRepository(db.paperDao(), PaperFetcher())
        val llmStore = LlmConfigStore(applicationContext)
        val vm = PapersViewModel(paperRepo, llmStore)
        val prefs = getSharedPreferences("app_lock", MODE_PRIVATE)

        setContent {
            MaterialTheme {
                var unlocked by rememberSaveable { mutableStateOf(false) }
                var hasPin by rememberSaveable { mutableStateOf(prefs.contains("pin_hash")) }
                if (!unlocked) {
                    AppLock(
                        hasPin = hasPin,
                        verify = { p -> AppLock.verify(prefs, p) },
                        onSetPin = { p -> AppLock.setPin(prefs, p); hasPin = true },
                        onUnlocked = { unlocked = true }
                    )
                } else {
                    RootNav(taskRepo, vm)
                }
            }
        }
    }
}

@Composable
fun RootNav(taskRepo: TaskRepository, vm: PapersViewModel) {
    val nav = rememberNavController()
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = true, onClick = { nav.navigate("todo") },
                icon = {}, label = { Text("待办") })
            NavigationBarItem(
                selected = false, onClick = { nav.navigate("papers") },
                icon = {}, label = { Text("论文") })
            NavigationBarItem(
                selected = false, onClick = { nav.navigate("settings") },
                icon = {}, label = { Text("设置") })
        }
    }) { pad ->
        NavHost(nav, "todo", Modifier.padding(pad)) {
            composable("todo") { TodoScreen(taskRepo) }
            composable("papers") { PaperScreen(vm) }
            composable("settings") {
                val cfg by vm.llmConfig.collectAsState()
                SettingsScreen(cfg) { b, k, m -> vm.llmStore.save(b, k, m) }
            }
        }
    }
}
