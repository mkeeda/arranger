package dev.mkeeda.arranger.sampleApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArrangerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        ) {
                            BasicUsageSample()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        ) {
                            AdvancedFormattingSample()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        ) {
                            CustomAttributeSample()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
