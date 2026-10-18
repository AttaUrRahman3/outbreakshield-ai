package com.example.ui.lab

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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import com.example.ui.report.AuditReportDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
fun MicrobiologyLabScreen(modifier: Modifier = Modifier) {
    // Lab Configuration State
    var labAreaId by remember { mutableStateOf("BSL-2 Culture Suite B-104") }
    var selectedPathogen by remember { mutableStateOf(PathogenProfile.CRE) }
    var selectedDisinfectant by remember { mutableStateOf(DisinfectantType.SODIUM_HYPOCHLORITE) }
    var productTradeName by remember { mutableStateOf("Laboratory Disinfectant Bleach Solution") }
    var hasVerifiedLabelClaim by remember { mutableStateOf(false) }
    var hasVerifiedContactTime by remember { mutableStateOf(false) }
    var isAcidOrAmmoniaPresent by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Lab Spill Scenario State
    var isCultureSpillActive by remember { mutableStateOf(false) }
    var hasAerosolOrBrokenGlass by remember { mutableStateOf(false) }
    var isPersonnelAuthorized by remember { mutableStateOf(true) }

    // Surface Checklist State - initialized with standard lab surfaces, all NOT_CHECKED
    var labSurfaces by remember {
        mutableStateOf(DefaultChecklists.getLabSurfaceObservations())
    }

    // Deterministic Evaluation: re-runs synchronously in 0ms whenever any relevant state changes
    val auditResult by remember {
        derivedStateOf {
            val input = AuditInput(
                workflow = WorkflowType.MICROBIOLOGY_LAB,
                pathogen = selectedPathogen,
                disinfectant = selectedDisinfectant,
                productTradeName = productTradeName.trim(),
                hasVerifiedLabelClaimForPathogen = hasVerifiedLabelClaim,
                hasVerifiedContactTimeFollowed = hasVerifiedContactTime,
                surfaces = labSurfaces,
                isCultureSpillScenario = isCultureSpillActive,
                isAcidOrAmmoniaCoPresent = isAcidOrAmmoniaPresent,
                hasAerosolOrBrokenGlassHazard = hasAerosolOrBrokenGlass,
                isPersonnelAuthorizedAndTrained = isPersonnelAuthorized
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
            // 1. Lab Area & Biohazard Profile Card
            LabSetupCard(
                labAreaId = labAreaId,
                onLabAreaIdChange = { labAreaId = it },
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
                onAcidOrAmmoniaPresentChange = { isAcidOrAmmoniaPresent = it },
                isCultureSpillActive = isCultureSpillActive,
                onCultureSpillChange = { isCultureSpillActive = it },
                hasAerosolOrBrokenGlass = hasAerosolOrBrokenGlass,
                onAerosolOrBrokenGlassChange = { hasAerosolOrBrokenGlass = it },
                isPersonnelAuthorized = isPersonnelAuthorized,
                onPersonnelAuthorizedChange = { isPersonnelAuthorized = it }
            )
        }

        item {
            // 2. Live Biosafety Assessment Banner
            LiveLabAuditSummaryCard(
                completenessScore = auditResult.completenessScore,
                riskLevel = auditResult.riskLevel,
                evaluatedCount = labSurfaces.count { it.status != SurfaceStatus.NOT_CHECKED },
                totalCount = labSurfaces.size,
                disclaimer = auditResult.disclaimer,
                onExportClick = { showReportDialog = true }
            )
        }

        // 3. Immediate Deterministic Safety Feedback
        if (auditResult.findings.isNotEmpty()) {
            item {
                Text(
                    text = "Biosafety & Deterministic Findings (${auditResult.findings.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(auditResult.findings) { _, finding ->
                LabDeterministicFindingCard(finding = finding)
            }
        }

        item {
            // 4. Lab Surface Checklist Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Laboratory Touchpoint Checklist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Audit BSC, bench, centrifuge, and equipment surfaces",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    color = ClinicalCyanContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${labSurfaces.count { it.status != SurfaceStatus.NOT_CHECKED }}/${labSurfaces.size} Evaluated",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ClinicalCyanText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 5. Checklist Surface Items
        itemsIndexed(labSurfaces) { index, observation ->
            LabSurfaceChecklistItem(
                observation = observation,
                onStatusChange = { newStatus ->
                    labSurfaces = labSurfaces.toMutableList().also { list ->
                        list[index] = list[index].copy(status = newStatus)
                    }
                }
            )
        }

        item {
            // 6. Biosafety Reference & SOP Escalation Guidance
            BiosafetySopNoticeCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showReportDialog) {
        val currentInput = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = selectedPathogen,
            disinfectant = selectedDisinfectant,
            productTradeName = productTradeName.trim(),
            hasVerifiedLabelClaimForPathogen = hasVerifiedLabelClaim,
            hasVerifiedContactTimeFollowed = hasVerifiedContactTime,
            surfaces = labSurfaces,
            isCultureSpillScenario = isCultureSpillActive,
            isAcidOrAmmoniaCoPresent = isAcidOrAmmoniaPresent,
            hasAerosolOrBrokenGlassHazard = hasAerosolOrBrokenGlass,
            isPersonnelAuthorizedAndTrained = isPersonnelAuthorized
        )
        AuditReportDialog(
            input = currentInput,
            result = auditResult,
            locationIdentifier = labAreaId,
            onDismissRequest = { showReportDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabSetupCard(
    labAreaId: String,
    onLabAreaIdChange: (String) -> Unit,
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
    onAcidOrAmmoniaPresentChange: (Boolean) -> Unit,
    isCultureSpillActive: Boolean,
    onCultureSpillChange: (Boolean) -> Unit,
    hasAerosolOrBrokenGlass: Boolean,
    onAerosolOrBrokenGlassChange: (Boolean) -> Unit,
    isPersonnelAuthorized: Boolean,
    onPersonnelAuthorizedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "1. Laboratory Suite & Biological Hazard",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ClinicalNavy
            )

            // Lab Area ID Input
            OutlinedTextField(
                value = labAreaId,
                onValueChange = onLabAreaIdChange,
                label = { Text("Laboratory Area / Bench ID") },
                placeholder = { Text("e.g., BSL-2 Culture Suite B-104, Inoculation Bay") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lab_area_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClinicalCyan,
                    unfocusedBorderColor = SurfaceBorder
                )
            )

            // Pathogen / Hazard Selector
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
                    label = { Text("Active Biological Agent / Inoculum") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pathogenDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .testTag("lab_pathogen_selector"),
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

            // 2. Culture / Biological Spill Scenario Section
            Text(
                text = "2. Biological Incident / Spill Scenario",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ClinicalNavy
            )

            Surface(
                color = if (isCultureSpillActive) SafetyHighContainer else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isCultureSpillActive) SafetyHighAmber else SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCultureSpillChange(!isCultureSpillActive) }
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
                            text = "Live Culture / Biohazard Spill Active",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCultureSpillActive) SafetyHighText else TextPrimary
                        )
                        Text(
                            text = "Enforces facility biosafety spill SOP; forbids immediate dry-wiping.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isCultureSpillActive,
                        onCheckedChange = onCultureSpillChange,
                        modifier = Modifier.testTag("lab_spill_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SafetyHighAmber
                        )
                    )
                }
            }

            // Spill details if active
            if (isCultureSpillActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SafetyHighContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SPILL RESPONSE SAFEGUARD CONTROLS:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SafetyHighText
                    )

                    // Aerosol / Broken Glass hazard
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAerosolOrBrokenGlassChange(!hasAerosolOrBrokenGlass) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasAerosolOrBrokenGlass,
                            onCheckedChange = onAerosolOrBrokenGlassChange,
                            modifier = Modifier.testTag("lab_aerosol_checkbox"),
                            colors = CheckboxDefaults.colors(checkedColor = SafetyCriticalRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Aerosol hazard or broken glass/containers present",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasAerosolOrBrokenGlass) SafetyCriticalText else TextPrimary
                            )
                            Text(
                                text = "Requires immediate room evacuation & escalation to Biosafety Officer",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Authorized personnel checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonnelAuthorizedChange(!isPersonnelAuthorized) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPersonnelAuthorized,
                            onCheckedChange = onPersonnelAuthorizedChange,
                            modifier = Modifier.testTag("lab_authorized_checkbox"),
                            colors = CheckboxDefaults.colors(checkedColor = ClinicalCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Responder is trained & authorized for BSL-2 spill remediation",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!isPersonnelAuthorized) SafetyCriticalText else TextPrimary
                            )
                            Text(
                                text = "Untrained staff must NOT attempt spill cleanup alone",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SurfaceBorder)

            // 3. Laboratory Disinfectant Selection
            Text(
                text = "3. Disinfectant Chemistry & Decontamination Verification",
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
                    label = { Text("Disinfectant Active Chemical Class") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = disinfectantDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .testTag("lab_disinfectant_selector"),
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

            // Commercial Product Trade Name Input
            OutlinedTextField(
                value = productTradeName,
                onValueChange = onProductTradeNameChange,
                label = { Text("Commercial Product Name / Solution ID") },
                placeholder = { Text("e.g. Spor-Klenz, Cavicide, 70% Reagent Alcohol") },
                singleLine = true,
                supportingText = {
                    Text("Distinguish formulation identity from general chemical class.")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lab_product_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClinicalCyan,
                    unfocusedBorderColor = SurfaceBorder
                )
            )

            // Product claim verification switch
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
                            text = "Product Efficacy Claim Verified on Label",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasVerifiedLabelClaim) SafetyCompliantText else TextPrimary
                        )
                        Text(
                            text = "Documented claim for ${selectedPathogen.commonName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = hasVerifiedLabelClaim,
                        onCheckedChange = onVerifiedLabelClaimChange,
                        modifier = Modifier.testTag("lab_verified_claim_switch"),
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
                            text = "Observed manufacturer certified wet dwell time on surfaces",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = hasVerifiedContactTime,
                        onCheckedChange = onVerifiedContactTimeChange,
                        modifier = Modifier.testTag("lab_verified_contact_time_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SafetyCompliantGreen
                        )
                    )
                }
            }

            // Acid or Ammonia Co-presence Checkbox
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
                    modifier = Modifier.testTag("lab_acid_ammonia_checkbox"),
                    colors = CheckboxDefaults.colors(checkedColor = SafetyCriticalRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Acidic or ammoniacal reagents co-present",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isAcidOrAmmoniaPresent) SafetyCriticalText else TextSecondary
                    )
                    Text(
                        text = "Enforces chemical mixing safeguards (e.g. toxic chlorine gas release)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveLabAuditSummaryCard(
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
            "CRITICAL BIOSAFETY BREACH"
        )
        RiskLevel.HIGH -> listOf(
            SafetyHighContainer,
            SafetyHighAmber,
            SafetyHighText,
            "HIGH BIOSAFETY ESCALATION"
        )
        RiskLevel.MODERATE -> listOf(
            SafetyModerateContainer,
            SafetyModerateBlue,
            SafetyModerateText,
            "MODERATE DECONTAMINATION DEFECT"
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
            .testTag("lab_audit_risk_badge")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Biosafety Assessment",
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
                        text = "Touchpoint Checklist Completeness",
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

            // Biosafety Meaning Notice
            Surface(
                color = SlateBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ENVIRONMENTAL / BIOSAFETY AUDIT PRIORITY",
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
                    .testTag("export_lab_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preview & Export Biosafety Report",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun LabDeterministicFindingCard(finding: AuditFinding) {
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
            .testTag("lab_deterministic_finding_card")
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
                        text = "MANDATED BIOSAFETY ACTION:",
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
                text = "Standard: ${finding.sourceReferenceNote}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabSurfaceChecklistItem(
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
                            text = "Lab Critical",
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
                LabStatusChip(
                    label = "Decontaminated",
                    isSelected = observation.status == SurfaceStatus.CLEAN_AND_DISINFECTED,
                    selectedBg = SafetyCompliantContainer,
                    selectedBorder = SafetyCompliantGreen,
                    selectedText = SafetyCompliantText,
                    onClick = { onStatusChange(SurfaceStatus.CLEAN_AND_DISINFECTED) }
                )

                LabStatusChip(
                    label = "Visible Soil/Spill",
                    isSelected = observation.status == SurfaceStatus.VISIBLE_SOIL,
                    selectedBg = SafetyHighContainer,
                    selectedBorder = SafetyHighAmber,
                    selectedText = SafetyHighText,
                    onClick = { onStatusChange(SurfaceStatus.VISIBLE_SOIL) }
                )

                LabStatusChip(
                    label = "Omitted/Missed",
                    isSelected = observation.status == SurfaceStatus.MISSED_SURFACE,
                    selectedBg = SafetyCriticalContainer,
                    selectedBorder = SafetyCriticalRed,
                    selectedText = SafetyCriticalText,
                    onClick = { onStatusChange(SurfaceStatus.MISSED_SURFACE) }
                )

                LabStatusChip(
                    label = "Dwell Truncated",
                    isSelected = observation.status == SurfaceStatus.INCORRECT_CONTACT_TIME,
                    selectedBg = SafetyHighContainer,
                    selectedBorder = SafetyHighAmber,
                    selectedText = SafetyHighText,
                    onClick = { onStatusChange(SurfaceStatus.INCORRECT_CONTACT_TIME) }
                )

                LabStatusChip(
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
private fun LabStatusChip(
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
private fun BiosafetySopNoticeCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateBackground),
        border = BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Biosafety Shield",
                    tint = ClinicalNavy,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Institutional Biosafety & Spill Response Governance",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ClinicalNavy
                )
            }
            Text(
                text = "• BMBL 6th Edition Section IV: Never dry-wipe active bacterial/fungal culture spills.\n" +
                        "• Escalation Rule: If aerosols, broken glass, or unknown agents are present, evacuate and notify the Biosafety Officer (BSO).\n" +
                        "• BSC Decontamination: A biosafety cabinet sash does not substitute for certified chemical wet contact dwell time.\n" +
                        "• Chemical Mixing: Never combine bleach with ammonia, acids, or autoclaved biohazard solutions.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}
