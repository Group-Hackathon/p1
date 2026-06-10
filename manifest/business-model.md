# Business model

## Summary

The application is free. The personal backend is the user's own (their VPS, their machine). Revenue comes from one thing: specialized analysis agents, purchased per follow-up period.

This matches the architecture: since we hold no data and run no central platform for medical records, the only thing we operate, and the only thing we charge for, is the analysis intelligence.

## The product being sold

An analysis agent purchase buys one follow-up period:

- The collection plan executed by the phone for the chosen duration.
- Daily multimodal analysis runs (Gemini via Vertex AI) producing micro-reports.
- The compiled graphs, photo timeline and physician briefing at the end.

Technically, an agent is a specialization layer (post-prompt, collection plan, output schemas) on top of Gemini. Marginal cost is inference plus a Cloud Run job. Creating a new agent is content and validation work, not engineering, so the catalog can grow weekly during the hackathon.

## Pricing (initial hypothesis, to be tested on real customers)

| Product | Price |
| --- | --- |
| Single follow-up, short (up to 14 days), e.g. wound monitoring | 9 EUR |
| Single follow-up, long (up to 60 days), e.g. dermatology, recovery | 19 EUR |
| Family pack, 5 follow-ups, any type | 39 EUR |
| Recurring chronic pre-consultation (per quarter) | 15 EUR / quarter |

Reference point: the price of one follow-up is below the out-of-pocket cost of a single consultation in most markets, and the pitch is that it makes that consultation worth more.

## Why people pay

- Patients: anxiety reduction through structure ("everything will be ready for the doctor"), no lost information, shorter and more useful appointments, repeat need (injuries and flares recur).
- Parents: the strongest buyer. A 20-day fever or eczema episode in a child generates exactly the willingness to pay this targets.
- Doctors as prescribers, not payers: a physician who receives one good briefing tells the next patient "track this with the app before you come back". The briefing footer is our acquisition channel.

## Revenue during the hackathon window

Constraint from the XPRIZE brief: real customers and real revenue before August 17th.

1. Week 1-3: ship the wound-monitoring agent end to end (it has the shortest follow-up cycle, so full purchase-to-briefing loops can complete within the hackathon).
2. Sell single follow-ups directly: parenting communities, sports clubs (sprains, tears), post-op patient forums.
3. Each delivered briefing is itself a demo artifact for the next sale.

A managed-hosting option (we provision the user's personal backend instance on their behalf, still isolated per user, still their data) can be offered at 2 EUR / month for non-technical users. It is operationally simple and removes the main onboarding barrier without recreating a central database.

## What we never monetize

- Medical data. We do not have it, so it cannot be sold, aggregated, or used for training. This is a structural guarantee and a core selling point, not a policy promise.
- Patient attention. No ads, no engagement mechanics beyond the collection routine itself.

## Unit economics sketch

Per short follow-up at 9 EUR: roughly 14 daily multimodal inference runs plus one compilation run. At current Gemini pricing this is well under 1 EUR of inference, leaving a healthy margin even with payment fees and support. Long follow-ups scale linearly and are priced accordingly.
