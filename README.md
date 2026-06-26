# Pre-Appointment 1

**Pre-Appointment 1 (P1)**, a mobile companion that prepares your medical appointments for you.

## The problem

When a patient finally sits in front of a doctor, most of the useful information is already gone. Symptoms that appeared three weeks ago are half-forgotten, the evolution of a wound or a rash exists only in memory, fever curves were never written down, and the few minutes of consultation are spent reconstructing history instead of making decisions.

Healthcare data is fragmented, and the most valuable part of it, what happens to the patient between two appointments, is almost never captured.

## What we are building

**Pre-Appointment 1** (the mobile application) is driven by an autonomous on-device agent that collects medically useful data in the period before a medical appointment, or during a doctor-prescribed follow-up.

Every day, the agent:

- Prompts the patient for short, targeted check-ins (symptoms, pain level, mood, sleep).
- Captures photos of evolving conditions (wounds, skin, swelling, posture) on a fixed schedule.
- Pulls available data from the phone and connected sources (steps, heart rate, sleep, temperature entries).
- Produces a daily micro-report.

These micro-reports are not shown to the patient as medical conclusions. The application never diagnoses and never alarms. Instead, all collected data is compiled, graphed over time, and summarized into a single structured briefing intended for a real physician, delivered at the time of the appointment.

The doctor opens one page and sees: what happened, when it started, how it evolved, with photos, curves and patient-reported context. Minutes of consultation time are saved, and nothing is lost.

## How data is stored

Medical data is the most sensitive data there is. We do not want it, and we do not store it.

Pre-Appointment 1 uses a self-deployed backend model: every user (or family, or clinic) deploys their own small backend instance with one command, on any cloud provider or on a self-hosted machine at home. The mobile application talks only to that personal instance.

The consequence is structural, not contractual:

- There is no central medical database to breach, sell or subpoena.
- Each user is the sole owner and legal custodian of their own medical records.
- Deleting your data means deleting your server. It is final and verifiable.

See `ARCHITECTURE.md` for the technical details of this model.

## How analysis works

Raw data alone is not a briefing. Analysis is performed by specialized cloud analysis agents: Gemini-based agents, each customized for a specific type of medical follow-up. Monitoring a healing wound does not require the same analysis as tracking a 20-day fever, post-surgery recovery, or a chronic skin condition.

Patients (or their doctors) activate the agent that matches their situation. The agent reads the data from the user's personal backend, with explicit consent and only during the follow-up period, and produces the daily micro-reports and the final physician briefing.

This is also our business model: the application is free, specialized analysis agents are purchased per follow-up period. See `manifest/business-model.md`.

## What this is not

- It is not a diagnostic tool. It never tells the patient what they have.
- It is not a replacement for a doctor. Its only output is a better-informed consultation.
- It is not a data company. We architecturally cannot access user medical data.

## Repository structure

| Path | Content |
| --- | --- |
| `README.md` | This document |
| `ARCHITECTURE.md` | Technical stack and system design |
| `manifest/` | Product principles, features, medical categories, business model |

## Status

Hackathon project for XPRIZE Gemini, category Professional Services Access. Goal: a real business operated by AI agents, with real customers and real revenue, by August 17th.

---

