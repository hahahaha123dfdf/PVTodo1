package com.yg.pvtodo.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 调用 OpenAI 兼容的 chat/completions 接口,生成论文讲解。
 * 兼容 DeepSeek / 通义 / Kimi / GLM / 各类中转站(只要 base_url + key + model 对得上)。
 */
object LlmClient {

    /** 讲解 prompt:面向科研提升,输出结构化 6 块。 */
    private const val EXPLAIN_PROMPT = """你是一位光伏功率预测方向的资深科研导师。下面给你一篇论文的标题和摘要,
请你基于摘要(若信息不足可合理推断但要标注"摘要未明确")为一位正在写光伏预测大论文(第四章计划做 MoE 统一自适应框架)的研究生做导读。
请严格按以下 6 个小标题输出,每个小标题下用简洁中文,要点化,不要空话:

【一句话总结】这篇论文解决什么问题、核心贡献是什么(1-2 句)。
【创新点】它新在哪里,与以往方法的区别(2-4 条)。
【方法/模型】用了什么技术路线、模型结构、关键设计(要点)。
【数据与实验】用了什么数据集/场景、评测指标、对比了哪些基线(摘要有的写,没有的标"摘要未明确")。
【局限与不足】方法或实验可能存在的局限(2-3 条)。
【可借鉴的 idea】重点!结合光伏预测 / 混合专家(MoE)/ 多任务 / 不确定性 等方向,给出 2-3 条"这一点能否/如何用到我自己的研究上"的具体启发。

不要输出多余开场白,直接从【一句话总结】开始。"""

    /** 生成讲解。返回讲解文本,失败抛异常。 */
    suspend fun explain(config: LlmConfig, title: String, abstract: String): String =
        withContext(Dispatchers.IO) {
            if (!config.isConfigured) throw IllegalStateException("请先在设置里填写 Base URL / API Key / 模型名")

            val userMsg = "标题:$title\n\n摘要:$abstract"
            val body = JSONObject().apply {
                put("model", config.model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", EXPLAIN_PROMPT))
                    put(JSONObject().put("role", "user").put("content", userMsg))
                })
                put("temperature", 0.5)
            }

            // 容错:baseUrl 末尾带不带 / 和 /v1 都能拼
            var base = config.baseUrl.trim().trimEnd('/')
            if (!base.endsWith("/v1") && !base.contains("/v1/")) base = "$base/v1"
            val url = "$base/chat/completions"

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 90000

            try {
                conn.outputStream.use { os -> os.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val respText = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw RuntimeException("HTTP $code: ${err.take(300)}")
                }
                parseContent(respText)
            } finally {
                conn.disconnect()
            }
        }

    private fun parseContent(resp: String): String {
        val json = JSONObject(resp)
        return json.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
    }
}
