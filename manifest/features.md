# Features

## Core loop

1. The user has an upcoming medical appointment, or a doctor prescribes a monitoring period.
2. The user activates the matching analysis agent (for example "Wound healing, 14 days").
3. The on-device agent executes the collection plan daily.
4. The cloud agent produces a daily micro-report, stored on the user's backend, not surfaced to the user.
5. On appointment day, the physician briefing is ready and shared with the doctor in one tap.

## Mobile application

### Follow-up activation

- Catalog of available analysis agents, grouped by medical category.
- Each agent shows: what will be collected, cadence, duration, price, and a sample briefing.
- Activation creates the follow-up plan and the consent record, and schedules all collection tasks.

### Daily guided collection

- Push notification at planned times; the whole daily routine takes under two minutes.
- Guided photo capture: ghost overlay of the previous photo for identical framing, distance and angle hints, automatic quality check (blur, lighting) with immediate retake prompt.
- Micro check-ins: 3 to 6 questions with tap answers (pain scale, symptom presence, sleep quality). Question phrasing adapts to previous answers via on-device model; static fallback.
- Free-text or voice note option for anything the user wants the doctor to know.

### Automatic collection

- Health platform pull (Health Connect on Android): steps, heart rate, sleep, weight, temperature entries where available.
- Manual vitals entry with reminder scheduling (temperature, blood pressure if the user has a device).

### Patient-facing screens

- Collection status: what was captured today, completeness streak, days remaining.
- The data itself is browsable (it is the user's data) but presented as records, never with interpretation.
- Follow-up controls: pause, revoke consent, delete everything.

### Briefing delivery

- On appointment day: time-limited read-only share link and QR code, opened by the doctor on any device, served from the user's own backend.
- PDF export as a fallback for doctors who want a file.

## Personal backend

- One-command self-deployment (Docker Compose) on any VPS or self-hosted machine.
- QR-code pairing with the mobile app.
- Stores all raw data, micro-reports and briefings; client-side encrypted photos.
- Issues and revokes scoped access tokens for analysis agents.
- Export everything as a single archive at any time.

## Cloud analysis agents

- One agent per follow-up type (see `medical-categories.md`).
- Daily run: read new data, multimodal Gemini analysis, write micro-report back.
- Micro-report contents: structured observations, extracted measurements (for example wound surface from photos), flags on notable changes, data-quality notes.
- End of period: compiled time-series graphs (fever curve, pain trend, measured healing progression), photo timeline, and the physician briefing.

### The physician briefing

One document, readable in under two minutes:

- Header: follow-up type, period, data completeness score.
- Timeline of key events as reported and as detected.
- Graphs: every quantitative stream plotted over the full period.
- Photo strip: comparable photos at regular intervals, with measured deltas.
- Patient-reported context, summarized.
- Explicit methodology note: what was collected, how, and what the AI did and did not do.

## Out of scope for the hackathon

- iOS application (architecture stays iOS-ready).
- Doctor-side accounts and dashboards.
- Wearable integrations beyond what Health Connect exposes.
- Any form of diagnosis, triage or treatment suggestion. Permanently out of scope.
