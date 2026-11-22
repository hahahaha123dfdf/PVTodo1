package com.yg.pvtodo.llm

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 用户自己的 LLM 接口配置(OpenAI 兼容格式)。 */
data class LlmConfig(val baseUrl: String, val apiKey: String, val model: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

/** 把配置存在本地 SharedPreferences,并提供响应式的 StateFlow。 */
class LlmConfigStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("llm_config", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(load())
    val config: StateFlow<LlmConfig> = _config

    private fun load(): LlmConfig = LlmConfig(
        baseUrl = prefs.getString("base_url", "") ?: "",
        apiKey = prefs.getString("api_key", "") ?: "",
        model = prefs.getString("model", "") ?: ""
    )

    fun save(baseUrl: String, apiKey: String, model: String) {
        prefs.edit()
            .putString("base_url", baseUrl.trim())
            .putString("api_key", apiKey.trim())
            .putString("model", model.trim())
            .apply()
        _config.value = LlmConfig(baseUrl.trim(), apiKey.trim(), model.trim())
    }
}
