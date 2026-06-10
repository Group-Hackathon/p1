# Principles

These principles are constraints, not aspirations. A feature that violates one of them does not ship.

## 1. The patient is the courier, the doctor is the reader

The application collects, organizes and summarizes. It never interprets for the patient. Every output that carries medical meaning, micro-reports, trend analyses, the final briefing, is addressed to a physician and formatted for a physician. The patient sees what was collected and that it is being prepared, never "what it means".

Rationale: showing partial medical interpretations to patients creates anxiety, self-diagnosis, and regulatory exposure as a medical device. Addressing a professional keeps the product in the documentation and decision-support space.

## 2. No diagnosis, ever

The system describes and quantifies: "the wound surface decreased 40 percent between day 3 and day 12", "fever episodes clustered in the evening". It never names a disease, never suggests a treatment, never tells the user whether to worry. The single safety exception: if collected values cross universally accepted emergency thresholds, the app displays a generic "seek medical care now" message with no further interpretation.

## 3. We never hold medical data

There is no central database of patient records, by architecture. Every user runs their own backend instance and is the sole owner of their data. Analysis agents receive scoped, time-limited, consented access and retain nothing. If our company disappears tomorrow, every user still has their complete medical record on their own server.

## 4. Consent is per follow-up, not per account

Activating a follow-up is the consent event. It states exactly which data streams will be collected, at what cadence, which agent will read them, and when access expires. Outside an active follow-up, the agent collects nothing and the cloud reads nothing. Revocation is immediate and one tap away.

## 5. Collection must be nearly effortless

The target user is unwell, busy, or both. Daily interaction must fit in under two minutes: one guided photo, a few taps, done. Everything that can be pulled automatically (sensor data, health platform data) is pulled automatically. If the daily routine feels like a chore, the data stops, and the product is dead.

## 6. Comparable data over rich data

A blurry photo taken from a different angle every day is worth less than a mediocre photo taken identically every day. The agent enforces consistency: framing overlays, same distance, same lighting hints, same questions at the same hour. Longitudinal comparability is the core value of the dataset.

## 7. Specialization over generality

There is no general "health AI" in this product. Each analysis agent is narrowly specialized for one follow-up type, with its own collection plan, its own prompts, its own briefing format. Narrow agents are easier to make good, easier to validate, easier to explain to a doctor, and easier to sell.

## 8. The doctor's time is the metric

The product succeeds if a physician reads the briefing in under two minutes and knows more than they would have learned in ten minutes of anamnesis. Every design decision in the briefing format optimizes for that reading.
