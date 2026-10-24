# OutbreakShield AI 🛡️
> **Intelligent Infection Prevention & Clinical Audit Suite**  
> Built for the **NYLP AI Hackathon 2026** (Theme: Building AI-Powered Products for Real-World Impact)

[![Live Demo](https://img.shields.io/badge/Demo-Live%20App-teal?style=for-the-badge)](https://cleanroom-compass-ai.lovable.app)
[![Tech Stack](https://img.shields.io/badge/Stack-React%2019%20%7C%20TanStack%20%7C%20Tailwind-blue?style=for-the-badge)](#tech-stack)

---

## 1. Problem Statement
Hospital-Acquired Infections (HAIs) affect millions of patients annually and represent one of the leading preventable causes of mortality in healthcare environments. 

Traditional Infection Prevention & Control (IPC) auditing suffers from critical bottlenecks:
- **Paper & Static Spreadsheets:** Audits take days to digitize, delaying critical responses.
- **Delayed Risk Detection:** Recurrent sterilization gaps and high-risk vectors often go unnoticed until a full outbreak occurs.
- **Lack of Actionable Guidance:** Clinical staff receive raw scores rather than prioritized, evidence-backed corrective action steps.

---

## 2. Solution Overview
**OutbreakShield AI** is an intelligent audit and analytics platform engineered to help healthcare facilities standardize inspections, detect contamination risks in real time, and generate automated, compliance-aligned corrective actions.

By combining streamlined digital checklists with domain-specific AI coaching, OutbreakShield AI bridges the gap between routine facility inspections and proactive patient safety.

---

## 3. Core Features
- **🏥 Digital Clinical Audits:** Interactive environmental and laboratory safety checklists tailored to medical wards, ICUs, and bio-containment areas.
- **⚡ Real-Time Risk & Compliance Scoring:** Instant automated categorization of audit performance (Safe, Warning, Critical) with deficiency tracking.
- **🤖 AI Infection Prevention Coach:** Analyzes audit trails, identifies root-cause patterns (e.g., recurring hand hygiene or ventilation failures), and recommends WHO/CDC-aligned remediation plans.
- **📊 Interactive Analytics & Trends:** Circular compliance meters, risk distribution charts, and longitudinal score comparisons across departments.
- **📋 Corrective Action Workflow:** Prioritized findings tracker with status toggles (`Open`, `In Progress`, `Resolved`) to close inspection gaps.

---

## 4. AI Methodology & System Design
- **Analytical Reasoning:** Ingests live checklist answers to compute weighted deficiency scores and detect non-compliance anomalies.
- **Contextual Clinical Assistant:** Leverages a guided IPC knowledge layer that contextualizes audit data against infection control standards, helping audit teams quickly draft escalation protocols.
- **Zero-Latency In-Browser Architecture:** Works offline-capable in clinical environments using client-side state persistence.

---

## 5. Tech Stack
- **Framework & Routing:** React 19, TanStack Start, TanStack Router
- **Styling & Design System:** Tailwind CSS v4, Lucide Icons
- **Data Visualization:** Recharts
- **State & Storage:** Reactive client-side local cache
- **Quality & Testing:** Vitest, Testing Library

---

## 6. Setup & Local Development

### Prerequisites
- Node.js 20+ installed
- npm or bun

### Installation
```bash
# Clone the repository
git clone https://github.com/<your-username>/<your-repo-name>.git

# Navigate into the project folder
cd <your-repo-name>

# Install dependencies
npm install

# Start local dev server
npm run dev