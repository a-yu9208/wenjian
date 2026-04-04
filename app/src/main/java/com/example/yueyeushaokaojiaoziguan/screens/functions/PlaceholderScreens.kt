package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PointsActivityScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎁", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("积分活动", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("消费积分比例、抵扣比例、手动增减积分", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("功能开发中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👤", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("个人信息", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("功能开发中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
