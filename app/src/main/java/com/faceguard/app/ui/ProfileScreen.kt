package com.faceguard.app.ui

import androidx.compose.foundation.background
import com.faceguard.app.viewmodel.ProfileViewModelFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.draw.clip
import com.faceguard.app.Routes
import com.faceguard.app.viewmodel.ProfileViewModel
import com.faceguard.data.database.FaceGuardDatabase
import com.faceguard.data.database.Profile
import com.faceguard.data.repository.ProfileRepository

// Colours matching MainActivity
val DarkBg = Color(0xFF0B0B14)
val DarkCard = Color(0xFF131320)
val DarkBorder = Color(0xFF1E1E36)
val AccentGreen = Color(0xFF00C878)
val AccentRed = Color(0xFFFF5577)
val AccentPurple = Color(0xFFAA88FF)
val AccentBlue = Color(0xFF5599FF)
val TextPrimary = Color(0xFFE4E4F4)
val TextMuted = Color(0xFF44446A)

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val database = FaceGuardDatabase.getDatabase(context)
    val repository = ProfileRepository(database.profileDao())
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(repository)
    )

    val profiles by viewModel.profiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(top = 52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Profiles", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("MANAGE FACE PROFILES", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { showAddDialog = true }
            ) {
                Text("+ Add", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile("👥", "${profiles.size}", "People", AccentGreen, Modifier.weight(1f))
            StatTile("🔒", "${profiles.count { it.isOwner }}", "Owners", AccentRed, Modifier.weight(1f))
            StatTile("📊", "${profiles.count { !it.isOwner }}", "Members", AccentBlue, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGreen)
            }
        } else if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👤", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No profiles yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Tap + Add to enroll your first face", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        onDelete = {
                            profileToDelete = profile
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        errorMessage?.let { error ->
            LaunchedEffect(error) {
                viewModel.clearError()
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, relation, isOwner ->
                viewModel.addProfile(name, relation, isOwner, null)
                showAddDialog = false
            }
        )
    }

    if (showDeleteDialog && profileToDelete != null) {
        DeleteProfileDialog(
            profile = profileToDelete!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteProfile(profileToDelete!!)
                showDeleteDialog = false
                profileToDelete = null
            }
        )
    }
}

@Composable
fun ProfileCard(profile: Profile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (profile.isOwner) AccentGreen.copy(alpha = 0.15f)
                                else AccentPurple.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (profile.isOwner) "👤" else "👥", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(profile.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${profile.relation} • ${if (profile.isOwner) "Owner" else "Member"}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed.copy(alpha = 0.15f))
                        .clickable { onDelete() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Delete", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (profile.faceVector != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreen.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("✓ Face enrolled", color = AccentGreen, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var isOwner by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Add Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Relation (e.g., Mom, Sister)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isOwner,
                        onCheckedChange = { isOwner = it },
                        colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                    )
                    Text("Is Owner", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, relation, isOwner) },
                enabled = name.isNotBlank() && relation.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = DarkBg)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun DeleteProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Delete Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Are you sure you want to delete ${profile.name}? This action cannot be undone.",
                color = TextMuted,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun StatTile(icon: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 9.sp)
        }
    }
}
