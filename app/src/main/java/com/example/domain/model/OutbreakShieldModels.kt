package com.example.domain.model

/**
 * High-level workflow separation between healthcare environmental hygiene and laboratory safety.
 */
enum class WorkflowType {
    HOSPITAL_CLEANING,
    MICROBIOLOGY_LAB
}

/**
 * Epidemiologically significant target pathogens and healthcare-associated infection (HAI) agents.
 */
enum class PathogenProfile(
    val commonName: String,
    val epaReferenceList: String
) {
    C_DIFFICILE("Clostridioides difficile (Endospores)", "EPA List K"),
    MRSA("Methicillin-Resistant Staphylococcus aureus", "EPA List H"),
    CRE("Carbapenem-Resistant Enterobacterales", "EPA Broad-Spectrum Hospital Disinfectant"),
    NOROVIRUS("Norovirus (Non-Enveloped Virus)", "EPA List G"),
    CANDIDA_AURIS("Candida auris (Fungal Pathogen)", "EPA List P"),
    ROUTINE("Routine Baseline (Standard Precautions)", "General Hospital Disinfectant")
}

/**
 * Common disinfectant active chemical classes.
 */
enum class DisinfectantType(val label: String) {
    QUATERNARY_AMMONIUM("Quaternary Ammonium Compound (Quat)"),
    SODIUM_HYPOCHLORITE("Sodium Hypochlorite (Bleach)"),
    ACCELERATED_HYDROGEN_PEROXIDE("Accelerated Hydrogen Peroxide (AHP)"),
    ALCOHOL("70% Alcohol (Isopropanol / Ethanol)"),
    OTHER("Other Chemical Class"),
    UNKNOWN("Unknown / Unlabeled Product")
}

/**
 * Physical inspection condition of audited surfaces.
 */
enum class SurfaceStatus {
    CLEAN_AND_DISINFECTED,
    VISIBLE_SOIL,
    MISSED_SURFACE,
    INCORRECT_CONTACT_TIME,
    NOT_CHECKED
}

/**
 * Prioritized risk rating for audit findings and overall audit outcome.
 * These reflect audit compliance priorities and safety breaches, NOT clinical infection probabilities.
 */
enum class RiskLevel {
    COMPLIANT,
    MODERATE,
    HIGH,
    CRITICAL
}

/**
 * Atomic audit finding produced deterministically by the rules engine.
 */
data class AuditFinding(
    val ruleId: String,
    val severity: RiskLevel,
    val explanation: String,
    val nextStep: String,
    val sourceReferenceNote: String
)

/**
 * Surface observation record.
 */
data class SurfaceObservation(
    val surfaceName: String,
    val status: SurfaceStatus,
    val isHighTouch: Boolean = true,
    val notes: String? = null
)

/**
 * Complete input payload evaluated by the deterministic engine.
 */
data class AuditInput(
    val workflow: WorkflowType,
    val pathogen: PathogenProfile,
    val disinfectant: DisinfectantType,
    val productTradeName: String? = null,
    val hasVerifiedLabelClaimForPathogen: Boolean = false,
    val hasVerifiedContactTimeFollowed: Boolean = false,
    val surfaces: List<SurfaceObservation> = emptyList(),
    val isCultureSpillScenario: Boolean = false,
    val isAcidOrAmmoniaCoPresent: Boolean = false,
    val hasAerosolOrBrokenGlassHazard: Boolean = false,
    val isPersonnelAuthorizedAndTrained: Boolean = true
)

/**
 * Complete audit output produced independently of UI and AI.
 */
data class AuditResult(
    val findings: List<AuditFinding>,
    val riskLevel: RiskLevel,
    val completenessScore: Int,
    val isAiUsed: Boolean = false,
    val disclaimer: String = "NOTICE: Audit risk ratings reflect environmental checklist compliance and chemical compatibility rules, NOT clinical infection probabilities. Always adhere to facility infection control policies, product master labels, and Safety Data Sheets (SDS)."
)
