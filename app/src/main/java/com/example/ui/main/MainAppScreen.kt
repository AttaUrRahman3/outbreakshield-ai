package com.example.ui.main

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.WorkflowType
import com.example.ui.clinical.HospitalCleaningScreen
import com.example.ui.lab.MicrobiologyLabScreen
import com.example.ui.theme.ClinicalCyan
import com.example.ui.theme.ClinicalCyanContainer
import com.example.ui.theme.ClinicalCyanText
import com.example.ui.theme.ClinicalNavy
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var selectedWorkflow by remember { mutableStateOf(WorkflowType.HOSPITAL_CLEANING) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ClinicalCyanContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield Logo",
                                    tint = ClinicalNavy,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OutbreakShield AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (selectedWorkflow == WorkflowType.HOSPITAL_CLEANING)
                                    "Clinical Inpatient Environmental Module"
                                else
                                    "Microbiology Laboratory Biosafety Module",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClinicalCyanContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClinicalNavy,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = SlateBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Workflow Navigation Bar
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .background(SlateBackground, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WorkflowTab(
                        title = "Hospital Wards",
                        icon = Icons.Default.LocalHospital,
                        isSelected = selectedWorkflow == WorkflowType.HOSPITAL_CLEANING,
                        testTag = "nav_hospital_tab",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedWorkflow = WorkflowType.HOSPITAL_CLEANING }
                    )

                    WorkflowTab(
                        title = "Microbiology Lab",
                        icon = Icons.Default.Biotech,
                        isSelected = selectedWorkflow == WorkflowType.MICROBIOLOGY_LAB,
                        testTag = "nav_lab_tab",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedWorkflow = WorkflowType.MICROBIOLOGY_LAB }
                    )
                }
            }

            // Workflow Screen Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedWorkflow) {
                    WorkflowType.HOSPITAL_CLEANING -> {
                        HospitalCleaningScreen()
                    }
                    WorkflowType.MICROBIOLOGY_LAB -> {
                        MicrobiologyLabScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) ClinicalNavy else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .heightIn(min = 48.dp) // Minimum 48dp accessible touch target
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .semantics { contentDescription = "$title workflow tab, ${if (isSelected) "selected" else "not selected"}" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}
