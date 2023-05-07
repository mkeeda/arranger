package dev.mkeeda.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}

@Composable
fun TextFieldSample() {
    rememberScrollState()
    var value by remember {
        mutableStateOf(
            TextFieldValue(
                annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.Blue)) {
                        append("H")
                    }
                    append("ello ")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)) {
                        append("W")
                    }
                    append("orld")
                }
            ),
        )
    }
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            value = newValue.copy(
                annotatedString = AnnotatedString(
                    text = newValue.text,
                    spanStyles = value.annotatedString.spanStyles,
                    paragraphStyles = value.annotatedString.paragraphStyles
                )
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    Box(modifier = Modifier.background(Color.White)) {
        TextFieldSample()
    }
}
