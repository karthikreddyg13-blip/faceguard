package com.faceguard.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faceguard.app.ui.theme.FaceGuardTheme

class BlockScreenActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PROFILE_NAME = "profile_name"
        private const val TAG = "BlockScreenActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Unknown"
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Unknown App"
        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Unknown Profile"

        Log.d(TAG, "========== BLOCK SCREEN CREATED ==========")
        Log.d(TAG, "Package name: $packageName")
        Log.d(TAG, "App name: $appName")
        Log.d(TAG, "Profile name: $profileName")

        setContent {
            FaceGuardTheme {
                BlockScreen(
                    appName = appName,
                    profileName = profileName,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Block screen destroyed")
    }
}

@Composable
fun BlockScreen(
    appName: String,
    profileName: String,
    onBack: () -> Unit
) {
    val DarkBg = Color(0xFF0B0B14)
    val DarkCard = Color(0xFF131320)
    val AccentRed = Color(0xFFFF5577)
    val TextPrimary = Color(0xFFE4E4F4)
    val TextMuted = Color(0xFF44446A)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon
            Text(
                "🔒",
                fontSize = 80.sp
            )
            
            // Blocked card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "App Blocked",
                        color = AccentRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider(
                        color = TextMuted.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // App info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "App",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            appName,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Profile info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Current Profile",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            profileName,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    HorizontalDivider(
                        color = TextMuted.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        "This app is restricted for this profile.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            
            // Back button
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    "Back",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
