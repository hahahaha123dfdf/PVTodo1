package com.yg.pvtodo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yg.pvtodo.llm.LlmConfig

@Composable
fun SettingsScreen(
    config: LlmConfig,
    hasPin: Boolean,
    onLock: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var model by remember(config.model) { mutableStateOf(config.model) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("设置", style = MaterialTheme.typography.titleLarge)

        // 应用锁
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("应用锁", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (hasPin) "已启用 PIN 锁。可点击下方立即锁定。" else "未设置 PIN(可在首次启动时设置)。",
                    style = MaterialTheme.typography.bodySmall
                )
                if (hasPin) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onLock, modifier = Modifier.fillMaxWidth()) {
                        Text("🔒 立即锁定")
                    }
                }
            }
        }

        Text("LLM 设置(用于 AI 论文讲解)", style = MaterialTheme.typography.titleMedium)
        Text(
            "填入你自己的 OpenAI 兼容接口。配置只保存在本机,不上传。\n" +
            "示例 Base URL: https://api.deepseek.com  或  https://api.openai.com/v1",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = baseUrl, onValueChange = { baseUrl = it; saved = false },
            label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it; saved = false },
            label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = model, onValueChange = { model = it; saved = false },
            label = { Text("模型名(如 deepseek-chat / qwen-plus / glm-4)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Button(
            onClick = { onSave(baseUrl, apiKey, model); saved = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存") }
        if (saved) Text("已保存 ✓", color = MaterialTheme.colorScheme.primary)
    }
}
