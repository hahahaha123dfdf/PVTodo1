package com.yg.pvtodo.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yg.pvtodo.llm.LlmClient
import com.yg.pvtodo.papers.PaperEntity
import com.yg.pvtodo.papers.PapersViewModel
import kotlinx.coroutines.launch

@Composable
fun PaperScreen(vm: PapersViewModel) {
    val papers by vm.papers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val config by vm.llmConfig.collectAsState()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var explaining by remember { mutableStateOf<PaperEntity?>(null) }
    var explainText by remember { mutableStateOf<String?>(null) }
    var explainLoading by remember { mutableStateOf(false) }
    var explainError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("光伏预测论文", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { vm.refresh() }) { Text("刷新") }
        }
        if (!config.isConfigured) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text(
                    "提示:在「设置」里填入你的 LLM Base URL / API Key / 模型名后,可点「AI 讲解」生成导读。",
                    modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text("加载失败: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        if (papers.isEmpty() && !loading) Text("暂无论文,点刷新或等每日推送(收藏的会置顶)", Modifier.padding(16.dp))

        LazyColumn {
            items(papers, key = { it.doi }) { p ->
                PaperCard(
                    p = p,
                    canExplain = config.isConfigured,
                    onFav = { vm.toggleFav(p) },
                    onExplain = {
                        explaining = p; explainText = null; explainError = null; explainLoading = true
                        scope.launch {
                            try {
                                explainText = LlmClient.explain(config, p.title, p.abstract)
                            } catch (e: Exception) {
                                explainError = e.message ?: e.toString()
                            }
                            explainLoading = false
                        }
                    },
                    onOpen = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.link))) }
                )
            }
        }
    }

    if (explaining != null) {
        ExplanationDialog(
            title = explaining!!.title,
            loading = explainLoading,
            text = explainText,
            error = explainError,
            onCopy = {
                explainText?.let {
                    clipboard.setText(AnnotatedString(it))
                    Toast.makeText(ctx, "讲解已复制", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { explaining = null; explainText = null; explainError = null }
        )
    }
}

@Composable
fun PaperCard(p: PaperEntity, canExplain: Boolean, onFav: () -> Unit, onExplain: () -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(p.tier) })
                Spacer(Modifier.width(8.dp))
                Text("${p.source}${p.journal?.let { " · $it" } ?: ""} · ${p.published}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    if (p.fav) "★ " else "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(p.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (p.authors.isNotBlank()) Text(p.authors, style = MaterialTheme.typography.bodySmall)
            if (p.abstract.isNotBlank()) Text(
                p.abstract.take(200) + if (p.abstract.length > 200) "…" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("原文") }
                Button(onClick = onExplain, enabled = canExplain) { Text("AI 讲解") }
                Spacer(Modifier.weight(1f))
                // 收藏/取消收藏
                TextButton(onClick = onFav) { Text(if (p.fav) "已收藏" else "收藏") }
            }
        }
    }
}

@Composable
fun ExplanationDialog(
    title: String,
    loading: Boolean,
    text: String?,
    error: String?,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 论文导读", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("正在生成导读…")
                    }
                    error != null -> Text("生成失败: $error", color = MaterialTheme.colorScheme.error)
                    text != null -> Text(text, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row {
                // 一键复制
                TextButton(onClick = onCopy, enabled = text != null && !loading) { Text("复制") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}
