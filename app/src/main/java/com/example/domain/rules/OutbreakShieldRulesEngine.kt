package com.example.domain.rules

import com.example.domain.model.AuditFinding
import com.example.domain.model.AuditInput
import com.example.domain.model.AuditResult
import com.example.domain.model.DisinfectantType
import com.example.domain.model.PathogenProfile
import com.example.domain.model.RiskLevel
import com.example.domain.model.SurfaceStatus
import com.example.domain.model.WorkflowType

/**
 * Deterministic Safety Rules Engine for OutbreakShield AI.
 *
 * CRITICAL ARCHITECTURAL CONSTRAINTS:
 * 1. Executes 100% offline with zero external network or Gemini dependencies.
 * 2. Evaluates verified CDC, EPA, OSHA, and biosafety standards deterministically.
 * 3. Never invents concentrations, dwell times, or EPA registration numbers.
 * 4. Critical deterministic findings cannot be downgraded by any downstream AI.
 * 5. Calculated risk levels represent audit compliance priorities, not clinical infection probabilities.
 */
object OutbreakShieldRulesEngine {

    fun evaluate(input: AuditInput): AuditResult {
        val findings = mutableListOf<AuditFinding>()

        // 1. DANGEROUS CHEMICAL MIXING (Highest Criticality)
        if (input.disinfectant == DisinfectantType.SODIUM_HYPOCHLORITE && input.isAcidOrAmmoniaCoPresent) {
            findings.add(
                AuditFinding(
                    ruleId = "RULE_DANGEROUS_CHEMICAL_MIXING",
                    severity = RiskLevel.CRITICAL,
                    explanation = "DANGEROUS CHEMICAL INCOMPATIBILITY: Mixing sodium hypochlorite (bleach) with acidic compounds generates toxic chlorine gas; mixing with ammonia generates toxic chloramine vapor. This is an acute respiratory hazard.",
                    nextStep = "DO NOT MIX CHEMICALS. If noxious vapors are detected, immediately ventilate and evacuate personnel. Surfaces must be thoroughly rinsed with clean water before introducing different chemical formulations.",
                    sourceReferenceNote = "OSHA Hazard Communication Standard 29 CFR 1910.1200 / Safety Data Sheets (SDS)"
                )
            )
        }

        // 2. UNKNOWN / UNLABELED DISINFECTANT
        if (input.disinfectant == DisinfectantType.UNKNOWN) {
            findings.add(
                AuditFinding(
                    ruleId = "RULE_UNKNOWN_DISINFECTANT",
                    severity = RiskLevel.HIGH,
                    explanation = "Unidentified or unlabeled disinfectant in use. Active chemical class, dilution ratio, material compatibility, and antimicrobial spectrum cannot be validated.",
                    nextStep = "Identify the product trade name and verify its EPA registration number before continuing with facility environmental release.",
                    sourceReferenceNote = "OSHA Hazard Communication Standard / EPA Disinfectant Labeling Requirements"
                )
            )
        }

        // 3. C. DIFFICILE & SPORICIDAL CLAIM VERIFICATION
        if (input.pathogen == PathogenProfile.C_DIFFICILE) {
            if (input.disinfectant == DisinfectantType.QUATERNARY_AMMONIUM) {
                // Quaternary ammonium compounds inherently lack sporicidal activity against bacterial endospores.
                // A UI switch alone CANNOT erase this critical finding without documented physical EPA List K verification on the trade product.
                val tradeName = input.productTradeName?.lowercase().orEmpty()
                val isExplicitlyVerifiedListK = input.hasVerifiedLabelClaimForPathogen &&
                        (tradeName.contains("list k") || tradeName.contains("sporicidal") || tradeName.contains("epa reg"))

                if (!isExplicitlyVerifiedListK) {
                    findings.add(
                        AuditFinding(
                            ruleId = "RULE_CDIFF_QUAT_INCOMPATIBLE",
                            severity = RiskLevel.CRITICAL,
                            explanation = "Quaternary ammonium compounds lack standard sporicidal activity against Clostridioides difficile endospores. A UI switch alone cannot validate sporicidal efficacy without physical EPA List K registration proof on the container label.",
                            nextStep = "Switch to a facility-approved EPA List K registered sporicidal disinfectant (such as certified sporicidal sodium hypochlorite or accelerated hydrogen peroxide) with confirmed label contact time.",
                            sourceReferenceNote = "EPA List K: Antimicrobial Products Effective Against C. difficile Spores / CDC Environmental Infection Control Guidelines"
                        )
                    )
                }
            } else if (!input.hasVerifiedLabelClaimForPathogen) {
                findings.add(
                    AuditFinding(
                        ruleId = "RULE_CDIFF_UNVERIFIED_CLAIM",
                        severity = RiskLevel.HIGH,
                        explanation = "Clostridioides difficile endospores require products with verified, label-stated sporicidal efficacy. The current product lacks verified sporicidal claims on file for bacterial endospores.",
                        nextStep = "Confirm product registration on EPA List K and follow certified label wet contact time.",
                        sourceReferenceNote = "EPA List K: Antimicrobial Products Effective Against C. difficile Spores"
                    )
                )
            }
        }

        // 4. NOROVIRUS VIRUCIDAL EFFICACY VERIFICATION
        if (input.pathogen == PathogenProfile.NOROVIRUS && !input.hasVerifiedLabelClaimForPathogen) {
            findings.add(
                AuditFinding(
                    ruleId = "RULE_NOROVIRUS_UNVERIFIED_CLAIM",
                    severity = RiskLevel.HIGH,
                    explanation = "Norovirus is a non-enveloped enteric virus requiring product-specific virucidal efficacy verification (e.g. EPA List G). Efficacy cannot be generalized by chemical class alone, particularly for alcohols or general-purpose quats.",
                    nextStep = "Check product EPA registration for verified norovirus virucidal claims and adhere strictly to the product label's certified wet contact time.",
                    sourceReferenceNote = "EPA List G: Registered Disinfectants Effective Against Norovirus / CDC Norovirus Infection Control Guidelines"
                )
            )
        }

        // 5. MRSA, CRE, & CANDIDA AURIS PRODUCT AND SURFACE-SPECIFIC VERIFICATION
        if (input.pathogen in listOf(PathogenProfile.MRSA, PathogenProfile.CRE, PathogenProfile.CANDIDA_AURIS) && !input.hasVerifiedLabelClaimForPathogen) {
            val severity = if (input.pathogen == PathogenProfile.CANDIDA_AURIS) RiskLevel.HIGH else RiskLevel.MODERATE
            findings.add(
                AuditFinding(
                    ruleId = "RULE_${input.pathogen.name}_UNVERIFIED_CLAIM",
                    severity = severity,
                    explanation = "Target pathogen ${input.pathogen.commonName} requires confirmed, label-stated antimicrobial efficacy (${input.pathogen.epaReferenceList}). Do not infer efficacy without verified product claims.",
                    nextStep = "Confirm that the specific product label lists efficacy against ${input.pathogen.commonName} and verify surface material compatibility.",
                    sourceReferenceNote = input.pathogen.epaReferenceList
                )
            )
        }

        // 6. UNVERIFIED OR INCORRECT CONTACT DWELL TIME
        val hasSurfaceContactDefect = input.surfaces.any { it.status == SurfaceStatus.INCORRECT_CONTACT_TIME }
        if (!input.hasVerifiedContactTimeFollowed || hasSurfaceContactDefect) {
            findings.add(
                AuditFinding(
                    ruleId = "RULE_UNVERIFIED_CONTACT_TIME",
                    severity = RiskLevel.HIGH,
                    explanation = "Unverified or truncated contact time detected. Chemical disinfectants must maintain continuous wet contact on the target surface for the exact duration certified on the manufacturer's EPA master label. Premature drying compromises microbicidal efficacy.",
                    nextStep = "Re-wet the surface with fresh disinfectant and ensure it remains undisturbed and visibly wet for the duration specified on the manufacturer label.",
                    sourceReferenceNote = "EPA Disinfectant Master Label Standards / CDC Guidelines for Disinfection and Sterilization"
                )
            )
        }

        // 7. VISIBLE ORGANIC SOIL (BIOBURDEN NEUTRALIZATION)
        val soiledSurfaces = input.surfaces.filter { it.status == SurfaceStatus.VISIBLE_SOIL }
        if (soiledSurfaces.isNotEmpty()) {
            val names = soiledSurfaces.joinToString(", ") { it.surfaceName }
            findings.add(
                AuditFinding(
                    ruleId = "RULE_VISIBLE_ORGANIC_SOIL",
                    severity = RiskLevel.MODERATE,
                    explanation = "Gross organic soil or bioburden observed on surface(s): $names. Organic matter rapidly consumes and neutralizes chemical disinfectants (particularly bleach and quats).",
                    nextStep = "Execute a two-step cleaning process per EPA master label: mechanically clean and remove gross organic soil first with a detergent/cleaner, then apply disinfectant for full wet contact time.",
                    sourceReferenceNote = "EPA Master Label Pre-Cleaning Requirement / CDC Environmental Cleaning Toolkit"
                )
            )
        }

        // 8. MISSED HIGH-TOUCH SURFACES
        val missedSurfaces = input.surfaces.filter { it.status == SurfaceStatus.MISSED_SURFACE }
        if (missedSurfaces.isNotEmpty()) {
            val names = missedSurfaces.joinToString(", ") { it.surfaceName }
            findings.add(
                AuditFinding(
                    ruleId = "RULE_MISSED_SURFACE",
                    severity = RiskLevel.HIGH,
                    explanation = "High-touch surface(s) omitted from cleaning and disinfection: $names. Omitted surfaces remain active transfer nodes for cross-transmission.",
                    nextStep = "Perform thorough terminal cleaning and disinfection on all omitted surfaces prior to releasing the room or workbench.",
                    sourceReferenceNote = "CDC Options for Evaluating Environmental Cleaning (Toolkits 1 & 2)"
                )
            )
        }

        // 9. LABORATORY CULTURE SPILL PROCEDURE ROUTING
        if (input.workflow == WorkflowType.MICROBIOLOGY_LAB && input.isCultureSpillScenario) {
            findings.add(
                AuditFinding(
                    ruleId = "RULE_LAB_SPILL_PROCEDURE",
                    severity = RiskLevel.HIGH,
                    explanation = "Live culture spill protocol active: live bacterial or fungal suspensions must never be immediately wiped dry or mopped, as mechanical agitation creates biohazard aerosols and hand contamination.",
                    nextStep = "Follow facility-approved biosafety spill SOP: restrict area access, alert laboratory personnel, don appropriate PPE (lab coat, gloves, eye protection), cover spill with absorbent towels to arrest fluid spread, saturate with facility-approved disinfectant from perimeter inward, and allow the facility-specified wet dwell time before final cleanup. Never invent universal dwell times or concentrations.",
                    sourceReferenceNote = "CDC/NIH Biosafety in Microbiological and Biomedical Laboratories (BMBL) 6th Ed. Section IV"
                )
            )

            // Critical Escalation for Aerosol, Broken Glass, or Unauthorized Personnel
            if (input.hasAerosolOrBrokenGlassHazard || !input.isPersonnelAuthorizedAndTrained) {
                findings.add(
                    AuditFinding(
                        ruleId = "RULE_LAB_ESCALATE_TO_BSO",
                        severity = RiskLevel.CRITICAL,
                        explanation = "ESCALATION REQUIRED: Biological spill involves potential aerosol generation, broken containers/glass, or personnel who lack specific training and authorization for BSL-2 spill remediation.",
                        nextStep = "DO NOT PROCEED WITH ROUTINE CLEANUP. Immediately hold breath, evacuate the room, close door, restrict entry, post biohazard spill warning signage, and contact the Institutional Biosafety Officer (BSO) or designated hazardous materials response team.",
                        sourceReferenceNote = "NIH/CDC BMBL Spill Cleanup Protocols / OSHA 29 CFR 1910.1030"
                    )
                )
            }
        }

        // 10. INCOMPLETE AUDIT DATA
        val uncheckedSurfaces = input.surfaces.filter { it.status == SurfaceStatus.NOT_CHECKED }
        if (input.surfaces.isEmpty() || uncheckedSurfaces.isNotEmpty()) {
            findings.add(
                AuditFinding(
                    ruleId = "RULE_INCOMPLETE_AUDIT",
                    severity = RiskLevel.MODERATE,
                    explanation = "Audit checklist is incomplete; one or more high-touch surfaces have not been evaluated.",
                    nextStep = "Complete visual and objective inspection for all checklist items before finalizing the audit report.",
                    sourceReferenceNote = "Quality Assurance Protocol for Environmental Services"
                )
            )
        }

        // Completeness calculation
        val total = input.surfaces.size
        val evaluated = input.surfaces.count { it.status != SurfaceStatus.NOT_CHECKED }
        val completenessScore = if (total > 0) ((evaluated.toDouble() / total.toDouble()) * 100).toInt() else 0

        // Prioritized overall risk ranking
        val overallRisk = when {
            findings.any { it.severity == RiskLevel.CRITICAL } -> RiskLevel.CRITICAL
            findings.any { it.severity == RiskLevel.HIGH } -> RiskLevel.HIGH
            findings.any { it.severity == RiskLevel.MODERATE } -> RiskLevel.MODERATE
            else -> RiskLevel.COMPLIANT
        }

        return AuditResult(
            findings = findings,
            riskLevel = overallRisk,
            completenessScore = completenessScore,
            isAiUsed = false
        )
    }
}
