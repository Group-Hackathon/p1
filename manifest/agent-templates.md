# Agent templates: fixed plans, parameterized by AI

## The principle

A design question every system like this must answer: when a user starts a follow-up, who decides what gets collected, the AI or a template?

Our answer is deliberate: **the collection plan is always a fixed, versioned, human-written template. Gemini never generates the plan. Gemini only parameterizes it.**

Most medical follow-up situations are repetitive. A sprain is a sprain, a healing wound is a healing wound. What needs to be observed before a consultation is well known: a daily photo, a pain score, a temperature curve. There is no need for a model to reinvent this per user, and there are strong reasons not to let it:

- **A template is a product.** The user buys something defined: "Wound monitoring, 14 days: one photo per day, pain score twice a day, infection check-ins." The consent screen is exact, the price is justified, the deliverable is known in advance. A dynamically generated plan is a promise, not a product.
- **A template is validatable.** A physician can review a template once, approve it, and that approval holds for every user who runs it. A plan generated at runtime by an LLM can never be reviewed before it executes, and a bad plan means days or weeks of unusable, unrepeatable data.
- **A template is reliable.** The on-device agent executes a deterministic schedule: notifications fire, photos are framed the same way, questions arrive at the same hours. Comparability over time, our core data value, depends on this rigidity.
- **A template is reproducible.** Two patients on the same template produce structurally identical datasets, which makes the analysis prompts simpler and the briefings consistent across doctors.

## Where Gemini fits

Gemini has exactly two roles, on either side of the fixed template, never inside it:

1. **At activation (parameterization).** A short onboarding dialog: "Where is the wound? Are there stitches? When did it happen? When is your appointment?" From the answers, Gemini selects the right template and fills its parameters: duration, body zone for the photo overlay, whether the temperature stream is enabled, check-in hours adapted to the user's routine. It fills slots in a fixed structure. It cannot add streams, remove safety check-ins, or change cadences outside the bounds each parameter declares.
2. **During and after collection (analysis).** Reading the photos, time series and answers; writing the daily micro-reports; compiling the final physician briefing. This is where model intelligence creates value, and it is unconstrained there because its output is reviewed by a physician, not executed by a scheduler.

In short: deterministic collection, intelligent interpretation.

## Anatomy of a template

Every template is a single declarative file containing:

| Section | Content |
| --- | --- |
| `meta` | Template id, version, medical category, typical duration range, price tier |
| `parameters` | The slots Gemini may fill at activation, each with allowed bounds |
| `streams` | What is collected: photo protocols, check-ins, manual vitals, automatic pulls |
| `schedule` | Cadence of each stream, expressed against the follow-up timeline |
| `analysis` | The specialization post-prompt and the micro-report schema for the cloud agent |
| `briefing` | The structure of the final physician document |
| `safety` | Hard thresholds that trigger the generic "seek medical care now" message |

Creating a new agent means writing one such file and having it reviewed. No engineering.

---

## Example template 1: Wound monitoring

```yaml
meta:
  id: wound-monitoring
  version: 1.0.0
  category: wound-and-injury
  duration_days: { min: 7, max: 21, default: 14 }
  price_tier: short

parameters:           # filled by Gemini during activation dialog, within bounds
  body_zone: enum [arm, leg, hand, foot, torso, head]
  has_stitches: bool
  infection_suspected: bool        # enables temperature stream
  checkin_hours: time[2] (between 07:00 and 22:00)

streams:
  wound_photo:
    type: guided_photo
    protocol: ghost_overlay_previous, fixed_distance_hint, blur_and_lighting_check
    zone: ${body_zone}
  pain_score:
    type: checkin
    questions: [pain_0_10, throbbing_yn]
  infection_checkin:
    type: checkin
    questions: [redness_spreading_yn, discharge_yn, odor_yn, warmth_yn]
  temperature:
    type: manual_vital
    enabled_if: ${infection_suspected}

schedule:
  wound_photo:        daily at ${checkin_hours[0]}
  pain_score:         daily at ${checkin_hours[0]}, ${checkin_hours[1]}
  infection_checkin:  daily at ${checkin_hours[1]}
  temperature:        every 8h while enabled

analysis:
  post_prompt: >
    You are observing a healing wound (${body_zone}, stitches: ${has_stitches})
    over ${duration_days} days. From each daily photo, estimate wound surface,
    color distribution and edge state relative to previous days. Correlate with
    pain scores and infection check-ins. Describe and quantify only. Never name
    a diagnosis, never suggest treatment.
  micro_report_schema: [surface_estimate_mm2, color_summary, edge_state,
                        pain_trend, infection_flags, data_quality_notes]

briefing:
  sections: [header_completeness, photo_strip_with_deltas, healing_curve,
             pain_graph, infection_flags_timeline, patient_notes_summary,
             methodology_note]

safety:
  - if: temperature >= 39.5 for 2 consecutive readings
    then: show_seek_care_message
  - if: infection_checkin has 3+ yes answers on same day
    then: show_seek_care_message
```

## Example template 2: Fever episode tracking

```yaml
meta:
  id: fever-episode
  version: 1.0.0
  category: fever-and-infection
  duration_days: { min: 5, max: 20, default: 10 }
  price_tier: short

parameters:
  patient_is_child: bool             # adjusts question wording and safety thresholds
  measurement_method: enum [oral, ear, forehead, rectal]
  medication_taken: bool             # enables medication log
  checkin_hours: time[3] (between 06:00 and 23:00)

streams:
  temperature:
    type: manual_vital
    method: ${measurement_method}
  symptom_checkin:
    type: checkin
    questions: [chills_yn, sweating_yn, appetite_0_3, pain_locations_multi,
                energy_0_3]
    wording: ${patient_is_child} ? parent_reported : self_reported
  medication_log:
    type: checkin
    enabled_if: ${medication_taken}
    questions: [medication_name, dose, time_taken]
  heart_rate:
    type: health_platform_pull
    optional: true

schedule:
  temperature:      daily at ${checkin_hours[0..2]}, plus on-demand entries
  symptom_checkin:  daily at ${checkin_hours[1]}
  medication_log:   on each dose
  heart_rate:       continuous pull, daily aggregation

analysis:
  post_prompt: >
    You are observing a fever episode over ${duration_days} days
    (child: ${patient_is_child}, measurement: ${measurement_method}).
    Build the fever curve, detect periodicity and evening/morning patterns,
    correlate spikes with symptoms and medication intake times. Describe and
    quantify only. Never name a diagnosis, never suggest treatment.
  micro_report_schema: [temp_min_max_avg, spike_events, periodicity_estimate,
                        symptom_cooccurrence, medication_response_window,
                        data_quality_notes]

briefing:
  sections: [header_completeness, fever_curve_full_period,
             medication_overlay_graph, symptom_timeline,
             periodicity_summary, patient_notes_summary, methodology_note]

safety:
  - if: ${patient_is_child} and temperature >= 40.0
    then: show_seek_care_message
  - if: not ${patient_is_child} and temperature >= 40.5
    then: show_seek_care_message
  - if: fever_duration > 5 days with no decrease trend
    then: show_seek_care_message
```
