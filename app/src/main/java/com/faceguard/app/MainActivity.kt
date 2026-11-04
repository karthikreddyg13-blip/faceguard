package com.faceguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faceguard.app.ui.theme.FaceGuardTheme
import com.faceguard.app.ui.ProfileScreen
import com.faceguard.app.ui.RecognitionScreen
import com.faceguard.app.ui.ProfileAppPermissionsScreen
import com.faceguard.app.viewmodel.ActivityViewModel
import com.faceguard.app.viewmodel.ActivityViewModelFactory
import com.faceguard.data.database.FaceGuardDatabase
import com.faceguard.data.repository.ActivityLogRepository
import com.faceguard.data.repository.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ── Colours ───────────────────────────────────────────────────────────────────
val DarkBg       = Color(0xFF0B0B14)
val DarkCard     = Color(0xFF131320)
val DarkBorder   = Color(0xFF1E1E36)
val AccentGreen  = Color(0xFF00C878)
val AccentRed    = Color(0xFFFF5577)
val AccentPurple = Color(0xFFAA88FF)
val AccentBlue   = Color(0xFF5599FF)
val TextPrimary  = Color(0xFFE4E4F4)
val TextMuted    = Color(0xFF44446A)

// ── Nav routes ────────────────────────────────────────────────────────────────
object Routes {
    const val HOME                   = "home"
    const val PROFILES               = "profiles"
    const val ACTIVITY               = "activity"
    const val SETTINGS               = "settings"
    const val RECOGNITION            = "recognition"
    const val PROFILE_APP_PERMISSIONS = "profile_app_permissions"
}

// ── Main Activity ─────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceGuardTheme {
                FaceGuardApp()
            }
        }
    }
}

// ── App with navigation ───────────────────────────────────────────────────────
@Composable
fun FaceGuardApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (!granted) showPermissionDialog = true
    }

    LaunchedEffect(Unit) {
        val status = context.checkSelfPermission(android.Manifest.permission.CAMERA)
        if (status == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraGranted = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = DarkCard,
            title = { Text("Camera Required", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "FaceGuard needs camera access to scan faces at the lock screen.",
                    color = TextMuted, fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = DarkBg)
                ) { Text("Grant Permission") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Later", color = TextMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME)       { HomeScreen(navController) }
            composable(Routes.PROFILES)   { ProfileScreen(navController) }
            composable(Routes.ACTIVITY)   { ActivityScreen() }
            composable(Routes.SETTINGS)   { SettingsScreen() }
            composable(Routes.RECOGNITION) {
                RecognitionScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ── Bottom Nav Bar ────────────────────────────────────────────────────────────
@Composable
fun BottomNavBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val items = listOf(
        Triple(Routes.HOME,     "🏠", "Home"),
        Triple(Routes.PROFILES, "👥", "Profiles"),
        Triple(Routes.ACTIVITY, "📊", "Activity"),
        Triple(Routes.SETTINGS, "⚙️", "Settings"),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { (route, icon, label) ->
            val isActive = currentRoute == route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.HOME) { saveState = true }
                    }
                }
            ) {
                Text(icon, fontSize = 22.sp)
                Text(label,
                    color = if (isActive) AccentGreen else TextMuted,
                    fontSize = 9.sp, letterSpacing = 0.5.sp)
                if (isActive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(99.dp)).background(AccentGreen))
                }
            }
        }
    }
}

// ── Home Screen ───────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var profileCount by remember { mutableStateOf(0) }
    var totalAllowedApps by remember { mutableStateOf(0) }
    var totalBlockedApps by remember { mutableStateOf(0) }
    var recognitionCount by remember { mutableStateOf(0) }
    var recentActivity by remember { mutableStateOf<List<com.faceguard.data.database.ActivityLog>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            val database = FaceGuardDatabase.getDatabase(context)
            val profiles = database.profileDao().getAll().first()
            profileCount = profiles.size

            val allRules = database.appRuleDao().getAll().first()
            totalAllowedApps = allRules.count { it.isAllowed }
            totalBlockedApps = allRules.count { !it.isAllowed }

            val logs = database.activityLogDao().getAll().first()
            recognitionCount = logs.size
            recentActivity = logs.take(10)

            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(top = 52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("FaceGuard", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("ACTIVE PROTECTION", color = TextMuted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(DarkCard),
                contentAlignment = Alignment.Center
            ) { Text("⚙", fontSize = 16.sp) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                                .background(AccentGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) { Text("🛡", fontSize = 22.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Protection active", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Scanning at every lock screen", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("ON", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                        .clip(RoundedCornerShape(99.dp)).background(DarkBorder)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.78f).height(3.dp)
                        .clip(RoundedCornerShape(99.dp)).background(AccentGreen))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's accuracy", color = TextMuted, fontSize = 10.sp)
                    Text("78%", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile("👥", "$profileCount", "Enrolled", AccentGreen, Modifier.weight(1f))
            StatTile("🔒", "$totalBlockedApps",  "Blocked", AccentRed,    Modifier.weight(1f))
            StatTile("✅", "$recognitionCount", "Logs",   AccentPurple, Modifier.weight(1f))
            StatTile("📱", "$totalAllowedApps",  "Allowed", AccentBlue,   Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Button(
                onClick = { navController.navigate(Routes.RECOGNITION) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Recognition", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RECENT ACTIVITY", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text("See all →", color = AccentGreen, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (recentActivity.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent activity", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                items(recentActivity) { log ->
                    ActivityLogRow(log)
                }
            }
        }
    }
}


// ── Activity Screen ───────────────────────────────────────────────────────────
@Composable
fun ActivityScreen() {
    val context = LocalContext.current
    val database = FaceGuardDatabase.getDatabase(context)
    val activityLogRepository = ActivityLogRepository(database.activityLogDao())
    val viewModel: ActivityViewModel = viewModel(
        factory = ActivityViewModelFactory(activityLogRepository)
    )

    val activityLogs by viewModel.activityLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg)
            .padding(top = 52.dp, start = 16.dp, end = 16.dp)
    ) {
        Text("Activity Log", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp))
        Text("ALL ACTIVITY", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGreen)
            }
        } else if (activityLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No activity yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Activity will appear here", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(activityLogs) { log ->
                    ActivityLogRow(log)
                }
            }
        }
    }
}

// ── Settings Screen ───────────────────────────────────────────────────────────
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg)
            .padding(top = 52.dp, start = 16.dp, end = 16.dp)
    ) {
        Text("Settings", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(modifier = Modifier.height(16.dp))

        val settingsItems = listOf(
            Triple("🎯", "Detection Sensitivity", "High — scans at every unlock"),
            Triple("📸", "Intruder Photos",        "Save + upload to Google Photos"),
            Triple("🔕", "Silent Mode",             "No alerts when locking apps"),
            Triple("🔑", "Fallback PIN",            "Backup when camera fails"),
            Triple("🗄", "Face Data Storage",       "On-device only — never uploaded"),
            Triple("📱", "Hide App Icon",           "Make FaceGuard invisible"),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settingsItems) { (icon, title, sub) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                                .background(AccentGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) { Text(icon, fontSize = 17.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(sub, color = TextMuted, fontSize = 10.sp)
                        }
                        Text("›", color = TextMuted, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────
@Composable
fun StatTile(icon: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.Start) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
fun ActivityLogRow(log: com.faceguard.data.database.ActivityLog) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(AccentGreen))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        log.profileName ?: "Unknown",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        log.result,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                formatTime(log.timestamp),
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}