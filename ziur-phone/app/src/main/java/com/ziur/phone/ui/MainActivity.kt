package com.ziur.phone.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        val initial = intent?.data?.schemeSpecificPart.orEmpty()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Bronze, secondary = Wine, background = Ink, surface = Panel,
                onPrimary = Ink, onBackground = Ivory, onSurface = Ivory
            )) {
                DialerScreen(
                    initialNumber = initial,
                    contactsPermission = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
                    onDial = ::placeCall,
                    onEnableScreening = ::requestScreeningRole,
                    onLoadContacts = {
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 201)
                    }
                )
            }
        }
    }

    private fun placeCall(number: String) {
        if (number.isBlank()) return
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingNumber = number
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 200)
            return
        }
        getSystemService(TelecomManager::class.java)
            ?.placeCall(Uri.fromParts("tel", number, null), Bundle())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingNumber?.let(::placeCall)
            pendingNumber = null
        }
        if (requestCode == 201 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            recreate()
        }
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java) ?: return
            if (manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                startActivityForResult(
                    manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 102
                )
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
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(contactsPermission) {
        if (contactsPermission) contacts = ContactRepository.load(context)
    }
    val matches = remember(number, contacts) {
        val query = number.filter(Char::isDigit)
        if (query.isEmpty()) emptyList() else contacts.filter {
            T9Engine.matches(it.displayName, query) ||
                it.number.filter(Char::isDigit).contains(query)
        }.take(5)
    }
    val keys = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ZIUR", color = Bronze, fontSize = 11.sp, letterSpacing = 4.sp, fontWeight = FontWeight.SemiBold)
                Text("PHONE", color = Ivory, fontSize = 25.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Light)
            }
            Text("拨号", color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text("联系人 · T9 拼音搜索", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = number.ifBlank { "输入号码或拼音数字" },
                    color = Ivory, fontSize = 27.sp, fontWeight = FontWeight.Light,
                    letterSpacing = 1.2.sp, textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("例如：妈妈 → 6262", color = Bronze, fontSize = 12.sp)
                if (!contactsPermission) {
                    TextButton(onClick = onLoadContacts) {
                        Text("授权读取联系人以启用拼音搜索", color = Bronze)
                    }
                }
            }
        }
        if (matches.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    matches.forEach { contact ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onDial(contact.number) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(contact.displayName, color = Ivory, fontSize = 15.sp)
                                Text(contact.number, color = Muted, fontSize = 12.sp)
                            }
                            Text("拨打", color = Bronze, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            keys.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (digit, letters) ->
                        Surface(
                            color = Panel, shape = CircleShape,
                            modifier = Modifier.weight(1f).aspectRatio(1f).clickable { number += digit }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(digit, color = Ivory, fontSize = 27.sp, fontWeight = FontWeight.Light)
                                if (letters.isNotEmpty()) {
                                    Text(letters, color = Bronze, fontSize = 9.sp, letterSpacing = 2.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { number = number.dropLast(1) }) {
                Text("⌫", color = Muted, fontSize = 22.sp)
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = Color(0xFF536D58), shape = CircleShape,
                modifier = Modifier.size(66.dp).clickable { onDial(number) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("☎", color = Color.White, fontSize = 27.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEnableScreening) {
                Text("启用来电筛选", color = Bronze, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "测试版本：归属地号码索引尚未完成，来电默认放行。",
            color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center
        )
    }
}
