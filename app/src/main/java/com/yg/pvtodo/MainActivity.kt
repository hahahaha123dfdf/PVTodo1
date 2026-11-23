package com.yg.pvtodo

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
        val paperRepo = PaperRepository(db.paperDao(), PaperFetcher)
        val llmStore = LlmConfigStore(applicationContext)
        val vm = PapersViewModel(paperRepo, llmStore)
        val prefs = getSharedPreferences("app_lock", MODE_PRIVATE)

        setContent {
            MaterialTheme {
                // hasPin: 是否设过 PIN
                var hasPin by rememberSaveable { mutableStateOf(prefs.contains("pin_hash")) }
                // locked: 是否处于锁定状态(每次冷启动默认锁)
                var locked by rememberSaveable { mutableStateOf(prefs.contains("pin_hash")) }

                if (locked) {
                    AppLock(
                        hasPin = hasPin,
                        verify = { p -> AppLock.verify(prefs, p) },
                        onSetPin = { p -> AppLock.setPin(prefs, p); hasPin = true },
                        onUnlocked = { locked = false }
                    )
                } else {
                    RootNav(
                        taskRepo, vm,
                        hasPin = hasPin,
                        onLock = { if (hasPin) locked = true },
                        onExit = { finishAffinity() }
                    )
                }
            }
        }
    }
}

@Composable
fun RootNav(
    taskRepo: TaskRepository,
    vm: PapersViewModel,
    hasPin: Boolean,
    onLock: () -> Unit,
    onExit: () -> Unit
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    // 返回键:在一级页面(待办/论文/设置)按返回 = 退出应用;在二级弹窗等则正常返回
    BackHandler {
        // 当前在底部 tab 的根页面,直接退出
        onExit()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute(nav) == "todo",
                    onClick = {
                        scope.launch {
                            nav.navigate("todo") { popUpTo(0); launchSingleTop = true }
                        }
                    },
                    icon = { Text("✓") }, label = { Text("待办") })
                NavigationBarItem(
                    selected = currentRoute(nav) == "papers",
                    onClick = {
                        scope.launch {
                            nav.navigate("papers") { popUpTo(0); launchSingleTop = true }
                        }
                    },
                    icon = { Text("📄") }, label = { Text("论文") })
                NavigationBarItem(
                    selected = currentRoute(nav) == "settings",
                    onClick = {
                        scope.launch {
                            nav.navigate("settings") { popUpTo(0); launchSingleTop = true }
                        }
                    },
                    icon = { Text("⚙") }, label = { Text("设置") })
            }
        }
    ) { pad ->
        NavHost(nav, "todo", Modifier.padding(pad)) {
            composable("todo") { TodoScreen(taskRepo) }
            composable("papers") { PaperScreen(vm) }
            composable("settings") {
                val cfg by vm.llmConfig.collectAsState()
                SettingsScreen(cfg, hasPin,
                    onLock = onLock,
                    onSave = { b, k, m -> vm.llmStore.save(b, k, m) })
            }
        }
    }
}

@Composable
fun currentRoute(nav: NavHostController): String =
    nav.currentBackStackEntryAsState().value?.destination?.route ?: "todo"
