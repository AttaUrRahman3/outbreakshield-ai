package com.example.ui.clinical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import com.example.ui.report.AuditReportDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.data.DefaultChecklists
import com.example.domain.model.AuditFinding
import com.example.domain.model.AuditInput
import com.example.domain.model.DisinfectantType
import com.example.domain.model.PathogenProfile
import com.example.domain.model.RiskLevel
import com.example.domain.model.SurfaceObservation
import com.example.domain.model.SurfaceStatus
import com.example.domain.model.WorkflowType
import com.example.domain.rules.OutbreakShieldRulesEngine
import com.example.ui.theme.ClinicalCyan
import com.example.ui.theme.ClinicalCyanContainer
import com.example.ui.theme.ClinicalCyanText
import com.example.ui.theme.ClinicalNavy
import com.example.ui.theme.ClinicalNavyLight
import com.example.ui.theme.SafetyCompliantContainer
import com.example.ui.theme.SafetyCompliantGreen
import com.example.ui.theme.SafetyCompliantText
import com.example.ui.theme.SafetyCriticalContainer
import com.example.ui.theme.SafetyCriticalRed
import com.example.ui.theme.SafetyCriticalText
import com.example.ui.theme.SafetyHighAmber
import com.example.ui.theme.SafetyHighContainer
import com.example.ui.theme.SafetyHighText
import com.example.ui.theme.SafetyModerateBlue
import com.example.ui.theme.SafetyModerateContainer
import com.example.ui.theme.SafetyModerateText
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalCleaningScreen(modifier: Modifier = Modifier) {
    // Audit Configuration State
    var roomId by remember { mutableStateOf("Room 402") }
    var selectedPathogen by remember { mutableStateOf(PathogenProfile.C_DIFFICILE) }
    var selectedDisinfectant by remember { mutableStateOf(DisinfectantType.QUATERNARY_AMMONIUM) }
    var productTradeName by remember { mutableStateOf("Standard Hospital Multi-Surface Wipe") }
    var hasVerifiedLabelClaim by remember { mutableStateOf(false) }
    var hasVerifiedContactTime by remember { mutableStateOf(false) }
    var isAcidOrAmmoniaPresent by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Surface Checklist State - initialized with standard high-touch surfaces, all NOT_CHECKED
    var surfaces by remember {
        mutableStateOf(DefaultChecklists.getClinicalSurfaceObservations())
    }

    // Deterministic Evaluation: re-runs synchronously in 0ms whenever any relevant state changes
    val auditResult by remember {
        derivedStateOf {
            val input = AuditInput(
                workflow = WorkflowType.HOSPITAL_CLEANING,
                pathogen = selectedPathogen,
                disinfectant = selectedDisinfectant,
                productTradeName = productTradeName.trim(),
                hasVerifiedLabelClaimForPathogen = hasVerifiedLabelClaim,
                hasVerifiedContactTimeFollowed = hasVerifiedContactTime,
                surfaces = surfaces,
                isCultureSpillScenario = false,
                isAcidOrAmmoniaCoPresent = isAcidOrAmmoniaPresent
            )
            OutbreakShieldRulesEngine.evaluate(input)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
                // 1. Audit Setup Card
                AuditSetupCard(
                    roomId = roomId,
                    onRoomIdChange = { roomId = it },
                    selectedPathogen = selectedPathogen,
                    onPathogenChange = { selectedPathogen = it },
                    selectedDisinfectant = selectedDisinfectant,
                    onDisinfectantChange = { selectedDisinfectant = it },
                    productTradeName = productTradeName,
                    onProductTradeNameChange = { productTradeName = it },
                    hasVerifiedLabelClaim = hasVerifiedLabelClaim,
                    onVerifiedLabelClaimChange = { hasVerifiedLabelClaim = it },
                    hasVerifiedContactTime = hasVerifiedContactTime,
                    onVerifiedContactTimeChange = { hasVerifiedContactTime = it },
                    isAcidOrAmmoniaPresent = isAcidOrAmmoniaPresent,
                    onAcidOrAmmoniaPresentChange = { isAcidOrAmmoniaPresent = it }
                )
            }

            item {
                // 2. Live Audit Summary & Priority Badge
                LiveAuditSummaryCard(
                    completenessScore = auditResult.completenessScore,
                    riskLevel = auditResult.riskLevel,
                    evaluatedCount = surfaces.count { it.status != SurfaceStatus.NOT_CHECKED },
                    totalCount = surfaces.size,
                    disclaimer = auditResult.disclaimer,
                    onExportClick = { showReportDialog = true }
                )
            }

            // 3. Immediate Deterministic Safety Feedback
            if (auditResult.findings.isNotEmpty()) {
                item {
                    Text(
                        text = "Deterministic Safety Findings (${auditResult.findings.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(auditResult.findings) { _, finding ->
                    DeterministicFindingCard(finding = finding)
                }
            }

            item {
                // 4. High-Touch Surface Checklist Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "High-Touch Surface Checklist",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Evaluate each critical transfer surface individually",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = ClinicalCyanContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${surfaces.count { it.status != SurfaceStatus.NOT_CHECKED }}/${surfaces.size} Checked",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ClinicalCyanText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 5. Checklist Surface Items
            itemsIndexed(surfaces) { index, observation ->
                SurfaceChecklistItem(
                    observation = observation,
                    onStatusChange = { newStatus ->
                        surfaces = surfaces.toMutableList().also { list ->
                            list[index] = list[index].copy(status = newStatus)
                        }
                    }
                )
            }

            item {
                // 6. Educational Disinfectant Matrix & Standards Notice
                RegulatoryReferenceCard()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

    if (showReportDialog) {
        val currentInput = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = selectedPathogen,
            disinfectant = selectedDisinfectant,
            productTradeName = productTradeName.trim(),
            hasVerifiedLabelClaimForPathogen = hasVerifiedLabelClaim,
            hasVerifiedContactTimeFollowed = hasVerifiedContactTime,
            surfaces = surfaces,
            isCultureSpillScenario = false,
            isAcidOrAmmoniaCoPresent = isAcidOrAmmoniaPresent
        )
        AuditReportDialog(
            input = currentInput,
            result = auditResult,
            locationIdentifier = roomId,
            onDismissRequest = { showReportDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditSetupCard(
    roomId: String,
    onRoomIdChange: (String) -> Unit,
    selectedPathogen: PathogenProfile,
    onPathogenChange: (PathogenProfile) -> Unit,
    selectedDisinfectant: DisinfectantType,
    onDisinfectantChange: (DisinfectantType) -> Unit,
    productTradeName: String,
    onProductTradeNameChange: (String) -> Unit,
    hasVerifiedLabelClaim: Boolean,
    onVerifiedLabelClaimChange: (Boolean) -> Unit,
    hasVerifiedContactTime: Boolean,
    onVerifiedContactTimeChange: (Boolean) -> Unit,
    isAcidOrAmmoniaPresent: Boolean,
    onAcidOrAmmoniaPresentChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "1. Room & Pathogen Profile",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ClinicalNavy
            )

            // Room ID Input
            OutlinedTextField(
                value = roomId,
                onValueChange = onRoomIdChange,
                label = { Text("Room / Bed Identifier") },
                placeholder = { Text("e.g., Room 402, ICU-04") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_id_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClinicalCyan,
                    unfocusedBorderColor = SurfaceBorder
                )
            )

            // Pathogen Dropdown
            var pathogenDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = pathogenDropdownExpanded,
                onExpandedChange = { pathogenDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "${selectedPathogen.commonName} (${selectedPathogen.epaReferenceList})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Pathogen / Isolation Reason") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pathogenDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .testTag("pathogen_selector"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClinicalCyan,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                ExposedDropdownMenu(
                    expanded = pathogenDropdownExpanded,
                    onDismissRequest = { pathogenDropdownExpanded = false }
                ) {
                    PathogenProfile.entries.forEach { pathogen ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(text = pathogen.commonName, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = pathogen.epaReferenceList,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            },
                            onClick = {
                                onPathogenChange(pathogen)
                                pathogenDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceBorder)

            Text(
                text = "2. Disinfectant Chemistry & Label Verification",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ClinicalNavy
            )

            // Disinfectant Class Dropdown
            var disinfectantDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = disinfectantDropdownExpanded,
                onExpandedChange = { disinfectantDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDisinfectant.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Active Disinfectant Chemical Class") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = disinfectantDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .testTag("disinfectant_selector"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClinicalCyan,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                ExposedDropdownMenu(
                    expanded = disinfectantDropdownExpanded,
                    onDismissRequest = { disinfectantDropdownExpanded = false }
                ) {
                    DisinfectantType.entries.forEach { disinfectant ->
                        DropdownMenuItem(
                            text = { Text(text = disinfectant.label) },
                            onClick = {
                                onDisinfectantChange(disinfectant)
                                disinfectantDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Product Trade Name Input (clearly distinguishing product identity from chemical class)
            OutlinedTextField(
                value = productTradeName,
                onValueChange = onProductTradeNameChange,
                label = { Text("Commercial Product Trade Name") },
                placeholder = { Text("e.g. Clorox Bleach Wipes, OxyCide, Virex II 256") },
                singleLine = true,
                supportingText = {
                    Text("Distinguish specific brand/formulation from general chemical class.")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClinicalCyan,
                    unfocusedBorderColor = SurfaceBorder
                )
            )

            // Label Claim Verification Switch
            Surface(
                color = if (hasVerifiedLabelClaim) SafetyCompliantContainer else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (hasVerifiedLabelClaim) SafetyCompliantGreen else SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVerifiedLabelClaimChange(!hasVerifiedLabelClaim) }
                    .padding(4.dp)
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
                            text = "Product-Specific Claim Verified on Label",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasVerifiedLabelClaim) SafetyCompliantText else TextPrimary
                        )
                        Text(
                            text = "EPA registration confirmed for ${selectedPathogen.commonName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = hasVerifiedLabelClaim,
                        onCheckedChange = onVerifiedLabelClaimChange,
                        modifier = Modifier.testTag("verified_label_claim_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SafetyCompliantGreen
                        )
                    )
                }
            }

            // Contact Time Verification Switch
            Surface(
                color = if (hasVerifiedContactTime) SafetyCompliantContainer else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (hasVerifiedContactTime) SafetyCompliantGreen else SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVerifiedContactTimeChange(!hasVerifiedContactTime) }
                    .padding(4.dp)
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
                            text = "Certified Wet Contact Time Followed",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasVerifiedContactTime) SafetyCompliantText else TextPrimary
                        )
                        Text(
                            text = "Surfaces kept visibly wet for manufacturer-certified duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = hasVerifiedContactTime,
                        onCheckedChange = onVerifiedContactTimeChange,
                        modifier = Modifier.testTag("verified_contact_time_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SafetyCompliantGreen
                        )
                    )
                }
            }

            // Chemical Co-Presence Checkbox (Mixing Hazard)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAcidOrAmmoniaPresentChange(!isAcidOrAmmoniaPresent) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAcidOrAmmoniaPresent,
                    onCheckedChange = onAcidOrAmmoniaPresentChange,
                    modifier = Modifier.testTag("acid_ammonia_switch"),
                    colors = CheckboxDefaults.colors(checkedColor = SafetyCriticalRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Acid or ammonia co-present on surfaces",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isAcidOrAmmoniaPresent) SafetyCriticalText else TextSecondary
                    )
                    Text(
                        text = "Enables chemical mixing hazard detection (e.g. chlorine gas risks)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveAuditSummaryCard(
    completenessScore: Int,
    riskLevel: RiskLevel,
    evaluatedCount: Int,
    totalCount: Int,
    disclaimer: String,
    onExportClick: () -> Unit
) {
    val (badgeBg, badgeBorder, badgeTextColor, badgeLabel) = when (riskLevel) {
        RiskLevel.CRITICAL -> listOf(
            SafetyCriticalContainer,
            SafetyCriticalRed,
            SafetyCriticalText,
            "CRITICAL SAFETY BREACH"
        )
        RiskLevel.HIGH -> listOf(
            SafetyHighContainer,
            SafetyHighAmber,
            SafetyHighText,
            "HIGH TRANSMISSION RISK"
        )
        RiskLevel.MODERATE -> listOf(
            SafetyModerateContainer,
            SafetyModerateBlue,
            SafetyModerateText,
            "MODERATE PROCESS DEFECT"
        )
        RiskLevel.COMPLIANT -> listOf(
            SafetyCompliantContainer,
            SafetyCompliantGreen,
            SafetyCompliantText,
            "STANDARD COMPLIANT"
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, badgeBorder as Color),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_risk_badge")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Audit Assessment",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ClinicalNavy
                )

                Surface(
                    color = badgeBg as Color,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = badgeLabel as String,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = badgeTextColor as Color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Completeness Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Checklist Completeness",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )
                    Text(
                        text = "$completenessScore% ($evaluatedCount of $totalCount evaluated)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (completenessScore == 100) SafetyCompliantText else TextPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { completenessScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (completenessScore == 100) SafetyCompliantGreen else ClinicalCyan,
                    trackColor = SurfaceBorder
                )
            }

            // Transparent Risk Meaning Explanation
            Surface(
                color = SlateBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AUDIT PRIORITY RATING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = disclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        lineHeight = 14.sp
                    )
                }
            }

            // Preview & Export Report Button
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = ClinicalNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("export_hospital_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preview & Export Audit Report",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DeterministicFindingCard(finding: AuditFinding) {
    val (cardBg, borderCol, textCol) = when (finding.severity) {
        RiskLevel.CRITICAL -> listOf(SafetyCriticalContainer, SafetyCriticalRed, SafetyCriticalText)
        RiskLevel.HIGH -> listOf(SafetyHighContainer, SafetyHighAmber, SafetyHighText)
        RiskLevel.MODERATE -> listOf(SafetyModerateContainer, SafetyModerateBlue, SafetyModerateText)
        RiskLevel.COMPLIANT -> listOf(SafetyCompliantContainer, SafetyCompliantGreen, SafetyCompliantText)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg as Color),
        border = BorderStroke(1.dp, borderCol as Color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deterministic_alert_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (finding.severity == RiskLevel.CRITICAL) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = "Alert Icon",
                        tint = textCol as Color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = finding.ruleId,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = textCol
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = finding.severity.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textCol,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = finding.explanation,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )

            // Corrective Action Step
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "REQUIRED CORRECTIVE ACTION:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textCol
                    )
                    Text(
                        text = finding.nextStep,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            // Reference Source
            Text(
                text = "Reference: ${finding.sourceReferenceNote}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurfaceChecklistItem(
    observation: SurfaceObservation,
    onStatusChange: (SurfaceStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = observation.surfaceName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ClinicalNavy,
                    modifier = Modifier.weight(1f)
                )

                if (observation.isHighTouch) {
                    Surface(
                        color = ClinicalCyanContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "High-Touch",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ClinicalCyanText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 5 Single-Tap Status Chips with Minimum 48dp Touch Target
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusChip(
                    label = "Clean & Disinfected",
                    isSelected = observation.status == SurfaceStatus.CLEAN_AND_DISINFECTED,
                    selectedBg = SafetyCompliantContainer,
                    selectedBorder = SafetyCompliantGreen,
                    selectedText = SafetyCompliantText,
                    onClick = { onStatusChange(SurfaceStatus.CLEAN_AND_DISINFECTED) }
                )

                StatusChip(
                    label = "Visible Soil",
                    isSelected = observation.status == SurfaceStatus.VISIBLE_SOIL,
                    selectedBg = SafetyHighContainer,
                    selectedBorder = SafetyHighAmber,
                    selectedText = SafetyHighText,
                    onClick = { onStatusChange(SurfaceStatus.VISIBLE_SOIL) }
                )

                StatusChip(
                    label = "Missed",
                    isSelected = observation.status == SurfaceStatus.MISSED_SURFACE,
                    selectedBg = SafetyCriticalContainer,
                    selectedBorder = SafetyCriticalRed,
                    selectedText = SafetyCriticalText,
                    onClick = { onStatusChange(SurfaceStatus.MISSED_SURFACE) }
                )

                StatusChip(
                    label = "Dwell Defect",
                    isSelected = observation.status == SurfaceStatus.INCORRECT_CONTACT_TIME,
                    selectedBg = SafetyHighContainer,
                    selectedBorder = SafetyHighAmber,
                    selectedText = SafetyHighText,
                    onClick = { onStatusChange(SurfaceStatus.INCORRECT_CONTACT_TIME) }
                )

                StatusChip(
                    label = "Not Checked",
                    isSelected = observation.status == SurfaceStatus.NOT_CHECKED,
                    selectedBg = SlateBackground,
                    selectedBorder = SurfaceBorder,
                    selectedText = TextMuted,
                    onClick = { onStatusChange(SurfaceStatus.NOT_CHECKED) }
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedBorder: Color,
    selectedText: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) selectedBg else Color.White,
        border = BorderStroke(1.dp, if (isSelected) selectedBorder else SurfaceBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .heightIn(min = 48.dp) // Minimum 48dp accessible touch target
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label status chip, ${if (isSelected) "selected" else "not selected"}" }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) selectedText else TextSecondary
            )
        }
    }
}

@Composable
private fun RegulatoryReferenceCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateBackground),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Standard Icon",
                    tint = ClinicalNavy,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Disinfection Standards & Verification Framework",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ClinicalNavy
                )
            }
            Text(
                text = "• EPA List K: Certified sporicidal products effective against Clostridioides difficile endospores.\n" +
                        "• EPA List G / List N: Registered disinfectants effective against Norovirus and viral pathogens.\n" +
                        "• Two-Step Protocol: Clean gross organic soil first with detergent before applying surface disinfectant.\n" +
                        "• Contact Dwell Time: Surfaces must remain continuously wet for the full label-certified duration.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}
