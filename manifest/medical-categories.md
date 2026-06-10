# Medical categories and analysis agents

Each analysis agent targets one follow-up situation, not one disease. The unit of specialization is "what must be observed over time before this consultation", because that is what defines the collection plan and the briefing.

Categories are ordered by hackathon priority. Priority is driven by: visual observability (photos carry signal), short natural follow-up windows (fits the hackathon timeline), low regulatory sensitivity, and willingness to pay.

## Tier 1 - Launch agents (built during the hackathon)

### 1. Wound and injury monitoring

Cuts, post-stitches, burns, ulcers, pressure sores, insect bites that worsen.

- Collected: daily standardized photo, pain score, redness/discharge/odor check-ins, temperature if infection is suspected.
- Analysis: wound surface and color evolution from photos, healing trajectory, infection warning signs timeline.
- Typical duration: 7 to 21 days. Strong photo signal, ideal first agent.

### 2. Dermatology follow-up

Rashes, eczema flares, psoriasis, acne treatments, suspicious moles awaiting a dermatologist slot.

- Collected: daily photos of affected zones with framing overlay, itch/pain scores, trigger diary (food, products, stress), treatment adherence.
- Analysis: lesion extent and color trends, flare-trigger correlation, photo timeline for the dermatologist.
- Typical duration: 14 to 60 days. Dermatologist wait times make the pre-consultation story strongest here.

### 3. Fever and infection episodes

Prolonged or recurring fever, post-travel fever, children's fever episodes.

- Collected: temperature entries at fixed hours, symptom check-ins (chills, sweat, appetite, pain locations), hydration and medication log, heart rate from health platform.
- Analysis: fever curve with periodicity detection, symptom co-occurrence timeline, medication-response pattern.
- Typical duration: 5 to 20 days.

### 4. Musculoskeletal injury and recovery

Sprains, muscle tears, tendinitis, back pain episodes, post-fracture monitoring.

- Collected: photos of swelling/bruising, pain scores by movement type, range-of-motion guided self-tests on video, activity load from step data.
- Analysis: swelling regression, pain-versus-load curves, mobility progression.
- Typical duration: 14 to 45 days.

## Tier 2 - Fast follow (templates ready, launched as demand appears)

### 5. Post-surgery recovery

Scar monitoring, pain trajectory, mobility milestones, medication adherence after an operation.

### 6. Chronic condition pre-consultation

The quarterly specialist visit for asthma, diabetes, hypertension, IBS: 30 days of structured data instead of "how have you been since last time".

### 7. Medication change observation

A new treatment or dosage change: side-effect diary, symptom evolution, adherence, vitals where relevant.

### 8. Pediatric episode tracking

Parent-operated follow-ups: growth concerns, recurring ENT episodes, feeding issues. Same engine, parent-friendly check-ins.

## Tier 3 - Requires extra care (post-hackathon, with medical advisors)

### 9. Mental health pre-consultation

Mood, sleep, energy and anxiety journaling before a psychiatry or therapy appointment. High value, but check-in wording and crisis pathways require professional review.

### 10. Pregnancy companion follow-ups

Symptom and vitals tracking between prenatal visits. High engagement, high sensitivity.

### 11. Geriatric and caregiver monitoring

Operated by a family caregiver: falls, confusion episodes, appetite, weight. Multi-stakeholder consent model needed.

## What is deliberately excluded

- Anything requiring an emergency response loop (chest pain, stroke symptoms). The app is not an emergency channel and says so.
- Oncology treatment monitoring and other follow-ups where AI-summarized observations could influence critical care decisions without validation.
- Any agent whose briefing a physician could mistake for a lab result or diagnostic output.

## Agent definition checklist

Every agent, in every tier, is defined by the same five artifacts, which is what keeps the catalog scalable:

1. Collection plan template (streams, cadence, photo protocols, questionnaires).
2. Specialization post-prompt for Gemini (domain context, what to observe, what matters).
3. Micro-report schema (structured daily output).
4. Briefing template (the physician-facing document).
5. Safety sheet (emergency thresholds that trigger the generic "seek care now" message, excluded interpretations).
