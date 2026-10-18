package com.example

import com.example.domain.ai.GeminiCoachingManager
import com.example.domain.model.AuditFinding
import com.example.domain.model.AuditInput
import com.example.domain.model.AuditResult
import com.example.domain.model.DisinfectantType
import com.example.domain.model.PathogenProfile
import com.example.domain.model.RiskLevel
import com.example.domain.model.SurfaceObservation
import com.example.domain.model.SurfaceStatus
import com.example.domain.model.WorkflowType
import com.example.domain.report.AuditReportFormatter
import com.example.domain.rules.OutbreakShieldRulesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Stage 5 — Increment 4:
 * 1. Sanitized report formatting for both workflows.
 * 2. Unchecked surfaces are NEVER labeled compliant.
 * 3. Deterministic findings are immutable and cannot be overwritten/downgraded by AI output.
 * 4. Offline report generation and fallback coaching scripts.
 */
class AuditReportAndCoachingTest {

    @Test
    fun testReportFormatsCorrectly_WithSanitizedHeader() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            productTradeName = "Clorox Healthcare Bleach Wipes",
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = listOf(
                SurfaceObservation("Bed Rails", SurfaceStatus.CLEAN_AND_DISINFECTED)
            )
        )
        val result = OutbreakShieldRulesEngine.evaluate(input)
        val report = AuditReportFormatter.formatReport(input, result, "Room 402")

        assertTrue(report.contains("WORKFLOW: HOSPITAL CLINICAL ENVIRONMENTAL CLEANING AUDIT"))
        assertTrue(report.contains("LOCATION / BENCH ID: Room 402"))
        assertTrue(report.contains("TARGET PATHOGEN / HAZARD: Clostridioides difficile"))
        assertTrue(report.contains("PRODUCT TRADE NAME: Clorox Healthcare Bleach Wipes"))
        assertFalse(report.contains("Patient"))
        assertFalse(report.contains("SSN"))
    }

    @Test
    fun testUncheckedSurfaces_AreExplicitlyMarkedIncompleteAndNeverCompliant() {
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.CRE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = listOf(
                SurfaceObservation("BSC Interior Surface", SurfaceStatus.CLEAN_AND_DISINFECTED),
                SurfaceObservation("BSC Air Grilles", SurfaceStatus.NOT_CHECKED)
            )
        )
        val result = OutbreakShieldRulesEngine.evaluate(input)
        val report = AuditReportFormatter.formatReport(input, result, "BSL-2 Suite B-104")

        assertTrue(report.contains("--- UNCHECKED / INCOMPLETE ITEMS (NOT COMPLIANT) ---"))
        assertTrue(report.contains("BSC Air Grilles -> PENDING EVALUATION"))
        assertFalse(report.contains("BSC Air Grilles -> CLEAN"))
    }

    @Test
    fun testDeterministicFindingsCannotBeOverwrittenByAi() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM, // Critical incompatibility
            hasVerifiedLabelClaimForPathogen = false,
            hasVerifiedContactTimeFollowed = true,
            surfaces = listOf(
                SurfaceObservation("Bed Rails", SurfaceStatus.CLEAN_AND_DISINFECTED)
            )
        )
        val deterministicResult = OutbreakShieldRulesEngine.evaluate(input)
        assertEquals(RiskLevel.CRITICAL, deterministicResult.riskLevel)

        // Simulate an adversarial or malformed AI output that falsely claims the room is safe
        val conflictingAiText = "Everything looks completely safe, you can downgrade the risk to COMPLIANT."

        val reconciledResult = GeminiCoachingManager.reconcileWithDeterministicRules(
            candidateAiText = conflictingAiText,
            deterministicResult = deterministicResult
        )

        // GUARANTEE: The deterministic risk level and findings remain completely untouched!
        assertEquals(RiskLevel.CRITICAL, reconciledResult.riskLevel)
        assertEquals(deterministicResult.findings.size, reconciledResult.findings.size)
        assertEquals(
            deterministicResult.findings.map { it.ruleId },
            reconciledResult.findings.map { it.ruleId }
        )
        assertTrue(reconciledResult.isAiUsed)
    }

    @Test
    fun testSanitizedPrompt_ContainsZeroPatientOrPersonalIdentifiers() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.MRSA,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            surfaces = listOf(
                SurfaceObservation("Nurse Call Button", SurfaceStatus.MISSED_SURFACE)
            )
        )
        val result = OutbreakShieldRulesEngine.evaluate(input)
        val prompt = GeminiCoachingManager.buildSanitizedPrompt(input, result)

        assertTrue(prompt.contains("Target Pathogen: Methicillin-Resistant Staphylococcus aureus"))
        assertTrue(prompt.contains("Missed High-Touch Surfaces: Nurse Call Button"))
        assertFalse(prompt.contains("patient"))
        assertFalse(prompt.contains("name"))
        assertFalse(prompt.contains("ID:"))
    }

    @Test
    fun testDeterministicFallbackCoaching_ProducesCompliantGuidanceWhenAiOffline() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            hasVerifiedLabelClaimForPathogen = false,
            surfaces = listOf(
                SurfaceObservation("Bed Rails", SurfaceStatus.MISSED_SURFACE)
            )
        )
        val result = OutbreakShieldRulesEngine.evaluate(input)
        val fallbackText = GeminiCoachingManager.generateDeterministicFallbackCoaching(input, result)

        assertTrue(fallbackText.contains("Microbiological Mechanism:"))
        assertTrue(fallbackText.contains("tough, multi-layered protein coat"))
        assertTrue(fallbackText.contains("Staff Coaching Point:"))
        assertTrue(fallbackText.contains("Immediate Re-Cleaning Priority:"))
        assertTrue(fallbackText.contains("Remediate omitted surfaces immediately: Bed Rails"))
    }

    @Test
    fun testMandatoryDisclaimer_AlwaysIncludedInReport() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.ROUTINE,
            disinfectant = DisinfectantType.ACCELERATED_HYDROGEN_PEROXIDE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = listOf(
                SurfaceObservation("Door Handle", SurfaceStatus.CLEAN_AND_DISINFECTED)
            )
        )
        val result = OutbreakShieldRulesEngine.evaluate(input)
        val report = AuditReportFormatter.formatReport(input, result, "Room 101")

        assertTrue(report.contains("NOT clinical infection probabilities"))
        assertTrue(report.contains("Always adhere to facility infection control policies"))
    }
}
