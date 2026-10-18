package com.example.domain.report

import com.example.domain.model.AuditFinding
import com.example.domain.model.AuditInput
import com.example.domain.model.AuditResult
import com.example.domain.model.RiskLevel
import com.example.domain.model.SurfaceStatus
import com.example.domain.model.WorkflowType

/**
 * Generates standardized, sanitized audit reports for hospital wards and microbiology laboratories.
 *
 * SAFETY GUARANTEES:
 * 1. Zero patient identifiers or confidential staff records.
 * 2. Unchecked surfaces are NEVER labeled compliant.
 * 3. Deterministic findings and rule IDs are prominently displayed.
 * 4. The mandatory regulatory disclaimer is always appended.
 * 5. Works 100% offline.
 */
object AuditReportFormatter {

    fun formatReport(
        input: AuditInput,
        result: AuditResult,
        locationIdentifier: String,
        aiCoachingDebrief: String? = null
    ): String {
        val sb = StringBuilder()

        val workflowLabel = if (input.workflow == WorkflowType.HOSPITAL_CLEANING) {
            "HOSPITAL CLINICAL ENVIRONMENTAL CLEANING AUDIT"
        } else {
            "MICROBIOLOGY LABORATORY BIOSAFETY AUDIT"
        }

        sb.appendLine("==================================================")
        sb.appendLine("             OUTBREAKSHIELD AI REPORT             ")
        sb.appendLine("==================================================")
        sb.appendLine("WORKFLOW: $workflowLabel")
        sb.appendLine("LOCATION / BENCH ID: ${locationIdentifier.trim().ifEmpty { "Unspecified Location" }}")
        sb.appendLine("TARGET PATHOGEN / HAZARD: ${input.pathogen.commonName} (${input.pathogen.epaReferenceList})")
        sb.appendLine("DISINFECTANT CLASS: ${input.disinfectant.label}")
        sb.appendLine("PRODUCT TRADE NAME: ${input.productTradeName?.ifBlank { "Unspecified Commercial Product" } ?: "Unspecified"}")
        sb.appendLine("LABEL CLAIM VERIFIED ON FILE: ${if (input.hasVerifiedLabelClaimForPathogen) "YES (Documented)" else "NO (Unverified)"}")
        sb.appendLine("CERTIFIED CONTACT TIME FOLLOWED: ${if (input.hasVerifiedContactTimeFollowed) "YES (Observed)" else "NO (Unverified / Truncated)"}")

        if (input.workflow == WorkflowType.MICROBIOLOGY_LAB) {
            sb.appendLine("CULTURE SPILL ACTIVE: ${if (input.isCultureSpillScenario) "YES" else "NO"}")
            if (input.isCultureSpillScenario) {
                sb.appendLine("AEROSOL / BROKEN GLASS HAZARD: ${if (input.hasAerosolOrBrokenGlassHazard) "YES (Escalation Triggered)" else "NO"}")
                sb.appendLine("RESPONDER AUTHORIZED / TRAINED: ${if (input.isPersonnelAuthorizedAndTrained) "YES" else "NO (Escalation Triggered)"}")
            }
        }

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("AUDIT COMPLETENESS: ${result.completenessScore}%")
        val evaluatedCount = input.surfaces.count { it.status != SurfaceStatus.NOT_CHECKED }
        sb.appendLine("SURFACES EVALUATED: $evaluatedCount of ${input.surfaces.size}")
        sb.appendLine("OVERALL AUDIT PRIORITY RATING: ${result.riskLevel.name}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine()

        // 1. EVALUATED SURFACES
        val evaluatedSurfaces = input.surfaces.filter { it.status != SurfaceStatus.NOT_CHECKED }
        sb.appendLine("--- EVALUATED SURFACES ---")
        if (evaluatedSurfaces.isEmpty()) {
            sb.appendLine("[None evaluated yet]")
        } else {
            evaluatedSurfaces.forEachIndexed { i, obs ->
                val statusText = when (obs.status) {
                    SurfaceStatus.CLEAN_AND_DISINFECTED -> "CLEAN & DISINFECTED"
                    SurfaceStatus.VISIBLE_SOIL -> "VISIBLE SOIL / BIOBURDEN"
                    SurfaceStatus.MISSED_SURFACE -> "MISSED / OMITTED"
                    SurfaceStatus.INCORRECT_CONTACT_TIME -> "CONTACT TIME TRUNCATED"
                    SurfaceStatus.NOT_CHECKED -> "NOT CHECKED"
                }
                sb.appendLine("${i + 1}. ${obs.surfaceName} -> $statusText")
            }
        }
        sb.appendLine()

        // 2. UNCHECKED / INCOMPLETE SURFACES (Never marked compliant)
        val uncheckedSurfaces = input.surfaces.filter { it.status == SurfaceStatus.NOT_CHECKED }
        sb.appendLine("--- UNCHECKED / INCOMPLETE ITEMS (NOT COMPLIANT) ---")
        if (uncheckedSurfaces.isEmpty()) {
            sb.appendLine("[All checklist surfaces were evaluated]")
        } else {
            uncheckedSurfaces.forEachIndexed { i, obs ->
                sb.appendLine("${i + 1}. ${obs.surfaceName} -> PENDING EVALUATION")
            }
        }
        sb.appendLine()

        // 3. DETERMINISTIC SAFETY FINDINGS (Immutable)
        sb.appendLine("--- DETERMINISTIC SAFETY FINDINGS (${result.findings.size}) ---")
        if (result.findings.isEmpty()) {
            sb.appendLine("[No safety incompatibilities or process defects detected]")
        } else {
            result.findings.forEachIndexed { i, finding ->
                sb.appendLine("[$i] RULE: ${finding.ruleId} | SEVERITY: ${finding.severity.name}")
                sb.appendLine("    FINDING: ${finding.explanation}")
                sb.appendLine("    ACTION: ${finding.nextStep}")
                sb.appendLine("    STANDARD: ${finding.sourceReferenceNote}")
                sb.appendLine()
            }
        }

        // 4. OPTIONAL AI COACHING COMMENTARY (Clearly distinguished & advisory)
        if (!aiCoachingDebrief.isNullOrBlank()) {
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("--- AI-GENERATED ADVISORY DEBRIEF ---")
            sb.appendLine("NOTE: Generated for educational/micro-training purposes only.")
            sb.appendLine("Does NOT replace institutional SOPs, EPA labels, or BSO directives.")
            sb.appendLine()
            sb.appendLine(aiCoachingDebrief.trim())
            sb.appendLine("--------------------------------------------------")
            sb.appendLine()
        }

        // 5. REGULATORY & SAFETY DISCLAIMER
        sb.appendLine("==================================================")
        sb.appendLine(result.disclaimer)
        sb.appendLine("==================================================")

        return sb.toString()
    }
}
