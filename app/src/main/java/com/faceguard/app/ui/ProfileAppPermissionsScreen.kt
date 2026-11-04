package com.faceguard.app.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faceguard.data.database.AppRule
import com.faceguard.data.database.Profile
import com.faceguard.data.repository.AppRuleRepository
import com.faceguard.data.database.FaceGuardDatabase
import com.faceguard.app.viewmodel.AppRuleViewModel
import com.faceguard.app.viewmodel.AppRuleViewModelFactory

val PermissionsBg = Color(0xFF0B0B14)
val PermissionsCard = Color(0xFF131320)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?
)

@Composable
fun ProfileAppPermissionsScreen(
    profile: Profile,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    val database = FaceGuardDatabase.getDatabase(context)
    val appRuleRepository = AppRuleRepository(database.appRuleDao())
    val viewModel: AppRuleViewModel = viewModel(
        factory = AppRuleViewModelFactory(appRuleRepository)
    )
    
    val appRules by viewModel.appRules.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var filteredApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    
    // Load installed apps
    LaunchedEffect(Unit) {
        installedApps = getInstalledApps(context)
        filteredApps = installedApps
    }
    
    // Load rules for this profile
    LaunchedEffect(profile.id, installedApps.size) {
        viewModel.loadRulesForProfile(profile.id, installedApps.size)
    }
    
    // Filter apps based on search
    LaunchedEffect(searchQuery, installedApps) {
        filteredApps = if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { 
                it.appName.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Clear error
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PermissionsBg)
            .padding(top = 52.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5577),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("←", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(profile.name, color = Color(0xFFE4E4F4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("App Permissions", color = Color(0xFF44446A), fontSize = 11.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PermissionsCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${summary.first}",
                        color = Color(0xFF00C878),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Allowed",
                        color = Color(0xFF44446A),
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${summary.second}",
                        color = Color(0xFFFF5577),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Blocked",
                        color = Color(0xFF44446A),
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${summary.third}",
                        color = Color(0xFF5599FF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Unconfigured",
                        color = Color(0xFF44446A),
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search apps...", color = Color(0xFF44446A)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00C878),
                unfocusedBorderColor = Color(0xFF1E1E36),
                focusedTextColor = Color(0xFFE4E4F4),
                unfocusedTextColor = Color(0xFFE4E4F4)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00C878))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    val rule = appRules.find { it.packageName == app.packageName }
                    AppPermissionItem(
                        app = app,
                        rule = rule,
                        onAllow = {
                            viewModel.addRule(profile.id, app.packageName, app.appName, true)
                        },
                        onBlock = {
                            viewModel.addRule(profile.id, app.packageName, app.appName, false)
                        },
                        onClear = {
                            if (rule != null) {
                                viewModel.deleteRule(rule)
                            }
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
}

@Composable
fun AppPermissionItem(
    app: InstalledApp,
    rule: AppRule?,
    onAllow: () -> Unit,
    onBlock: () -> Unit,
    onClear: () -> Unit
) {
    val isAllowed = rule?.isAllowed == true
    val isBlocked = rule?.isAllowed == false
    val isUnconfigured = rule == null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PermissionsCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App icon
                if (app.icon != null) {
                    val painter: Painter = remember(app.icon) {
                        BitmapPainter(app.icon!!.toBitmap().asImageBitmap())
                    }
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E36)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📱", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        app.appName,
                        color = Color(0xFFE4E4F4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        app.packageName,
                        color = Color(0xFF44446A),
                        fontSize = 10.sp
                    )
                }
            }
            
            // Allow/Block buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Allow button
                FilterChip(
                    selected = isAllowed,
                    onClick = {
                        if (isAllowed) {
                            onClear()
                        } else {
                            onAllow()
                        }
                    },
                    label = { Text("Allow", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E36),
                        labelColor = Color(0xFF44446A),
                        selectedContainerColor = Color(0xFF00C878),
                        selectedLabelColor = Color.White
                    ),
                    border = null,
                    modifier = Modifier.height(32.dp)
                )
                
                // Block button
                FilterChip(
                    selected = isBlocked,
                    onClick = {
                        if (isBlocked) {
                            onClear()
                        } else {
                            onBlock()
                        }
                    },
                    label = { Text("Block", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E36),
                        labelColor = Color(0xFF44446A),
                        selectedContainerColor = Color(0xFFFF5577),
                        selectedLabelColor = Color.White
                    ),
                    border = null,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

/**
 * Get list of installed apps on the device
 */
private fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val apps = mutableListOf<InstalledApp>()
    
    // Critical Android framework packages to exclude
    val criticalPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.contacts",
        "com.android.mms",
        "com.android.calculator2",
        "com.android.deskclock",
        "com.android.calendar",
        "com.android.camera2",
        "com.android.gallery3d"
    )
    
    // Common Google apps to always include
    val googleApps = setOf(
        "com.android.chrome",
        "com.google.android.googlequicksearchbox",
        "com.google.android.gm",
        "com.google.android.youtube",
        "com.google.android.apps.photos",
        "com.android.vending",
        "com.google.android.gms",
        "com.google.android.apps.maps",
        "com.google.android.apps.docs",
        "com.google.android.apps.tachyon",
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.google.android.calendar",
        "com.google.android.apps.photos",
        "com.google.android.apps.youtube.music",
        "com.google.android.play.games",
        "com.google.android.youtube.tv"
    )
    
    try {
        val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (appInfo in installedPackages) {
            try {
                val packageName = appInfo.packageName
                
                // Skip critical Android framework packages
                if (criticalPackages.contains(packageName)) {
                    continue
                }
                
                // Include Google apps even if they're system apps
                if (googleApps.contains(packageName)) {
                    addApp(appInfo, packageManager, apps)
                    continue
                }
                
                // Skip non-updatable system apps
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatableSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                
                if (isSystemApp && !isUpdatableSystemApp) {
                    continue
                }
                
                addApp(appInfo, packageManager, apps)
            } catch (e: Exception) {
                // Skip apps that can't be loaded
            }
        }
    } catch (e: Exception) {
        // Handle permission errors
    }
    
    return apps.sortedBy { it.appName.lowercase() }
}

private fun addApp(
    appInfo: android.content.pm.ApplicationInfo,
    packageManager: PackageManager,
    apps: MutableList<InstalledApp>
) {
    val appName = packageManager.getApplicationLabel(appInfo).toString()
    val packageName = appInfo.packageName
    val icon = try {
        packageManager.getApplicationIcon(appInfo)
    } catch (e: Exception) {
        null
    }
    
    apps.add(InstalledApp(packageName, appName, icon))
}
