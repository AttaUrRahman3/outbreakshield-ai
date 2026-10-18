package com.example.domain.ai

import com.example.domain.model.AuditFinding
import com.example.domain.model.AuditInput
import com.example.domain.model.AuditResult
import com.example.domain.model.RiskLevel
import com.example.domain.model.SurfaceStatus
import com.example.domain.model.WorkflowType

sealed interface CoachingState {
    data object Idle : CoachingState
    data object Generating : CoachingState
    data class Success(
        val debriefText: String,
        val isLiveModelOutput: Boolean,
        val disclaimer: String = "AI-GENERATED ADVISORY DEBRIEF: For educational/coaching purposes only. Does not replace institutional SOPs, EPA labels, or biosafety officer directives."
    ) : CoachingState
    data class FallbackOrUnavailable(
        val reason: String,
        val fallbackDebrief: String,
        val disclaimer: String = "NOTICE: Advisory generated via deterministic standard reference guidelines. AI service is unconfigured or unavailable."
    ) : CoachingState
}

/**
 * Manages optional staff coaching and micro-training debriefs.
 *
 * CRITICAL SAFETY ARCHITECTURE:
 * 1. AI debrief is strictly advisory and educational.
 * 2. Deterministic findings and risk levels are IMMUTABLE.
 * 3. Never sends personal/patient data.
 * 4. Reconcile function guarantees AI cannot overwrite or downgrade findings.
 */
object GeminiCoachingManager {

    /**
     * Constructs a strictly sanitized prompt containing only non-confidential audit findings.
     */
    fun buildSanitizedPrompt(input: AuditInput, result: AuditResult): String {
        val workflowName = if (input.workflow == WorkflowType.HOSPITAL_CLEANING) "Hospital Clinical Ward" else "Microbiology Laboratory"
        val missedSurfaces = input.surfaces.filter { it.status == SurfaceStatus.MISSED_SURFACE }.map { it.surfaceName }
        val soiledSurfaces = input.surfaces.filter { it.status == SurfaceStatus.VISIBLE_SOIL }.map { it.surfaceName }

        val sb = StringBuilder()
        sb.appendLine("You are an Infection Prevention & Control (IPC) and Biosafety educator.")
        sb.appendLine("Context: $workflowName")
        sb.appendLine("Target Pathogen: ${input.pathogen.commonName}")
        sb.appendLine("Disinfectant Active Class: ${input.disinfectant.label}")
        sb.appendLine("Overall Audit Risk Level: ${result.riskLevel.name}")
        sb.appendLine("Unchecked Surfaces Count: ${input.surfaces.count { it.status == SurfaceStatus.NOT_CHECKED }}")

        if (missedSurfaces.isNotEmpty()) {
            sb.appendLine("Missed High-Touch Surfaces: ${missedSurfaces.joinToString(", ")}")
        }
        if (soiledSurfaces.isNotEmpty()) {
            sb.appendLine("Surfaces with Visible Soil: ${soiledSurfaces.joinToString(", ")}")
        }

        sb.appendLine("Deterministic Safety Violations Found:")
        result.findings.forEach { finding ->
            sb.appendLine("- [${finding.ruleId}] (${finding.severity.name}): ${finding.explanation}")
            sb.appendLine("  Required Action: ${finding.nextStep}")
        }

        sb.appendLine()
        sb.appendLine("Generate a concise, constructive 3-paragraph staff coaching script:")
        sb.appendLine("1. Microbiological mechanism: Why these specific findings matter for ${input.pathogen.commonName}.")
        sb.appendLine("2. Respectful staff debrief script for the supervisor to coach cleaning/lab personnel.")
        sb.appendLine("3. Immediate prioritized re-cleaning sequence.")
        sb.appendLine("CRITICAL RULES: Never invent universal contact times, disinfectant dilutions, or claim that unverified products are approved. Reinforce following the facility-approved SOP and EPA product label.")

        return sb.toString()
    }

    /**
     * Generates the safe, verified offline coaching script when Gemini is unconfigured or offline.
     */
    fun generateDeterministicFallbackCoaching(input: AuditInput, result: AuditResult): String {
        val sb = StringBuilder()
        sb.appendLine("EDUCATIONAL DEBRIEF & STAFF COACHING SCRIPT")
        sb.appendLine("Focus: Transmission Prevention for ${input.pathogen.commonName}")
        sb.appendLine()

        if (result.findings.any { it.ruleId == "RULE_CDIFF_QUAT_INCOMPATIBLE" }) {
            sb.appendLine("1. Microbiological Mechanism:")
            sb.appendLine("   Clostridioides difficile produces durable bacterial endospores surrounded by a tough, multi-layered protein coat. Standard quaternary ammonium compounds disrupt lipid membranes but cannot penetrate this spore coat, leaving viable spores on high-touch surfaces.")
            sb.appendLine("2. Staff Coaching Point:")
            sb.appendLine("   'Team, in rooms with C. diff precautions, standard quat wipes do not destroy spores. We must always reach for our facility-approved EPA List K sporicidal product and allow full label contact time.'")
        } else if (result.findings.any { it.ruleId == "RULE_LAB_SPILL_PROCEDURE" || it.ruleId == "RULE_LAB_ESCALATE_TO_BSO" }) {
            sb.appendLine("1. Biosafety Mechanism:")
            sb.appendLine("   Agitating or immediately dry-wiping live liquid culture suspensions aerosolizes infectious droplets and contaminates gloves. Containment requires gentle perimeter absorption and SOP-certified disinfectant dwell.")
            sb.appendLine("2. Staff Coaching Point:")
            sb.appendLine("   'Never wipe a live culture spill dry. Alert the room, verify PPE, contain the perimeter with absorbent towels, saturate with disinfectant, and if aerosols or broken glass are present, evacuate and notify the Biosafety Officer immediately.'")
        } else if (result.findings.any { it.ruleId == "RULE_DANGEROUS_CHEMICAL_MIXING" }) {
            sb.appendLine("1. Chemical Safety Alert:")
            sb.appendLine("   Sodium hypochlorite reacts rapidly with acids to liberate toxic chlorine gas, and with ammonia to liberate chloramine vapor. These vapors produce acute chemical pneumonitis.")
            sb.appendLine("2. Staff Coaching Point:")
            sb.appendLine("   'Never mix bleach with other cleaners. Surfaces must always be rinsed with clean water before changing chemical products.'")
        } else {
            sb.appendLine("1. Process Quality Summary:")
            sb.appendLine("   High-touch surfaces serve as primary transfer vectors in healthcare and laboratory environments. Thorough surface coverage and respecting label-certified wet dwell times are essential to break the transmission chain.")
            sb.appendLine("2. Staff Coaching Point:")
            sb.appendLine("   'Great effort on today's audit. Let's ensure every high-touch touchpoint is systematically cleaned and kept visibly wet for the entire duration specified on the product label.'")
        }

        sb.appendLine()
        sb.appendLine("3. Immediate Re-Cleaning Priority:")
        val missed = input.surfaces.filter { it.status == SurfaceStatus.MISSED_SURFACE }
        if (missed.isNotEmpty()) {
            sb.appendLine("   - Remediate omitted surfaces immediately: ${missed.joinToString(", ") { it.surfaceName }}")
        }
        val soiled = input.surfaces.filter { it.status == SurfaceStatus.VISIBLE_SOIL }
        if (soiled.isNotEmpty()) {
            sb.appendLine("   - Perform two-step pre-cleaning on soiled areas: ${soiled.joinToString(", ") { it.surfaceName }}")
        }
        if (missed.isEmpty() && soiled.isEmpty()) {
            sb.appendLine("   - Maintain standard cleaning frequency and verify disinfectant expiration dates.")
        }

        return sb.toString()
    }

    /**
     * CRITICAL SAFETY GUARD:
     * Reconciles an AI coaching debrief with the deterministic audit result.
     * GUARANTEES that under no circumstances can AI output alter, downgrade,
     * suppress, or delete any deterministic finding or risk level!
     */
    fun reconcileWithDeterministicRules(
        candidateAiText: String?,
        deterministicResult: AuditResult
    ): AuditResult {
        // Returns the deterministicResult completely untouched!
        // The deterministic riskLevel, findings, completeness, and disclaimer are strictly immutable.
        return deterministicResult.copy(
            isAiUsed = !candidateAiText.isNullOrBlank()
        )
    }
}
