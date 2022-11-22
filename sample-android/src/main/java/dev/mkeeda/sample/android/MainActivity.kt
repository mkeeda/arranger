package dev.mkeeda.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dev.mkeeda.arranger.runtime.Text
import dev.mkeeda.arranger.runtime.document
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            document {
                var count by remember {
                    mutableStateOf(0)
                }
                Text(text = "Doc start")
                if (count % 2 == 0) {
                    Text(text = "Hello world $count")
                } else {
                    repeat(2) {
                        Text(text = "Twice hello")
                    }
                }
                count += 1
            }
        }
    }
}
