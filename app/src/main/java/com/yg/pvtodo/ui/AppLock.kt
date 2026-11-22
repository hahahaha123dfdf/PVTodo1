package com.yg.pvtodo.ui

import android.content.SharedPreferences
import java.security.MessageDigest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object AppLock {
    private fun sha(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    fun setPin(prefs: SharedPreferences, pin: String) { prefs.edit().putString("pin_hash", sha(pin)).apply() }
    fun verify(prefs: SharedPreferences, pin: String): Boolean =
        prefs.getString("pin_hash", null) == sha(pin)
}

@Composable
fun AppLock(
    hasPin: Boolean,
    verify: (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(if (hasPin) "verify" else "set") }
    var confirm by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (mode == "verify") "输入 PIN 解锁" else if (mode == "set") "设置 4 位 PIN" else "确认 PIN",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { pin = it; err = false } },
            label = { Text("PIN") }
        )
        if (err) Text("PIN 错误或两次不一致", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            when (mode) {
                "verify" -> if (verify(pin)) onUnlocked() else err = true
                "set" -> { confirm = pin; pin = ""; mode = "confirm" }
                "confirm" -> if (pin == confirm) { onSetPin(pin); onUnlocked() } else { err = true; pin = ""; mode = "set" }
            }
        }) { Text(if (mode == "verify") "解锁" else "下一步") }
    }
}
