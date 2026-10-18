package com.example.domain.data

import com.example.domain.model.AuditSurfaceItem
import com.example.domain.model.FacilityWorkflow
import com.example.domain.model.LabScenario
import com.example.domain.model.SurfaceCondition
import com.example.domain.model.SurfaceObservation
import com.example.domain.model.SurfaceStatus

object DefaultChecklists {

    /**
     * Standard clinical inpatient environmental surfaces for OutbreakShield AI.
     * All items initialize to NOT_CHECKED to ensure incomplete audits are never silently treated as compliant.
     */
    fun getClinicalSurfaceObservations(): List<SurfaceObservation> {
        return listOf(
            SurfaceObservation("Bed Rails & Frame Controls", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Nurse Call Button & TV Remote", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("IV Infusion Pump & Pole", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Overbed Meal Table", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Room & Bathroom Door Handles", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Sink Faucet & Dispenser Handles", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Blood Pressure Cuff & Vital Cables", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Toilet Flush Handle & Grab Bars", SurfaceStatus.NOT_CHECKED, isHighTouch = true)
        )
    }

    /**
     * Standard microbiology laboratory surfaces for OutbreakShield AI.
     * All items initialize to NOT_CHECKED to ensure incomplete audits are never silently treated as compliant.
     */
    fun getLabSurfaceObservations(): List<SurfaceObservation> {
        return listOf(
            SurfaceObservation("Biosafety Cabinet (BSC) Interior Work Surface", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("BSC Air Intake Grilles & Sash Lower Edge", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Culture Inoculation Bench & Absorbent Pad", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Specimen Receiving & Vortex Mixer Top", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Centrifuge Exterior Lid & Rotor Bucket Wells", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Micropipette Shafts & Carousel Stand", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Incubator Door Latches & Temperature Dial", SurfaceStatus.NOT_CHECKED, isHighTouch = true),
            SurfaceObservation("Lab Entry/Exit Door Handles & Push Plates", SurfaceStatus.NOT_CHECKED, isHighTouch = true)
        )
    }

    fun getClinicalInpatientSurfaces(): List<AuditSurfaceItem> {
        return listOf(
            AuditSurfaceItem(
                id = "clin_bed_rails",
                label = "Bed Rails & Frame Adjustment Buttons",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_call_button",
                label = "Nurse Call Button & TV Remote Control",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_iv_pump",
                label = "IV Infusion Pump Touchpad & Pole",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_overbed_table",
                label = "Overbed Meal Table (Top & Edges)",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_bp_cuff",
                label = "Blood Pressure Cuff & Vital Monitor Cables",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_door_handle",
                label = "Room & Bathroom Door Handles / Push Plates",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_sink_faucet",
                label = "Sink Faucet Handles & Dispenser Levers",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            ),
            AuditSurfaceItem(
                id = "clin_toilet_flush",
                label = "Toilet Flush Handle & Grab Rails",
                condition = SurfaceCondition.CLEAN_COMPLIANT,
                isHighTouchCritical = true
            )
        )
    }

    fun getLabSurfaces(scenario: LabScenario): List<AuditSurfaceItem> {
        return when (scenario) {
            LabScenario.BSL2_BIOSAFETY_CABINET -> listOf(
                AuditSurfaceItem(
                    id = "lab_bsc_work_surface",
                    label = "BSC Interior Stainless Steel Work Surface",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_bsc_grilles",
                    label = "Front & Rear Air Intake Grilles",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_bsc_sash",
                    label = "Glass Sash (Inner & Outer Wipe-down)",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = false
                ),
                AuditSurfaceItem(
                    id = "lab_bsc_vacuum_line",
                    label = "Vacuum Line / Gas Valve & In-line HEPA Filter",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_bsc_rinse_compliance",
                    label = "Post-Decontamination Alcohol / Water Rinse",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                )
            )
            LabScenario.INOCULATION_BENCH -> listOf(
                AuditSurfaceItem(
                    id = "lab_bench_mat",
                    label = "Benchtop Work Surface / Absorbent Pad",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_pipette_body",
                    label = "Micropipette Shafts & Ejector Buttons",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_vortex_cup",
                    label = "Vortex Mixer Rubber Head & Speed Dial",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = false
                ),
                AuditSurfaceItem(
                    id = "lab_timer_racks",
                    label = "Lab Timers & Test Tube Racks",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = false
                ),
                AuditSurfaceItem(
                    id = "lab_loop_sterilizer",
                    label = "Micro-incinerator / Inoculation Loop Stand Base",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = false
                )
            )
            LabScenario.LIQUID_CULTURE_SPILL -> listOf(
                AuditSurfaceItem(
                    id = "lab_spill_containment",
                    label = "Immediate Perimeter Absorption (Paper Towel Cover)",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_spill_saturation",
                    label = "Disinfectant Saturation (Perimeter to Center)",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_spill_dwell_verified",
                    label = "Verified Wet Contact Dwell Time per Biosafety SOP",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_spill_biohazard_disposal",
                    label = "Debris Disposal into Autoclave Biohazard Bag",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_spill_ppe_degown",
                    label = "PPE De-gowning & Immediate Hand Hygiene",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                )
            )
            LabScenario.CENTRIFUGE_DECONTAMINATION -> listOf(
                AuditSurfaceItem(
                    id = "lab_cent_rotor",
                    label = "Centrifuge Rotor & Bucket Wells",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_cent_aerosol_cap",
                    label = "Sealed Aerosol-Tight Bucket Caps & O-Rings",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_cent_chamber",
                    label = "Centrifuge Bowl Chamber & Lid Gasket",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                )
            )
            LabScenario.INCUBATOR_CLEANING -> listOf(
                AuditSurfaceItem(
                    id = "lab_inc_shelves",
                    label = "Incubator Copper / Stainless Shelves",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_inc_water_pan",
                    label = "Water Humidity Pan Autoclaved & Refilled",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = true
                ),
                AuditSurfaceItem(
                    id = "lab_inc_inner_glass",
                    label = "Inner Glass Door & Magnetic Outer Latch",
                    condition = SurfaceCondition.CLEAN_COMPLIANT,
                    isHighTouchCritical = false
                )
            )
        }
    }
}
