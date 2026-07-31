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
import com.faceguard.app.viewmodel.ActivityViewModel
import com.faceguard.app.viewmodel.ActivityViewModelFactory
import com.faceguard.app.FaceGuardDatabase as OldFaceGuardDatabase
import com.faceguard.data.database.FaceGuardDatabase
import com.faceguard.data.repository.ActivityLogRepository
import com.faceguard.data.repository.ProfileRepository
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

// ── Sample data ───────────────────────────────────────────────────────────────
data class ActivityItem(
    val name: String, val tag: String, val time: String,
    val dotColor: Color, val tagColor: Color
)

val sampleActivity = listOf(
    ActivityItem("You (Owner)",    "Full access",   "2 min ago", AccentGreen,  AccentGreen),
    ActivityItem("Mom",            "2 apps hidden", "1 hr ago",  Color(0xFFFF6B9D), AccentRed),
    ActivityItem("Sister",         "2 apps hidden", "2 hr ago",  AccentPurple, AccentPurple),
    ActivityItem("Unknown person", "All blocked",   "3 hr ago",  AccentRed,    AccentRed),
)

// ── Nav routes ────────────────────────────────────────────────────────────────
object Routes {
    const val HOME       = "home"
    const val PROFILES   = "profiles"
    const val ACTIVITY   = "activity"
    const val SETTINGS   = "settings"
    const val ADD_PERSON = "add_person"
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
            composable(Routes.HOME)       { HomeScreen() }
            composable(Routes.PROFILES)   { ProfileScreen(navController) }
            composable(Routes.ACTIVITY)   { ActivityScreen() }
            composable(Routes.SETTINGS)   { SettingsScreen() }
            composable(Routes.ADD_PERSON) {
                AddPersonScreen(
                    onBack  = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
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
fun HomeScreen() {
    val context = LocalContext.current
    var profileCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            profileCount = OldFaceGuardDatabase.getProfiles(context).size
            kotlinx.coroutines.delay(1000)
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
            StatTile("🔒", "5",  "Protected", AccentRed,    Modifier.weight(1f))
            StatTile("✅", "11", "Unlocks",   AccentPurple, Modifier.weight(1f))
            StatTile("⚠",  "1",  "Strangers", AccentBlue,   Modifier.weight(1f))
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
            items(sampleActivity) { item -> ActivityRow(item) }
        }
    }
}

// ── Profiles Screen ───────────────────────────────────────────────────────────
@Composable
fun ProfilesScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var appRules by remember { mutableStateOf<Map<Int, List<AppRule>>>(emptyMap()) }

    // Load real profiles from SharedPreferences database
    LaunchedEffect(Unit) {
        while (true) {
            profiles = OldFaceGuardDatabase.getProfiles(context)
            val rulesMap = mutableMapOf<Int, List<AppRule>>()
            profiles.forEach { profile ->
                rulesMap[profile.id] = OldFaceGuardDatabase.getRulesForProfile(context, profile.id)
            }
            appRules = rulesMap
            kotlinx.coroutines.delay(500)
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
                Text("Profiles", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("MANAGE FACE PROFILES", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { navController.navigate(Routes.ADD_PERSON) }
            ) {
                Text("+ Add", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile("👥", "${profiles.size}", "People",    AccentGreen, Modifier.weight(1f))
            StatTile("🔒", "${appRules.values.sumOf { it.size }}", "Protected", AccentRed, Modifier.weight(1f))
            StatTile("📊", "12", "Events", AccentBlue, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                    RealProfileCard(
                        profile = profile,
                        appRules = appRules[profile.id] ?: emptyList()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("STRANGER MODE", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                    .background(AccentRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Text("🔒", fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Unknown person", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("All private apps hidden automatically", color = TextMuted, fontSize = 10.sp)
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(AccentRed.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("All locked", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Real Profile Card — shows profiles from database ──────────────────────────
@Composable
fun RealProfileCard(profile: Profile, appRules: List<AppRule>) {
    val hiddenApps = appRules.filter { it.shouldHide }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (profile.isOwner) AccentGreen.copy(alpha = 0.15f) else AccentPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (profile.isOwner) "👤" else "👥", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (profile.isOwner) "Full access — all apps visible"
                        else "${hiddenApps.size} apps hidden",
                        color = TextMuted, fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (profile.isOwner) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (profile.isOwner) "Owner" else "${hiddenApps.size} hidden",
                        color = if (profile.isOwner) AccentGreen else AccentRed,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            if (hiddenApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    hiddenApps.take(3).forEach { rule ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(AccentRed.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("🔒 ${rule.appName}", color = AccentRed, fontSize = 9.sp)
                        }
                    }
                    if (hiddenApps.size > 3) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(AccentRed.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("+${hiddenApps.size - 3} more", color = AccentRed, fontSize = 9.sp)
                        }
                    }
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
    val profileRepository = ProfileRepository(database.profileDao())
    val viewModel: ActivityViewModel = viewModel(
        factory = ActivityViewModelFactory(activityLogRepository, profileRepository)
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
                items(activityLogs) { logWithProfile ->
                    ActivityRow(ActivityItem(
                        name = logWithProfile.profileName,
                        tag = logWithProfile.activityLog.result,
                        time = android.text.format.DateUtils.getRelativeTimeSpanString(
                            logWithProfile.activityLog.timestamp).toString(),
                        dotColor = when (logWithProfile.activityLog.result) {
                            "Profile Added" -> AccentGreen
                            "Profile Deleted" -> AccentRed
                            "Face Enrolled" -> AccentBlue
                            "OWNER" -> AccentGreen
                            "KNOWN" -> AccentPurple
                            "STRANGER" -> AccentRed
                            else -> TextMuted
                        },
                        tagColor = when (logWithProfile.activityLog.result) {
                            "Profile Added" -> AccentGreen
                            "Profile Deleted" -> AccentRed
                            "Face Enrolled" -> AccentBlue
                            "OWNER" -> AccentGreen
                            "KNOWN" -> AccentPurple
                            "STRANGER" -> AccentRed
                            else -> TextMuted
                        }
                    ))
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
fun ActivityRow(item: ActivityItem) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(item.dotColor))
                Spacer(modifier = Modifier.width(10.dp))
                Text(item.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(item.tagColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(item.tag, color = item.tagColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(item.time, color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}