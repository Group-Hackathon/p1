# MVP Security Vulnerability: Anonymous Device Authentication

## The Problem
Currently, the mobile applications (both iOS and Android) implement a frictionless, silent authentication mechanism. This is achieved by generating a random `device_id` on the client side and using a hardcoded string (`secret_device_password`) to automatically register and log in the user against the backend API.

While this provides an excellent user experience for an MVP (as users do not need to create accounts to start using the app), it introduces a **Critical Security Vulnerability**.

Because the `secret_device_password` and the API endpoints are baked directly into the mobile application binaries (.apk and .ipa), a malicious actor can easily reverse-engineer the app, extract these credentials, and write an automated script to bombard the Cloud Run backend. 

Since the backend calls Google's Gemini API for analysis, an attacker could simulate thousands of fake devices, triggering endless Gemini requests. This could lead to a severe Denial of Wallet (DoW) attack, causing massive unexpected cloud billing charges.

> [!NOTE]  
> **Temporary MVP Safeguard (For Developers):**
> Do not panic! While this vulnerability exists in the current codebase, the actual financial risk is fully mitigated for the MVP. The Gemini API key is linked to a disposable Google Cloud account. We have configured a strict, fixed billing quota that cannot be exceeded. In the worst-case scenario of an attack, the API will simply stop responding once the small credit is exhausted, and we can easily swap the disposable Google account if needed.

## How We Will Fix It in Production

Before moving from the MVP phase to the production phase, we must implement proper security measures. We will adopt one or more of the following industry-standard solutions:

### 1. Cryptographic App Attestation (Recommended)
We will implement **Firebase App Check** (using Apple DeviceCheck/AppAttest for iOS and Play Integrity API for Android). 
This requires the client device to fetch a cryptographic token from Apple/Google proving that:
- The request originates from our official, unmodified application.
- The app is running on a genuine, non-compromised physical device.
Our backend will verify this token before processing any API requests. If a Python script or compromised app attempts to hit the API, the backend will immediately reject it without calling Gemini.

### 2. Backend Rate Limiting & Quotas
We will enforce strict API limits directly on the Cloud Run server or at the API Gateway (e.g., Cloudflare):
- **IP-based Limits:** Limit the number of accounts that can be created from a single IP address per day.
- **Account Quotas:** Limit the number of Gemini recommendation requests a single `profile_id` can make per 24 hours.

### 3. Transition to Authenticated Users
We will abandon the anonymous `device_id` login and enforce legitimate user authentication mechanisms, such as:
- **Sign in with Apple**
- **Google Sign-in**
This forces attackers to generate real Apple/Google accounts to interact with the service, severely increasing the cost and difficulty of launching an automated attack.
