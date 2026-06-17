# Pre-Appointment 1 - Version 1.0.3

## New Features & Commercial Upgrades
- **Dynamic Onboarding & Monetization**: Implemented a dynamic pricing model based on tracking duration (0.20€/day, capped at 5.99€). The AI analysis is now presented as a "proprietary intelligent technology" analyzing thousands of medical records (no "Gemini" branding).
- **Manual Protocol Configuration**: Free/Offline fallback allowing patients to manually configure their reminders day-by-day (intentionally tedious to encourage premium conversion).
- **PDF Data Visualization**: The PDF report now generates native, medical-grade line charts (Temperature & Pain evolution) directly on the document, providing instant value to physicians.
- **Local Push Notifications (Architecture)**: Implemented zero-cost, 100% reliable local push notifications via `AlarmManager` to remind patients of their tracking without server costs.

---

# Commercial & Technical Roadmap - Version 1.0.4

In the upcoming version, we will focus on real payment integration and finalizing the notification system.

## 1. Google Play Billing Integration (In-App Purchases)
- Replace the fake payment button with real Google Play Console Billing.
- **Adaptive Pricing Strategy**: Since Google Play Billing does not support arbitrary "on-the-fly" dynamic prices, we will pre-create a set of SKUs in the Play Console representing price steps (e.g., `sku_tier_1` for 0.20€, `sku_tier_2` for 0.40€, up to `sku_tier_30` for 5.99€). The app will automatically select and trigger the correct SKU based on the duration.

## 2. Notification Triggers
- Connect the `NotificationHelper` to the onboarding flow to automatically arm the 08:00, 12:00, and 20:00 alarms based on the generated/purchased schedule.

## 3. Real Authentication & Cloud Sync
- Integrate Firebase Auth (Google / Apple Sign-In) to ensure patient data is backed up and recoverable if the device is lost.
