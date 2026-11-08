package com.ziur.phone.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziur.phone.core.ContactRepository
import com.ziur.phone.core.DiagnosticLog
import com.ziur.phone.core.PhoneContact
import com.ziur.phone.core.T9Engine

private val Ink = Color(0xFF09090B)
private val Panel = Color(0xFF171316)
private val Wine = Color(0xFF4C202D)
private val Bronze = Color(0xFFC5A16B)
private val Ivory = Color(0xFFF1E9DB)
private val Muted = Color(0xFFAA9E91)

class MainActivity : ComponentActivity() {
    private var pendingNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagnosticLog.init(applicationContext)
        val initial = intent?.data?.schemeSpecificPart.orEmpty()
        val permission = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        DiagnosticLog.i("APP_START", "version=0.1.1-debug permission=$permission")
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Bronze, secondary = Wine, background = Ink, surface = Panel,
                onPrimary = Ink, onBackground = Ivory, onSurface = Ivory
            )) {
                DialerScreen(
                    initialNumber = initial,
                    contactsPermission = permission,
                    onDial = ::placeCall,
                    onEnableScreening = ::requestScreeningRole,
                    onLoadContacts = {
                        DiagnosticLog.i("PERMISSION", "request READ_CONTACTS")
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 201)
                    }
                )
            }
        }
    }

    private fun placeCall(number: String) {
        DiagnosticLog.i("DIAL", "requested=$number")
        if (number.isBlank()) return
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingNumber = number
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 200)
            return
        }
        runCatching {
            getSystemService(TelecomManager::class.java)?.placeCall(Uri.fromParts("tel", number, null), Bundle())
            DiagnosticLog.i("DIAL", "placeCall invoked")
        }.onFailure { DiagnosticLog.e("DIAL", "placeCall failed", it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        DiagnosticLog.i("PERMISSION_RESULT", "request=$requestCode granted=$granted")
        if (requestCode == 200 && granted) {
            pendingNumber?.let(::placeCall)
            pendingNumber = null
        }
        if (requestCode == 201) recreate()
    }

    private fun requestScreeningRole() {
        DiagnosticLog.i("SCREENING", "request role")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java) ?: return
            if (manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 102)
            }
        }
    }
}

@Composable
private fun DialerScreen(
    initialNumber: String,
    contactsPermission: Boolean,
    onDial: (String) -> Unit,
    onEnableScreening: () -> Unit,
    onLoadContacts: () -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    var contacts by remember { mutableStateOf(emptyList<PhoneContact>()) }
    var showLogs by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(contactsPermission) {
        if (contactsPermission) {
            runCatching { ContactRepository.load(context) }
                .onSuccess {
                    contacts = it
                    DiagnosticLog.i("CONTACT_LOAD", "count=" + it.size)
                }
                .onFailure { DiagnosticLog.e("CONTACT_LOAD", "query failed", it) }
        } else {
            contacts = emptyList()
            DiagnosticLog.w("CONTACT_LOAD", "permission missing")
        }
    }

    val query = number.filter(Char::isDigit)
    val nameMatches = remember(query, contacts) {
        if (query.isBlank()) emptyList() else contacts.filter { T9Engine.matches(it.displayName, query) }
    }
    val numberMatches = remember(query, contacts, nameMatches) {
        if (query.isBlank()) emptyList() else contacts.filter {
            it !in nameMatches && it.number.filter(Char::isDigit).contains(query)
        }
    }
    val matches = (nameMatches + numberMatches).take(2)

    LaunchedEffect(query, contacts.size) {
        if (query.isNotBlank()) {
            DiagnosticLog.i("T9_QUERY", "query=$query contacts=" + contacts.size +
                " nameMatches=" + nameMatches.size + " numberMatches=" + numberMatches.size)
            if (nameMatches.isEmpty()) {
                contacts.take(40).forEach {
                    DiagnosticLog.i("T9_SAMPLE", it.displayName + " => " + T9Engine.digitsForName(it.displayName))
                }
            } else {
                nameMatches.forEach {
                    DiagnosticLog.i("T9_MATCH", it.displayName + " / " + it.number)
                }
            }
        }
    }

    val keys = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ZIUR", color = Bronze, fontSize = 11.sp, letterSpacing = 4.sp, fontWeight = FontWeight.SemiBold)
                Text("PHONE", color = Ivory, fontSize = 25.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Light)
            }
            TextButton(onClick = {
                logText = DiagnosticLog.read()
                showLogs = true
            }) { Text("诊断日志", color = Bronze, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text("联系人 · T9 拼音搜索", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(number.ifBlank { "输入号码或拼音数字" }, color = Ivory, fontSize = 27.sp,
                    fontWeight = FontWeight.Light, letterSpacing = 1.2.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(5.dp))
                Text("例如：妈妈 → 6262", color = Bronze, fontSize = 12.sp)
                if (!contactsPermission) {
                    TextButton(onClick = onLoadContacts) { Text("授权读取联系人以启用拼音搜索", color = Bronze) }
                }
            }
        }

        // 固定结果槽：无论是否有匹配项都占相同高度，键盘不会随搜索结果跳动。
        Box(Modifier.fillMaxWidth().height(76.dp).padding(top = 7.dp), contentAlignment = Alignment.TopCenter) {
            when {
                matches.isNotEmpty() -> Surface(
                    color = Panel, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(68.dp)
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                        matches.forEach { contact ->
                            Row(
                                Modifier.fillMaxWidth().weight(1f).clickable {
                                    DiagnosticLog.i("CONTACT_PICK", contact.displayName + " / " + contact.number)
                                    onDial(contact.number)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(contact.displayName, color = Ivory, fontSize = 14.sp, maxLines = 1)
                                    Text(contact.number, color = Muted, fontSize = 11.sp, maxLines = 1)
                                }
                                Text("拨打", color = Bronze, fontSize = 12.sp)
                            }
                        }
                    }
                }
                query.isNotBlank() -> Text(
                    if (!contactsPermission) "尚未授权联系人读取" else "没有匹配联系人 · 点击「诊断日志」查看原因",
                    color = Muted, fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                )
                else -> Unit
            }
        }

        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (digit, letters) ->
                        Surface(
                            color = Panel, shape = CircleShape,
                            modifier = Modifier.weight(1f).aspectRatio(1f).clickable {
                                number += digit
                                DiagnosticLog.i("KEYPAD", "key=$digit query=" + number.filter(Char::isDigit))
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(digit, color = Ivory, fontSize = 27.sp, fontWeight = FontWeight.Light)
                                if (letters.isNotEmpty()) Text(letters, color = Bronze, fontSize = 9.sp, letterSpacing = 2.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                number = number.dropLast(1)
                DiagnosticLog.i("KEYPAD", "backspace")
            }) { Text("⌫", color = Muted, fontSize = 22.sp) }
            Spacer(Modifier.weight(1f))
            Surface(color = Color(0xFF536D58), shape = CircleShape,
                modifier = Modifier.size(66.dp).clickable { onDial(number) }) {
                Box(contentAlignment = Alignment.Center) { Text("☎", color = Color.White, fontSize = 27.sp) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEnableScreening) { Text("启用来电筛选", color = Bronze, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(4.dp))
        Text("测试版本：归属地号码索引尚未完成，来电默认放行。",
            color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
    }

    if (showLogs) {
        AlertDialog(
            onDismissRequest = { showLogs = false },
            title = { Text("ZiUR 诊断日志") },
            text = { Text(logText.ifBlank { "暂无日志" }, fontSize = 11.sp,
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ZiUR diagnostic log", logText))
                    DiagnosticLog.i("LOG_EXPORT", "copied to clipboard")
                }) { Text("复制日志") }
            },
            dismissButton = { TextButton(onClick = { showLogs = false }) { Text("关闭") } }
        )
    }
}
