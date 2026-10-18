package com.example

import com.example.domain.model.AuditInput
import com.example.domain.model.DisinfectantType
import com.example.domain.model.PathogenProfile
import com.example.domain.model.RiskLevel
import com.example.domain.model.SurfaceObservation
import com.example.domain.model.SurfaceStatus
import com.example.domain.model.WorkflowType
import com.example.domain.rules.OutbreakShieldRulesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OutbreakShieldRulesEngine.
 *
 * Verifies all 10 core safety requirements deterministically:
 * 1. C. difficile plus unverified quaternary ammonium
 * 2. Verified applicable sporicidal product claims
 * 3. Norovirus product verification
 * 4. Missed surfaces
 * 5. Unverified contact time
 * 6. Dangerous chemical mixing
 * 7. Lab spill routing
 * 8. Incomplete data
 * 9. Deterministic repeatability
 * 10. Offline operation
 */
class OutbreakShieldRulesEngineTest {

    private fun sampleCompliantSurfaces(): List<SurfaceObservation> = listOf(
        SurfaceObservation("Bed Rails", SurfaceStatus.CLEAN_AND_DISINFECTED),
        SurfaceObservation("Call Button", SurfaceStatus.CLEAN_AND_DISINFECTED),
        SurfaceObservation("IV Pump", SurfaceStatus.CLEAN_AND_DISINFECTED)
    )

    // 1. C. difficile plus unverified quaternary ammonium
    @Test
    fun testCDifficileWithUnverifiedQuat_TriggersCriticalFinding() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            hasVerifiedLabelClaimForPathogen = false,
            hasVerifiedContactTimeFollowed = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_CDIFF_QUAT_INCOMPATIBLE" && it.severity == RiskLevel.CRITICAL })
        assertTrue(result.findings.any { it.explanation.contains("Quaternary ammonium compounds lack standard sporicidal activity") })
    }

    // 2. Verified applicable sporicidal product claims
    @Test
    fun testVerifiedSporicidalClaims_DoesNotTriggerIncompatibility() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.ACCELERATED_HYDROGEN_PEROXIDE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertFalse(result.findings.any { it.ruleId == "RULE_CDIFF_QUAT_INCOMPATIBLE" })
        assertFalse(result.findings.any { it.ruleId == "RULE_CDIFF_UNVERIFIED_CLAIM" })
        assertEquals(RiskLevel.COMPLIANT, result.riskLevel)
    }

    // 3. Norovirus product verification
    @Test
    fun testNorovirusUnverifiedClaims_TriggersHighRiskFinding() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.NOROVIRUS,
            disinfectant = DisinfectantType.ALCOHOL,
            hasVerifiedLabelClaimForPathogen = false,
            hasVerifiedContactTimeFollowed = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertTrue(result.findings.any { it.ruleId == "RULE_NOROVIRUS_UNVERIFIED_CLAIM" && it.severity == RiskLevel.HIGH })
        assertTrue(result.findings.any { it.sourceReferenceNote.contains("EPA List G") })
    }

    // 4. Missed surfaces
    @Test
    fun testMissedSurfaces_TriggersHighRiskFinding() {
        val surfacesWithMiss = listOf(
            SurfaceObservation("Bed Rails", SurfaceStatus.CLEAN_AND_DISINFECTED),
            SurfaceObservation("Call Button", SurfaceStatus.MISSED_SURFACE)
        )
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.MRSA,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = surfacesWithMiss
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertTrue(result.findings.any { it.ruleId == "RULE_MISSED_SURFACE" && it.severity == RiskLevel.HIGH })
        assertTrue(result.findings.any { it.explanation.contains("Call Button") })
    }

    // 5. Unverified contact time
    @Test
    fun testUnverifiedContactTime_TriggersFinding() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.ROUTINE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = false, // Not followed / unverified
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertTrue(result.findings.any { it.ruleId == "RULE_UNVERIFIED_CONTACT_TIME" && it.severity == RiskLevel.HIGH })
    }

    // 6. Dangerous chemical mixing
    @Test
    fun testDangerousChemicalMixing_TriggersCriticalAlert() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.ROUTINE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            isAcidOrAmmoniaCoPresent = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_DANGEROUS_CHEMICAL_MIXING" && it.severity == RiskLevel.CRITICAL })
        assertTrue(result.findings.any { it.explanation.contains("toxic chlorine gas") })
    }

    // 7. Lab spill routing
    @Test
    fun testLabSpillRouting_TriggersFacilitySopRequirement() {
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.CRE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            isCultureSpillScenario = true,
            surfaces = listOf(SurfaceObservation("Spill Zone", SurfaceStatus.CLEAN_AND_DISINFECTED))
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertTrue(result.findings.any { it.ruleId == "RULE_LAB_SPILL_PROCEDURE" && it.severity == RiskLevel.HIGH })
        assertTrue(result.findings.any { it.nextStep.contains("Follow facility-approved biosafety spill SOP") })
    }

    // 8. Incomplete data
    @Test
    fun testIncompleteData_TriggersAuditIncompleteWarning() {
        val incompleteSurfaces = listOf(
            SurfaceObservation("Bed Rails", SurfaceStatus.CLEAN_AND_DISINFECTED),
            SurfaceObservation("Overbed Table", SurfaceStatus.NOT_CHECKED)
        )
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.ROUTINE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = incompleteSurfaces
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertTrue(result.findings.any { it.ruleId == "RULE_INCOMPLETE_AUDIT" && it.severity == RiskLevel.MODERATE })
        assertEquals(50, result.completenessScore)
    }

    // 9. Deterministic repeatability
    @Test
    fun testDeterministicRepeatability_ProducesIdenticalOutputAcrossRuns() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.CANDIDA_AURIS,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            hasVerifiedLabelClaimForPathogen = false,
            hasVerifiedContactTimeFollowed = false,
            surfaces = listOf(
                SurfaceObservation("Sink Faucet", SurfaceStatus.VISIBLE_SOIL),
                SurfaceObservation("Door Handle", SurfaceStatus.MISSED_SURFACE)
            )
        )

        val run1 = OutbreakShieldRulesEngine.evaluate(input)
        val run2 = OutbreakShieldRulesEngine.evaluate(input)
        val run3 = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(run1.riskLevel, run2.riskLevel)
        assertEquals(run1.riskLevel, run3.riskLevel)
        assertEquals(run1.findings.size, run2.findings.size)
        assertEquals(run1.findings.map { it.ruleId }, run2.findings.map { it.ruleId })
        assertEquals(run1.findings.map { it.ruleId }, run3.findings.map { it.ruleId })
    }

    // 10. Offline operation
    @Test
    fun testOfflineOperation_WorksWithoutNetworkOrGeminiKey() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.ROUTINE,
            disinfectant = DisinfectantType.ACCELERATED_HYDROGEN_PEROXIDE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertFalse(result.isAiUsed)
        assertNotNull(result.disclaimer)
        assertTrue(result.disclaimer.contains("NOT clinical infection probabilities"))
    }

    // 11. Laboratory Spill with Aerosol / Broken Glass requires BSO Escalation
    @Test
    fun testLabSpillWithAerosolOrBrokenGlass_TriggersCriticalBsoEscalation() {
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.CRE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            isCultureSpillScenario = true,
            hasAerosolOrBrokenGlassHazard = true,
            isPersonnelAuthorizedAndTrained = true,
            surfaces = listOf(SurfaceObservation("Spill Zone", SurfaceStatus.CLEAN_AND_DISINFECTED))
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_LAB_ESCALATE_TO_BSO" && it.severity == RiskLevel.CRITICAL })
        assertTrue(result.findings.any { it.nextStep.contains("contact the Institutional Biosafety Officer") })
    }

    // 12. Laboratory Spill with Unauthorized Staff requires Escalation
    @Test
    fun testLabSpillWithUnauthorizedPersonnel_TriggersCriticalBsoEscalation() {
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.MRSA,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            isCultureSpillScenario = true,
            hasAerosolOrBrokenGlassHazard = false,
            isPersonnelAuthorizedAndTrained = false, // Untrained responder
            surfaces = listOf(SurfaceObservation("Bench Spill", SurfaceStatus.CLEAN_AND_DISINFECTED))
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_LAB_ESCALATE_TO_BSO" })
    }

    // 13. Unchecked surfaces preserved as incomplete and never silently compliant
    @Test
    fun testUncheckedSurfaces_PreservedAsIncompleteAndNeverCompliant() {
        val labSurfaces = listOf(
            SurfaceObservation("BSC Interior Surface", SurfaceStatus.NOT_CHECKED),
            SurfaceObservation("BSC Air Grilles", SurfaceStatus.NOT_CHECKED)
        )
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.ROUTINE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            surfaces = labSurfaces
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(0, result.completenessScore)
        assertTrue(result.findings.any { it.ruleId == "RULE_INCOMPLETE_AUDIT" })
        assertTrue(result.riskLevel != RiskLevel.COMPLIANT)
    }

    // 14. Hospital cleaning workflow preserved without interference
    @Test
    fun testClinicalWorkflowPreserved_WithoutInterferenceFromLabRules() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            hasVerifiedLabelClaimForPathogen = false,
            hasVerifiedContactTimeFollowed = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_CDIFF_QUAT_INCOMPATIBLE" })
        assertFalse(result.findings.any { it.ruleId == "RULE_LAB_SPILL_PROCEDURE" })
        assertFalse(result.findings.any { it.ruleId == "RULE_LAB_ESCALATE_TO_BSO" })
    }

    // 15. C. difficile safety regression: switching verification control ON alone CANNOT erase critical finding
    @Test
    fun testCDifficileSafetyRegression_SwitchingVerificationControlOnAlone_CannotEraseCriticalFinding() {
        val input = AuditInput(
            workflow = WorkflowType.HOSPITAL_CLEANING,
            pathogen = PathogenProfile.C_DIFFICILE,
            disinfectant = DisinfectantType.QUATERNARY_AMMONIUM,
            productTradeName = "Generic Quaternary Hospital Wipe",
            hasVerifiedLabelClaimForPathogen = true, // User switched toggle ON in UI
            hasVerifiedContactTimeFollowed = true,
            surfaces = sampleCompliantSurfaces()
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        // Must STILL produce CRITICAL because quat lacks physical EPA List K proof
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_CDIFF_QUAT_INCOMPATIBLE" && it.severity == RiskLevel.CRITICAL })
        assertTrue(result.findings.any { it.explanation.contains("A UI switch alone cannot validate sporicidal efficacy") })
    }

    // 16. Dangerous chemical mixing in Microbiology Laboratory workflow
    @Test
    fun testDangerousChemicalMixing_InMicrobiologyLab_TriggersCriticalAlert() {
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.CRE,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            isAcidOrAmmoniaCoPresent = true, // Co-presence of acid/ammonia in lab
            surfaces = listOf(SurfaceObservation("BSC Interior Surface", SurfaceStatus.CLEAN_AND_DISINFECTED))
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_DANGEROUS_CHEMICAL_MIXING" && it.severity == RiskLevel.CRITICAL })
        assertTrue(result.findings.any { it.explanation.contains("toxic chlorine gas") })
    }

    // 17. Laboratory spill escalation: spill active alone without aerosol/untrained personnel
    @Test
    fun testLaboratorySpillActiveAlone_TriggersHighRiskSpillProcedure_WithoutBsoEscalation() {
        val input = AuditInput(
            workflow = WorkflowType.MICROBIOLOGY_LAB,
            pathogen = PathogenProfile.MRSA,
            disinfectant = DisinfectantType.SODIUM_HYPOCHLORITE,
            hasVerifiedLabelClaimForPathogen = true,
            hasVerifiedContactTimeFollowed = true,
            isCultureSpillScenario = true,
            hasAerosolOrBrokenGlassHazard = false,
            isPersonnelAuthorizedAndTrained = true,
            surfaces = listOf(SurfaceObservation("Spill Containment Zone", SurfaceStatus.CLEAN_AND_DISINFECTED))
        )

        val result = OutbreakShieldRulesEngine.evaluate(input)

        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(result.findings.any { it.ruleId == "RULE_LAB_SPILL_PROCEDURE" && it.severity == RiskLevel.HIGH })
        // Must NOT escalate to BSO because there is no aerosol/glass hazard and personnel is authorized/trained
        assertFalse(result.findings.any { it.ruleId == "RULE_LAB_ESCALATE_TO_BSO" })
    }
}

