package com.example.domain.rules

import com.example.domain.model.AuditEvaluationResult
import com.example.domain.model.AuditRiskLevel
import com.example.domain.model.AuditSurfaceItem
import com.example.domain.model.DisinfectantClass
import com.example.domain.model.FacilityWorkflow
import com.example.domain.model.LabScenario
import com.example.domain.model.SafetyViolation
import com.example.domain.model.SurfaceCondition
import com.example.domain.model.TargetOrganism
import com.example.domain.model.ViolationSeverity

/**
 * On-device deterministic safety engine.
 *
 * CRITICAL SAFETY PRINCIPLE:
 * This engine executes synchronously in 0 ms and enforces verified CDC, EPA, and biosafety guidelines.
 * AI models (Gemini) can provide educational explanation and contextual coaching,
 * but CANNOT override or weaken the deterministic rules defined here.
 */
object DeterministicSafetyEngine {

    fun evaluate(
        workflow: FacilityWorkflow,
        organism: TargetOrganism,
        disinfectant: DisinfectantClass,
        surfaces: List<AuditSurfaceItem>,
        labScenario: LabScenario? = null
    ): AuditEvaluationResult {
        val violations = mutableListOf<SafetyViolation>()

        // 1. Spore-Forming Pathogen Incompatibility (EPA List K)
        if (organism.isSporeFormer) {
            if (disinfectant == DisinfectantClass.QUATERNARY_AMMONIUM) {
                violations.add(
                    SafetyViolation(
                        severity = ViolationSeverity.CRITICAL_INCOMPATIBILITY,
                        title = "Critical Incompatibility: Quats vs. Bacterial Endospores",
                        explanation = "Quaternary ammonium compounds lack sporicidal activity against ${organism.commonName} endospores. Using quats leaves infectious spores viable on surfaces.",
                        requiredAction = "Immediately switch to a facility-approved EPA List K registered sporicidal agent (such as sodium hypochlorite or an EPA-registered sporicidal accelerated hydrogen peroxide). Verify the specific label contact time.",
                        authoritativeStandard = "EPA List K: Antimicrobial Products Effective Against C. difficile Spores / CDC Guideline for Disinfection and Sterilization"
                    )
                )
            } else if (disinfectant == DisinfectantClass.ALCOHOL_70) {
                violations.add(
                    SafetyViolation(
                        severity = ViolationSeverity.CRITICAL_INCOMPATIBILITY,
                        title = "Critical Incompatibility: Alcohol vs. Bacterial Endospores",
                        explanation = "70% alcohols do not destroy bacterial endospores. Spores survive alcohol exposure intact.",
                        requiredAction = "Select a certified sporicidal disinfectant from the facility EPA List K inventory and adhere to the certified wet contact time on the product label.",
                        authoritativeStandard = "CDC HICPAC Guidelines / WHO Clean Care is Safer Care"
                    )
                )
            }
        }

        // 2. Non-Enveloped Enteric Virus Incompatibility (EPA List G/N)
        if (organism.isNonEnvelopedVirus) {
            if (disinfectant == DisinfectantClass.ALCOHOL_70) {
                violations.add(
                    SafetyViolation(
                        severity = ViolationSeverity.HIGH_RISK_BREACH,
                        title = "Efficacy Warning: 70% Alcohol vs. Non-Enveloped Viruses",
                        explanation = "Alcohols exhibit suboptimal, variable efficacy against non-enveloped enteric viruses (${organism.commonName}). Alcohol rapidly evaporates before required virucidal dwell times can be reached.",
                        requiredAction = "Utilize an EPA-registered disinfectant with specific virucidal claims for norovirus (refer to EPA List G or facility protocol) and maintain full wet contact time.",
                        authoritativeStandard = "EPA List G: Registered Disinfectants Effective Against Norovirus / CDC Norovirus Guidance"
                    )
                )
            }
        }

        // 3. Mycobacteria / Acid-Fast Incompatibility
        if (organism == TargetOrganism.MYCOBACTERIUM_TB) {
            if (disinfectant == DisinfectantClass.QUATERNARY_AMMONIUM) {
                violations.add(
                    SafetyViolation(
                        severity = ViolationSeverity.HIGH_RISK_BREACH,
                        title = "Efficacy Gap: Quat vs. Mycobacterium tuberculosis",
                        explanation = "Standard hospital quaternary ammonium compounds are generally not certified tuberculocides due to the thick, lipid-rich mycolic acid cell wall of TB.",
                        requiredAction = "Ensure the product is specifically EPA List E registered with a verified tuberculocidal claim, or use an intermediate-level hospital disinfectant (e.g., sodium hypochlorite or accelerated hydrogen peroxide).",
                        authoritativeStandard = "EPA List E: Registered Antimicrobial Products with Tuberculocidal Claims"
                    )
                )
            }
        }

        // 4. Visible Soil Load & Bioburden Protocol
        val soiledSurfaces = surfaces.filter { it.condition == SurfaceCondition.VISIBLE_SOIL_DUST }
        if (soiledSurfaces.isNotEmpty()) {
            val surfaceNames = soiledSurfaces.take(3).joinToString(", ") { it.label }
            violations.add(
                SafetyViolation(
                    severity = ViolationSeverity.CAUTION_LABEL_DWELL,
                    title = "Pre-Cleaning Required: Organic Soil Observed",
                    explanation = "Visible soil, dust, or organic matter detected on: $surfaceNames. Organic bioburden rapidly consumes and neutralizes chemical disinfectants (particularly bleach and quats).",
                    requiredAction = "Implement a two-step cleaning protocol per product master label: first perform gross mechanical cleaning with a detergent/cleaner, followed by fresh disinfectant application for full label dwell time.",
                    authoritativeStandard = "EPA Product Master Label Standard / CDC Guidelines for Environmental Cleaning"
                )
            )
        }

        // 5. Premature Drying / Insufficient Dwell Time
        val insufficientDwellSurfaces = surfaces.filter { it.condition == SurfaceCondition.INSUFFICIENT_DWELL_TIME }
        if (insufficientDwellSurfaces.isNotEmpty()) {
            val surfaceNames = insufficientDwellSurfaces.take(3).joinToString(", ") { it.label }
            violations.add(
                SafetyViolation(
                    severity = ViolationSeverity.HIGH_RISK_BREACH,
                    title = "Insufficient Dwell Time: Premature Drying",
                    explanation = "Surfaces dried before the EPA label contact time elapsed on: $surfaceNames. Pathogens survive when wet contact time is truncated.",
                    requiredAction = "Reapply disinfectant to maintain continuous surface wetness for the entire duration specified on the manufacturer's EPA label. Do not wipe dry before dwell completes.",
                    authoritativeStandard = "EPA Disinfectant Master Label Registration Requirements"
                )
            )
        }

        // 6. Missed Critical Surfaces
        val missedCritical = surfaces.filter { it.condition == SurfaceCondition.MISSED_SURFACE && it.isHighTouchCritical }
        if (missedCritical.isNotEmpty()) {
            val surfaceNames = missedCritical.take(3).joinToString(", ") { it.label }
            violations.add(
                SafetyViolation(
                    severity = ViolationSeverity.HIGH_RISK_BREACH,
                    title = "Omission: High-Touch Surface Missed",
                    explanation = "Critical high-touch vectors remained uncleaned: $surfaceNames. These surfaces are primary transfer nodes for cross-transmission.",
                    requiredAction = "Immediately complete terminal decontamination of these missed surfaces prior to room release or bench utilization.",
                    authoritativeStandard = "CDC Options for Evaluating Environmental Cleaning (Toolkits 1 & 2)"
                )
            )
        }

        // 7. Laboratory-Specific Workflow Checks
        if (workflow == FacilityWorkflow.MICROBIOLOGY_LABORATORY && labScenario != null) {
            when (labScenario) {
                LabScenario.LIQUID_CULTURE_SPILL -> {
                    violations.add(
                        SafetyViolation(
                            severity = ViolationSeverity.BSL2_LAB_WARNING,
                            title = "BSL-2 Spill Containment Protocol",
                            explanation = "Liquid microbial culture spills present significant aerosolization and hand contact hazards. Never dry-wipe or mop immediately.",
                            requiredAction = "Alert co-workers, don appropriate PPE (gloves, lab coat, eye protection), cover spill with absorbent towels to arrest spreading, gently saturate with facility-approved disinfectant from edge inward, and observe the specific biosafety SOP wet dwell time (typically 15-20 min depending on agent) before cleanup.",
                            authoritativeStandard = "CDC/NIH Biosafety in Microbiological and Biomedical Laboratories (BMBL) 6th Ed."
                        )
                    )
                }
                LabScenario.BSL2_BIOSAFETY_CABINET -> {
                    if (disinfectant == DisinfectantClass.SODIUM_HYPOCHLORITE) {
                        violations.add(
                            SafetyViolation(
                                severity = ViolationSeverity.CAUTION_LABEL_DWELL,
                                title = "Corrosion Alert: Bleach on Stainless Steel BSC",
                                explanation = "Sodium hypochlorite residues cause pitting corrosion on 304/316 stainless steel interiors and exhaust air intake grilles.",
                                requiredAction = "After completing hypochlorite decontamination contact time, follow with a thorough rinse using sterile 70% isopropanol/ethanol or sterile water to remove corrosive chlorine salts.",
                                authoritativeStandard = "NSF/ANSI Standard 49 - Biosafety Cabinetry Design and Decontamination"
                            )
                        )
                    }
                }
                LabScenario.CENTRIFUGE_DECONTAMINATION -> {
                    violations.add(
                        SafetyViolation(
                            severity = ViolationSeverity.BSL2_LAB_WARNING,
                            title = "Centrifuge Aerosol Hazard Check",
                            explanation = "Centrifuge tube breakage can create concentrated, high-velocity aerosols inside sealed buckets or rotor bowls.",
                            requiredAction = "If breakage is suspected, allow sealed buckets to stand for 30 minutes to permit aerosol settling before opening inside a Biosafety Cabinet. Disinfect rotor and cups with a non-corrosive disinfectant according to centrifuge manufacturer specifications.",
                            authoritativeStandard = "CDC/NIH BMBL Centrifuge Safety Standards"
                        )
                    )
                }
                else -> { /* standard lab bench checks */ }
            }
        }

        // Calculate compliance metrics
        val total = surfaces.size.coerceAtLeast(1)
        val compliantCount = surfaces.count { it.condition == SurfaceCondition.CLEAN_COMPLIANT }
        val percentage = ((compliantCount.toDouble() / total.toDouble()) * 100).toInt()

        // Determine derived Risk Level
        val riskLevel = when {
            violations.any { it.severity == ViolationSeverity.CRITICAL_INCOMPATIBILITY } -> AuditRiskLevel.CRITICAL
            violations.any { it.severity == ViolationSeverity.HIGH_RISK_BREACH } -> AuditRiskLevel.HIGH
            violations.any { it.severity == ViolationSeverity.BSL2_LAB_WARNING } -> AuditRiskLevel.HIGH
            percentage < 80 || violations.isNotEmpty() -> AuditRiskLevel.MODERATE
            else -> AuditRiskLevel.COMPLIANT
        }

        return AuditEvaluationResult(
            workflow = workflow,
            organism = organism,
            disinfectant = disinfectant,
            totalSurfacesChecked = total,
            compliantSurfacesCount = compliantCount,
            compliancePercentage = percentage,
            riskLevel = riskLevel,
            violations = violations,
            labScenario = labScenario
        )
    }
}
