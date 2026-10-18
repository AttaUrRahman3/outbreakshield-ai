package com.example.domain.model

/**
 * Distinguishes Hospital Clinical Environmental Cleaning from Microbiology Laboratory Safety.
 * These two workflows have fundamentally different risk profiles, surface taxonomies,
 * and decontamination protocols.
 */
enum class FacilityWorkflow(val displayName: String, val description: String) {
    CLINICAL_ENVIRONMENTAL(
        displayName = "Hospital Clinical Wards",
        description = "Inpatient rooms, ICUs, and contact isolation discharge cleaning."
    ),
    MICROBIOLOGY_LABORATORY(
        displayName = "Microbiology Laboratory",
        description = "BSL-2 biosafety cabinets, inoculation benches, and culture spill containment."
    )
}

/**
 * Pathogen categories with distinct resistance profiles and EPA list mappings.
 */
enum class TargetOrganism(
    val commonName: String,
    val scientificClassification: String,
    val epaListReference: String,
    val isSporeFormer: Boolean = false,
    val isNonEnvelopedVirus: Boolean = false
) {
    CLOSTRIDIOIDES_DIFFICILE(
        commonName = "C. difficile (C. diff)",
        scientificClassification = "Bacterial Endospores",
        epaListReference = "EPA List K (Sporicides)",
        isSporeFormer = true
    ),
    NOROVIRUS(
        commonName = "Norovirus / Rotavirus",
        scientificClassification = "Non-Enveloped Virus",
        epaListReference = "EPA List G / List N (Virucides)",
        isNonEnvelopedVirus = true
    ),
    MRSA_VRE(
        commonName = "MRSA / VRE",
        scientificClassification = "Multidrug-Resistant Gram-Positive",
        epaListReference = "EPA List H (MRSA / VRE)"
    ),
    CRE_PSEUDOMONAS(
        commonName = "CRE / Pseudomonas aeruginosa",
        scientificClassification = "Multidrug-Resistant Gram-Negative",
        epaListReference = "EPA Broad-Spectrum Hospital Disinfectants"
    ),
    BLOODBORNE_PATHOGENS(
        commonName = "Bloodborne (HBV / HCV / HIV)",
        scientificClassification = "Enveloped Viruses & Bloodborne Agents",
        epaListReference = "OSHA Bloodborne Pathogens Standard / EPA List B/C/D"
    ),
    MYCOBACTERIUM_TB(
        commonName = "Mycobacterium tuberculosis (TB)",
        scientificClassification = "Acid-Fast Mycobacteria (Lipid-rich cell wall)",
        epaListReference = "EPA List E (Tuberculocides)"
    ),
    ROUTINE_BASELINE(
        commonName = "Standard Baseline (No Known Isolation)",
        scientificClassification = "General Inpatient / Lab Surfaces",
        epaListReference = "General Hospital Disinfectant"
    )
}

/**
 * Active chemical classes of hospital and laboratory surface disinfectants.
 */
enum class DisinfectantClass(
    val displayName: String,
    val typicalMechanism: String,
    val generalLimitations: String
) {
    QUATERNARY_AMMONIUM(
        displayName = "Quaternary Ammonium (Quats)",
        typicalMechanism = "Membrane disruption & surfactant action",
        generalLimitations = "Inactive against bacterial spores; variable against non-enveloped viruses; neutralized by cotton/cellulose."
    ),
    SODIUM_HYPOCHLORITE(
        displayName = "Sodium Hypochlorite (Bleach)",
        typicalMechanism = "Oxidative protein denaturation & nucleic acid destruction",
        generalLimitations = "Corrosive to metals (requires rinse on stainless steel); inactivated by heavy organic soil; pungent fumes."
    ),
    ACCELERATED_HYDROGEN_PEROXIDE(
        displayName = "Accelerated Hydrogen Peroxide (AHP)",
        typicalMechanism = "Hydroxyl radical oxidative breakdown",
        generalLimitations = "Requires verified formulation for sporicidal claims; light/heat sensitive."
    ),
    ALCOHOL_70(
        displayName = "70% Isopropanol / Ethanol",
        typicalMechanism = "Protein precipitation & membrane dissolution",
        generalLimitations = "Rapid evaporation may prevent required wet contact time; lacks sporicidal activity; flammable."
    ),
    PHENOLIC(
        displayName = "Phenolic Compounds",
        typicalMechanism = "Cell wall penetration & enzyme inactivation",
        generalLimitations = "Contraindicated in neonatal nurseries; residual odor; inactive against bacterial spores."
    ),
    PERACETIC_ACID(
        displayName = "Peracetic Acid Blends",
        typicalMechanism = "Potent oxidation of sulfhydryl and sulfur bonds",
        generalLimitations = "Pungent odor; requires proper PPE & ventilation; material compatibility check needed."
    )
}

/**
 * Surface inspection item evaluation states.
 */
enum class SurfaceCondition {
    CLEAN_COMPLIANT,
    VISIBLE_SOIL_DUST,
    MISSED_SURFACE,
    INSUFFICIENT_DWELL_TIME
}

/**
 * Specific laboratory operational context.
 */
enum class LabScenario {
    BSL2_BIOSAFETY_CABINET,
    INOCULATION_BENCH,
    CENTRIFUGE_DECONTAMINATION,
    LIQUID_CULTURE_SPILL,
    INCUBATOR_CLEANING
}

/**
 * Individual checklist surface.
 */
data class AuditSurfaceItem(
    val id: String,
    val label: String,
    val condition: SurfaceCondition = SurfaceCondition.CLEAN_COMPLIANT,
    val isHighTouchCritical: Boolean = true,
    val notes: String = ""
)

/**
 * Severity ranking of deterministic safety findings.
 */
enum class ViolationSeverity {
    CRITICAL_INCOMPATIBILITY,
    HIGH_RISK_BREACH,
    CAUTION_LABEL_DWELL,
    BSL2_LAB_WARNING
}

/**
 * Structured safety violation produced by the deterministic engine.
 */
data class SafetyViolation(
    val severity: ViolationSeverity,
    val title: String,
    val explanation: String,
    val requiredAction: String,
    val authoritativeStandard: String
)

/**
 * Overall audit risk category.
 * Note: Clearly distinguished as a checklist-derived compliance score, NOT a clinical infection probability.
 */
enum class AuditRiskLevel(val label: String) {
    CRITICAL("Critical Safety Breach"),
    HIGH("Elevated Transmission Risk"),
    MODERATE("Caution - Process Defect"),
    COMPLIANT("Standard Compliance Achieved")
}

/**
 * Complete evaluation result from the deterministic engine.
 */
data class AuditEvaluationResult(
    val workflow: FacilityWorkflow,
    val organism: TargetOrganism,
    val disinfectant: DisinfectantClass,
    val totalSurfacesChecked: Int,
    val compliantSurfacesCount: Int,
    val compliancePercentage: Int,
    val riskLevel: AuditRiskLevel,
    val violations: List<SafetyViolation>,
    val labScenario: LabScenario? = null,
    val disclaimer: String = "NOTICE: Risk category is derived deterministically from checklist completeness and EPA/CDC disinfectant compatibility matrices. This is an audit and educational tool; always follow your facility-approved infection control manual, product label, and Safety Data Sheet (SDS)."
)
