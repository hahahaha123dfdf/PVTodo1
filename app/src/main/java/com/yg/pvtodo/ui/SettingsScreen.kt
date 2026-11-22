package com.yg.pvtodo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yg.pvtodo.llm.LlmConfig

@Composable
fun SettingsScreen(config: LlmConfig, onSave: (String, String, String) -> Unit) {
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var model by remember(config.model) { mutableStateOf(config.model) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("LLM 设置(用于 AI 论文讲解)", style = MaterialTheme.typography.titleLarge)
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
        Spacer(Modifier.height(8.dp))
        Text(
            "配置好后,在「论文」标签里点任意论文的「AI 讲解」按钮即可生成导读\n" +
            "(创新点 / 方法 / 数据实验 / 局限 / 可借鉴 idea)。",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
