# Core Pipeline & Strategic Vision

## 1. Core Application Pipeline

The patient journey through Pre-Appointment 1 is designed to be frictionless, deterministic, and medically sound.

### Phase 1: Intake & Agent Selection
1. **App Launch:** The user opens the app to start a new follow-up period.
2. **Symptom Input:** The user describes their situation via natural text/voice or selects from a predefined catalog (e.g., "Healing Wound", "Fever Episode").
3. **Template Recommendation:** Gemini analyzes the input and recommends the most appropriate **Agent Template** from our database. 

### Phase 2: Intelligent Script Adaptation (Parameterization)
1. **Dynamic Onboarding:** Gemini engages in a brief dialog to fill the template's required parameters (e.g., "Where is the wound?", "Are there stitches?", "What times of day are best for reminders?").
2. **Template Locking:** The AI *does not* invent the medical protocol. It only adapts the parameters of a medically validated script. Once parameterized, the script is locked and synchronized with the backend.
3. **Scheduling:** The app generates local notifications and tasks (photos, check-ins, vital entries) precisely matching the locked template schedule.

### Phase 3: Daily Data Collection (The Routine)
1. **Guided Capture:** Users receive push notifications. For photos, a "ghost overlay" ensures consistent framing and angles day-over-day.
2. **Dynamic Questionnaires:** Short check-ins (pain scale, infection signs) whose phrasing adapts to the patient's previous answers, keeping the routine engaging but structurally consistent.
3. **Silent Sync:** All data, alongside automatic pulls from Health Connect (steps, sleep, heart rate), is securely synced to the user's personal backend.
4. **Daily Micro-Reports:** The Cloud Agent (Gemini 3.5 Flash) silently analyzes the day's payload (e.g., estimating wound surface from a photo) and stores a structured micro-report.

### Phase 4: End of Period & The Physician Briefing
1. **Data Aggregation:** At the end of the period, or on the day of the appointment, the backend compiles all raw data and micro-reports.
2. **Data Visualization:** A Python-based visualization module transforms time-series data into clear, professional graphs (fever curves, pain trends, healing progression charts).
3. **Final Compilation:** Gemini compiles these visuals and a photo timeline into a single, cohesive "Physician Briefing."
4. **Handoff:** The patient shares a time-limited secure link (via QR code) with the doctor, presenting a complete, objective, and beautifully formatted medical history of the period.

---

## 2. Our Competitive Advantage: The Validated Script Database

What truly sets Pre-Appointment 1 apart from generic AI health assistants is our approach to the collection protocol. 

### The Flaw of Current AI in Healthcare
Most AI health apps act as generic chatbots. They ask open-ended questions and generate dynamic, unpredictable follow-up plans. This is a liability. A dynamically generated plan cannot be peer-reviewed, cannot guarantee medical relevance, and produces inconsistent datasets that doctors cannot trust.

### Our Solution: The Medical Script Database
Our core IP and primary competitive advantage is our **growing database of Agent Templates (Scripts)**. 

*   **Co-Created with Practicing Physicians:** Our templates are not invented by AI; they are defined in collaboration with real doctors, specialists, and medical protocols.
*   **Medical Terminology & Precision:** A script strictly defines what to collect, at what cadence, and which safety thresholds (e.g., fever > 39.5°C) require immediate medical attention. 
*   **The "Doctor X" Protocol:** This allows us to offer specialized, branded follow-ups. For instance, we can offer "The Post-Op Knee Protocol recommended by Dr. Smith," giving patients confidence and doctors a format they inherently trust because they designed it.
*   **Deterministic Foundation, Intelligent Execution:** We use Gemini exactly where it shines—analyzing complex multimodal data (photos, text) and adapting conversational tone—while keeping the medical protocol itself strictly deterministic, repeatable, and safe.

This hybrid approach allows us to scale a library of highly specialized, medically sound tracking agents that competitors relying purely on generative text generation cannot match.
