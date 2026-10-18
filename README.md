# OutbreakShield AI

Offline-First Infection Prevention & Environmental Hygiene Audit Assistant for Healthcare Facilities and Microbiology Laboratories.

Developed for the **NYLP AI Hackathon 2026** under the theme **"Build AI for Real-World Impact"**.

---

## 1. Problem Statement
Hospital-Acquired Infections (HAIs) and laboratory-acquired exposures cause preventable morbidity, mortality, and severe institutional burdens worldwide. Pathogens like *Clostridioides difficile*, *Candida auris*, MRSA, and Norovirus require specific, non-interchangeable chemical disinfectants and strict wet contact times. In fast-paced healthcare settings:
- Cleaning staff often use quaternary ammonium compounds on *C. difficile* spores, falsely believing they are effective.
- Incompatibilities (e.g., sodium hypochlorite mixed with acids/ammonia creating toxic chloramine gases) pose acute safety hazards.
- Spills in microbiology laboratories risk aerosol transmission if not escalated immediately to a trained Biosafety Officer (BSO).
- Cloud-only AI tools fail during network outages or in shielded hospital basements and biocontainment suites.

## 2. Solution Overview
**OutbreakShield AI** is an offline-first, mobile Android audit assistant that enforces **deterministic, uncompromised safety rules** alongside **assistive, sanitized AI coaching**:
1. **Deterministic Safety Engine:** Runs 100% locally on-device. Evaluates pathogen-disinfectant compatibility (e.g., EPA List K validation for *C. difficile*), wet contact time adherence, chemical mixing hazard detection, and laboratory spill escalation.
2. **Dual Clinical & Biosafety Workflows:**
   - **Hospital Clinical Wards:** Room-by-room terminal cleaning audits with high-touch surface checklists (bed rails, IV pumps, call buttons, etc.).
   - **Microbiology Laboratory:** Biohazard workflows covering Biosafety Cabinets (BSC), inoculation benches, centrifuge touchpoints, and spill containment protocols.
3. **Safe Audit Reports & Handover:** Monospace-formatted logs detailing verified vs. pending surfaces, deterministic findings, and corrective instructions with Android share-sheet & clipboard integration.
4. **Offline AI Coaching & Debrief:** Generates non-hallucinatory staff debriefing scripts from verified findings without exposing patient or confidential data.

---

## 3. Key Features
- **Deterministic Rules Evaluation:** Instant safety alerts (Compliant, Moderate, High, Critical) with zero network latency.
- **Strict High-Touch Checklist:** Unchecked surfaces are never marked compliant; preserves true completion percentage.
- **Biosafety Escalation:** Automatically routes biohazards, aerosol spills, or untrained incidents to institutional Biosafety Officers.
- **Chemical Safety Matrix:** Real-time warnings against mixing bleach with acids or ammonia.
- **Complete Offline Resilience:** Functions seamlessly in basements, negative-pressure rooms, and disconnected environments.

---

## 4. Tech Stack
- **Platform:** Native Android (Target SDK 36, Min SDK 24)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material Design 3 (Dynamic Color, Edge-to-Edge)
- **Architecture:** Clean Architecture / MVVM with immutable state flows
- **Testing:** JUnit 4 & Robolectric JVM testing for rules engine and UI workflows

---

## 5. AI / API Usage & Safety Architecture
- **Assisted AI Role:** Advisory coaching and staff debrief generation based exclusively on sanitized deterministic findings.
- **Zero-Trust Safety Rule:** AI commentary can **never** override, suppress, downgrade, or contradict deterministic infection control findings.
- **Privacy & Sanitization:** Zero PHI (Protected Health Information), staff names, or patient data are sent to language models.
- **Graceful Fallback:** If live AI services are unconfigured or offline, the app employs an on-device rule-based fallback debrief.

---

## 6. Setup & Build Instructions
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd outbreakshield
   ```
2. **Open in Android Studio:**
   - Android Studio Ladybug | 2024.2.1 or newer recommended.
   - Gradle will automatically synchronize dependencies.
3. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 7. Known Limitations & Future Roadmap
- Computer vision integration for automated chemical label reading (planned via on-device CameraX and ML Kit).
- Barcode/QR asset scanning for hospital room inventory tracking.
- Multi-language support for diverse environmental services staff.

---

## 8. Hackathon Submission Information
- **Event:** NYLP AI Hackathon 2026
- **Category:** Healthcare & Environmental Safety
- **Core Principle:** Safety-First AI — Combining deterministic biomedical logic with generative coaching.
