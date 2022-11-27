package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunTest {
    @Test
    fun test() = runTest {
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
