package com.aptoide.diceroll.sdk.core.analytics.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ConsentBanner(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    val privacyText = buildAnnotatedString {
        append(
            "To help us verify your purchases and understand how you found our app, we use AppsFlyer to collect technical identifiers and to measure our marketing performance and ensure your account features are delivered correctly.\n" +
                "\n" +
                "By clicking \"Accept\", you consent to:\n" +
                "\n" +
                " - Data Usage: Sharing device info for attribution.\n" +
                "\n" +
                " - Personalization: Allowing us to verify event results for better service.\n" +
                "\n" +
                "You can withdraw your consent at any time in the App Settings. For more details, see our "
        )

        pushStringAnnotation(tag = "URL", annotation = "https://github.com/Aptoide/aptoide-diceroll-sdk/blob/master/PRIVACY_POLICY.md")

        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Privacy Policy")
        }

        pop()
        append(".")
    }
    // Full screen box to dim the background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) { }, // Prevent clicks passing through
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("We value your privacy", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                ClickableText(
                    text = privacyText,
                    style = MaterialTheme.typography.bodyMedium,
                    onClick = { offset ->
                        privacyText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                        Text("Decline")
                    }
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    ConsentBanner({ }, {})
}
