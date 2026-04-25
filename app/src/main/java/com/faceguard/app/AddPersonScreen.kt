package com.faceguard.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: String = "📱"
)

@Composable
fun AddPersonScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var name         by remember { mutableStateOf("") }
    var relation     by remember { mutableStateOf("Family") }
    var isOwner      by remember { mutableStateOf(false) }
    var photoUri     by remember { mutableStateOf<Uri?>(null) }
    var isSaving     by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val selectedApps = remember { mutableStateListOf<String>() }
    val relations = listOf("Family", "Friend", "Colleague", "Other")

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri }

    val commonApps = listOf(
        InstalledApp("com.instagram.android",      "Instagram",  "📸"),
        InstalledApp("com.whatsapp",               "WhatsApp",   "💬"),
        InstalledApp("com.snapchat.android",       "Snapchat",   "👻"),
        InstalledApp("org.telegram.messenger",     "Telegram",   "✈️"),
        InstalledApp("com.twitter.android",        "Twitter/X",  "🐦"),
        InstalledApp("com.facebook.katana",        "Facebook",   "👤"),
        InstalledApp("com.google.android.youtube", "YouTube",    "▶️"),
        InstalledApp("com.spotify.music",          "Spotify",    "🎵"),
        InstalledApp("com.netflix.mediaclient",    "Netflix",    "🎬"),
        InstalledApp("com.google.android.gm",      "Gmail",      "📧"),
        InstalledApp("com.google.android.apps.photos", "Photos", "🖼️"),
        InstalledApp("com.android.messaging",      "Messages",   "💌"),
    )

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCard)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Text("←", color = TextPrimary, fontSize = 18.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Add Person", color = TextPrimary,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("ENROLL A NEW FACE PROFILE", color = TextMuted,
                    fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo upload
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("FACE PHOTO", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Photo selected — tap to change", color = AccentGreen, fontSize = 12.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tap to upload a clear face photo", color = TextMuted, fontSize = 12.sp)
                            Text("Front facing, good lighting, no glasses", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Name input
            item {
                Text("NAME", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Mom, Sister, John", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = AccentGreen,
                        unfocusedBorderColor    = DarkBorder,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = AccentGreen,
                        focusedContainerColor   = DarkCard,
                        unfocusedContainerColor = DarkCard,
                    ),
                    singleLine = true
                )
            }

            // Relation picker
            item {
                Text("RELATION", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    relations.forEach { rel ->
                        val isSelected = relation == rel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentGreen.copy(alpha = 0.2f) else DarkCard)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clickable { relation = rel },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rel,
                                color = if (isSelected) AccentGreen else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Owner toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("This is me (Owner)", color = TextPrimary,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Owner gets full access to all apps", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isOwner,
                            onCheckedChange = { isOwner = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = DarkBg,
                                checkedTrackColor   = AccentGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBorder,
                            )
                        )
                    }
                }
            }

            // Apps to hide header
            item {
                if (!isOwner) {
                    Text("APPS TO HIDE FOR THIS PERSON", color = TextMuted,
                        fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tap apps to hide them when this person unlocks the phone",
                        color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // App list
            if (!isOwner) {
                items(commonApps) { app ->
                    val isSelected = selectedApps.contains(app.packageName)
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isSelected) selectedApps.remove(app.packageName)
                            else selectedApps.add(app.packageName)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AccentRed.copy(alpha = 0.12f) else DarkCard
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(app.icon, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(app.appName, color = TextPrimary, fontSize = 14.sp)
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier.size(22.dp)
                                        .clip(RoundedCornerShape(6.dp)).background(AccentRed),
                                    contentAlignment = Alignment.Center
                                ) { Text("🔒", fontSize = 11.sp) }
                            } else {
                                Box(modifier = Modifier.size(22.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(DarkBorder))
                            }
                        }
                    }
                }
            }

            // Error message
            item {
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AccentRed, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Save button
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMessage = "Please enter a name"
                        return@Button
                    }
                    if (photoUri == null) {
                        errorMessage = "Please upload a face photo"
                        return@Button
                    }
                    errorMessage = ""
                    isSaving = true

                    scope.launch {
                        try {
                            val profileId = FaceGuardDatabase.saveProfile(
                                context,
                                Profile(
                                    name     = name.trim(),
                                    relation = relation,
                                    isOwner  = isOwner
                                )
                            )
                            selectedApps.forEach { packageName ->
                                val appName = commonApps
                                    .find { it.packageName == packageName }
                                    ?.appName ?: packageName
                                FaceGuardDatabase.saveRule(
                                    context,
                                    AppRule(
                                        profileId   = profileId,
                                        packageName = packageName,
                                        appName     = appName
                                    )
                                )
                            }
                            FaceGuardDatabase.saveLog(
                                context,
                                ActivityLog(
                                    profileId  = profileId,
                                    personName = name.trim(),
                                    result     = "ENROLLED"
                                )
                            )
                            isSaving = false
                            onSaved()
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor   = DarkBg
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = DarkBg,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}