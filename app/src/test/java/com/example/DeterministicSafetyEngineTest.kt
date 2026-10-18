package com.example

import com.example.domain.data.DefaultChecklists
import com.example.domain.model.AuditRiskLevel
import com.example.domain.model.DisinfectantClass
import com.example.domain.model.FacilityWorkflow
import com.example.domain.model.LabScenario
import com.example.domain.model.SurfaceCondition
import com.example.domain.model.TargetOrganism
import com.example.domain.model.ViolationSeverity
import com.example.domain.rules.DeterministicSafetyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicSafetyEngineTest {

    @Test
    fun testCDifficileWithQuat_ReturnsCriticalIncompatibility() {
        val surfaces = DefaultChecklists.getClinicalInpatientSurfaces()
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.CLINICAL_ENVIRONMENTAL,
            organism = TargetOrganism.CLOSTRIDIOIDES_DIFFICILE,
            disinfectant = DisinfectantClass.QUATERNARY_AMMONIUM,
            surfaces = surfaces
        )

        assertEquals(AuditRiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.violations.any { it.severity == ViolationSeverity.CRITICAL_INCOMPATIBILITY })
        assertTrue(result.violations.any { it.title.contains("Quats vs. Bacterial Endospores") })
    }

    @Test
    fun testCDifficileWithAlcohol_ReturnsCriticalIncompatibility() {
        val surfaces = DefaultChecklists.getClinicalInpatientSurfaces()
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.CLINICAL_ENVIRONMENTAL,
            organism = TargetOrganism.CLOSTRIDIOIDES_DIFFICILE,
            disinfectant = DisinfectantClass.ALCOHOL_70,
            surfaces = surfaces
        )

        assertEquals(AuditRiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.violations.any { it.severity == ViolationSeverity.CRITICAL_INCOMPATIBILITY })
        assertTrue(result.violations.any { it.title.contains("Alcohol vs. Bacterial Endospores") })
    }

    @Test
    fun testNorovirusWithAlcohol_ReturnsHighRiskBreach() {
        val surfaces = DefaultChecklists.getClinicalInpatientSurfaces()
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.CLINICAL_ENVIRONMENTAL,
            organism = TargetOrganism.NOROVIRUS,
            disinfectant = DisinfectantClass.ALCOHOL_70,
            surfaces = surfaces
        )

        assertTrue(result.violations.any { it.severity == ViolationSeverity.HIGH_RISK_BREACH })
        assertTrue(result.violations.any { it.title.contains("Non-Enveloped Viruses") })
    }

    @Test
    fun testVisibleSoilLoad_TriggersPreCleaningCaution() {
        val surfaces = DefaultChecklists.getClinicalInpatientSurfaces().mapIndexed { index, item ->
            if (index == 0) item.copy(condition = SurfaceCondition.VISIBLE_SOIL_DUST) else item
        }
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.CLINICAL_ENVIRONMENTAL,
            organism = TargetOrganism.MRSA_VRE,
            disinfectant = DisinfectantClass.SODIUM_HYPOCHLORITE,
            surfaces = surfaces
        )

        assertTrue(result.violations.any { it.severity == ViolationSeverity.CAUTION_LABEL_DWELL })
        assertTrue(result.violations.any { it.title.contains("Pre-Cleaning Required") })
    }

    @Test
    fun testLabCultureSpill_TriggersBsl2Warning() {
        val surfaces = DefaultChecklists.getLabSurfaces(LabScenario.LIQUID_CULTURE_SPILL)
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.MICROBIOLOGY_LABORATORY,
            organism = TargetOrganism.CRE_PSEUDOMONAS,
            disinfectant = DisinfectantClass.SODIUM_HYPOCHLORITE,
            surfaces = surfaces,
            labScenario = LabScenario.LIQUID_CULTURE_SPILL
        )

        assertTrue(result.violations.any { it.severity == ViolationSeverity.BSL2_LAB_WARNING })
        assertTrue(result.violations.any { it.title.contains("Spill Containment Protocol") })
    }

    @Test
    fun testAllCompliant_ReturnsCompliantStatus() {
        val surfaces = DefaultChecklists.getClinicalInpatientSurfaces()
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.CLINICAL_ENVIRONMENTAL,
            organism = TargetOrganism.MRSA_VRE,
            disinfectant = DisinfectantClass.ACCELERATED_HYDROGEN_PEROXIDE,
            surfaces = surfaces
        )

        assertEquals(AuditRiskLevel.COMPLIANT, result.riskLevel)
        assertEquals(100, result.compliancePercentage)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun testDisclaimerPresent_AlwaysContainsNotice() {
        val surfaces = DefaultChecklists.getClinicalInpatientSurfaces()
        val result = DeterministicSafetyEngine.evaluate(
            workflow = FacilityWorkflow.CLINICAL_ENVIRONMENTAL,
            organism = TargetOrganism.ROUTINE_BASELINE,
            disinfectant = DisinfectantClass.QUATERNARY_AMMONIUM,
            surfaces = surfaces
        )

        assertNotNull(result.disclaimer)
        assertTrue(result.disclaimer.contains("NOTICE: Risk category is derived deterministically"))
    }
}
