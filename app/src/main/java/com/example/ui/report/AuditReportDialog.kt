package com.example.ui.report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.ai.CoachingState
import com.example.domain.ai.GeminiCoachingManager
import com.example.domain.model.AuditInput
import com.example.domain.model.AuditResult
import com.example.domain.report.AuditReportFormatter
import com.example.ui.theme.ClinicalCyan
import com.example.ui.theme.ClinicalCyanContainer
import com.example.ui.theme.ClinicalCyanText
import com.example.ui.theme.ClinicalNavy
import com.example.ui.theme.SafetyHighAmber
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuditReportDialog(
    input: AuditInput,
    result: AuditResult,
    locationIdentifier: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var coachingState by remember { mutableStateOf<CoachingState>(CoachingState.Idle) }

    // Generates formatted report with or without optional AI debrief
    val reportText = remember(input, result, locationIdentifier, coachingState) {
        val debrief = when (val state = coachingState) {
            is CoachingState.Success -> state.debriefText
            is CoachingState.FallbackOrUnavailable -> "${state.fallbackDebrief}\n\n[Status: ${state.reason}]"
            else -> null
        }
        AuditReportFormatter.formatReport(input, result, locationIdentifier, debrief)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("audit_report_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audit Report & Export",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ClinicalNavy
                        )
                        Text(
                            text = "Sanitized audit documentation for handover",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(48.dp) // Minimum 48dp touch target
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = TextMuted
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceBorder)

                // Optional AI Coaching Button / Card
                when (val state = coachingState) {
                    is CoachingState.Idle -> {
                        Surface(
                            color = ClinicalCyanContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ClinicalCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Generate Staff Coaching Script",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = ClinicalCyanText
                                    )
                                    Text(
                                        text = "Demo Status: Live Gemini is not configured. Generates verified offline coaching fallback.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }

                                Button(
                                    onClick = {
                                        coachingState = CoachingState.Generating
                                        coroutineScope.launch {
                                            // Realistic educational synthesis delay and fallback cleanly to verified offline engine
                                            delay(1200)
                                            val fallbackText = GeminiCoachingManager.generateDeterministicFallbackCoaching(input, result)
                                            coachingState = CoachingState.FallbackOrUnavailable(
                                                reason = "Live Gemini API unconfigured — Deterministic Offline Fallback Active",
                                                fallbackDebrief = fallbackText
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ClinicalCyan),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("generate_coaching_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Debrief", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    is CoachingState.Generating -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateBackground, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ClinicalCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Synthesizing educational coaching script...",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    is CoachingState.FallbackOrUnavailable -> {
                        Surface(
                            color = SlateBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "ADVISORY STAFF COACHING INCLUDED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ClinicalNavy
                                )
                                Text(
                                    text = "Generated via verified reference guidelines. ${state.reason}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                    is CoachingState.Success -> {
                        Surface(
                            color = SlateBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Live AI debrief attached to report.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClinicalCyanText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Monospace Report Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = reportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Row (Copy to Clipboard & Share via Intent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy to Clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("OutbreakShield Report", reportText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp) // 48dp touch target
                            .testTag("copy_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text")
                    }

                    // Share Intent
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                putExtra(Intent.EXTRA_SUBJECT, "OutbreakShield Audit Report - $locationIdentifier")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share OutbreakShield Audit Report")
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalNavy),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp) // 48dp touch target
                            .testTag("share_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Report")
                    }
                }
            }
        }
    }
}
