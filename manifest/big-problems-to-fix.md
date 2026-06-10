# Big problems to fix

An honest list of the structural weaknesses of the project as currently defined. These are not polish items; each one can kill the product if ignored. They are ordered by severity.

Context for all points below: the product will launch first in jurisdictions with lighter health-data and medical-software regulation, and self-hosting is an option, not the default. The default offering is a managed, secured, per-user private hosting instance operated by us, with everything that implies.

## 1. The target user and the privacy architecture pull in opposite directions

The self-deploy model works for technical, sovereignty-motivated users. Our declared target user is the exact opposite: sick, busy, non-technical, often a parent in the middle of a stressful episode.

Consequence already accepted: most users will choose the managed private hosting option. This means we de facto host medical data, even if each instance is isolated per user. The "we never hold medical data" claim must therefore be rewritten honestly:

- Self-hosting remains available and fully supported: it is the proof that the architecture is user-owned, and the option for those who want zero trust in us.
- The managed offering must carry its real obligations: per-user isolation, client-side encryption where possible, breach liability, data processing agreements with our cloud and model providers, and a clear exit path (one-tap export, one-tap migration to self-hosted).
- Marketing must never say "we cannot access your data" about the managed tier if that is not technically true. Overclaiming privacy is worse than not claiming it.

To do: define precisely what the managed tier guarantees (encryption boundaries, who holds keys, what staff can access), and write it down before the first user signs up.

## 2. "No diagnosis" probably does not exempt us from medical device regulation everywhere

Software that produces a document intended to inform a clinical decision, which is the physician briefing by definition, can qualify as a medical device in strict jurisdictions (EU MDR being the canonical example), even when it never names a disease. "We describe, we do not diagnose" is a product distinction, not automatically a regulatory one.

Mitigation chosen: launch first in markets with lighter or clearer rules for wellness and documentation software, and treat strict markets (EU, and US FDA-regulated claims) as a later phase requiring proper regulatory review.

To do:

- Select the initial launch markets explicitly and verify the qualification rules for each before launch, not after.
- Keep the product squarely in the "structured patient diary plus summarization" category: no scores that resemble clinical indices, no risk levels, no urgency grading beyond the generic safety message.
- Keep a written record of these design choices; it is the regulatory file we will need later.

## 3. We promise quantitative measurements we cannot deliver

"Wound surface in mm2" estimated from uncalibrated smartphone photos is false precision. The first serious physician will dismantle it, and our credibility with it.

To do:

- Replace absolute measurements with relative evolution ("estimated surface change versus day 1"), clearly labeled as estimates.
- Add an optional calibration marker to the photo protocol (any flat reference object of known size in frame) for users who want absolute numbers.
- The briefing methodology note must state measurement uncertainty explicitly.

## 4. The patient pays for a product that is invisible to them

Fourteen days of daily effort, zero feedback, and the value is delivered to someone else (the doctor) at the very end. Regulatorily virtuous, catastrophic for retention and perceived value.

To do: give the patient a visible, non-medical reward layer. Collection completeness, photo quality streaks, "your file is 80 percent ready for your appointment", a preview of the briefing structure (not its content). The patient must feel the file getting stronger every day without receiving any interpretation.

## 5. Consumer revenue within the hackathon window is unrealistic

B2C health acquisition in ten weeks, through an app store review process for a health app that takes medical photos, on a product whose value cycle takes 14 days to complete once, does not produce real revenue by August 17.

To do:

- Sell through prescribers, not consumers: physiotherapists, sports clubs, home wound-care nurses, post-op clinics. One prescriber brings ten patients and provides the validation feedback loop.
- Invoice prescribers manually at first; defer in-app payments entirely.
- Treat each delivered briefing as the demo artifact for the next sale.

## 6. The scope must be amputated, not prioritized

The currently documented project (KMP app with camera, permissions, background jobs, health platform integration; self-deployable backend; agent runtime; payments; four launch agents) is a nine-month project wearing a hackathon badge.

Hackathon scope, decided:

- Keep: the Android app (KMP structure), one template (wound monitoring), the daily collection loop, the cloud analysis agent, the physician briefing with share link.
- Cut from the hackathon, keep in the architecture: the self-deploy flow (managed hosting only at first, self-hosting ships post-hackathon), Health Connect integration, in-app payments, the dermatology, fever and musculoskeletal agents.
- The repository documentation may describe the full vision, but the build plan must only contain the kept list above.

## 7. The doctor was designed for, but never consulted

The briefing format optimizes for a reader we have not interviewed. Doctors live inside their practice software; an external QR link is friction, and a document whose reliability they cannot assess will be treated as patient-reported data at best.

To do:

- Before freezing the briefing format, put a draft in front of at least three practicing clinicians and iterate on their reading, not our assumptions.
- Position the briefing honestly as structured patient-reported data, enhanced and organized, never as clinical measurements.
- EHR or national health-record integrations are out of scope, but a clean printable PDF is mandatory because that is what survives every workflow.
