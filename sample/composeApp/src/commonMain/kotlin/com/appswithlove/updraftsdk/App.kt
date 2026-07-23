package com.appswithlove.updraftsdk

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appswithlove.updraft.Updraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Updraft SDK Sample") }) },
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Button(onClick = { Updraft.checkForUpdate() }) {
                    Text("Check for update")
                }
                Button(onClick = { Updraft.showFeedback() }) {
                    Text("Give feedback")
                }
            }
        }
    }
}
