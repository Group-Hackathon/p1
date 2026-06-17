# Team Update: Project Setup & Dev Launch (English Version)

Hey team! 

The workspace for **Project 1 (Pre-Appointment 1)** is fully initialized for dev. Here is the current state and how you can jump in:

## Current Setup & Architecture

1. **Android App (`androidp1/`)**: A self-contained KMP project containing:
   - Shared business logic modules (`shared:core` & `shared:agent`).
   - The native Android app module (`app`) using Jetpack Compose.
   - *A test APK will be built soon, pointing to our shared development backend so anyone can test instantly.*

2. **Global Backend (`global-app-backend/`)**:
   - The central orchestrator that handles user accounts, profiles, subscriptions, and Stripe integration.
   - The global PostgreSQL schema is documented inside this directory.
   - **No medical data** is stored here.

3. **Private User Backend (`deploy-your-own-backend/`)**:
   - A secure, self-hosted Docker Compose stack (Go + PostgreSQL + MinIO + Caddy) for user-owned medical storage.

---

## How to Contribute & Collaborate

* **Add Features**: You are welcome to modify the entire project directly to implement features or UI improvements.
* **Experiment & Custom Pipelines**: If you want to try different setups, custom prompts, or distinct API pipelines, please **fork the main repo inside the Group-Hackathon organization** using your name (e.g., `p1-your-name`).
* **Credentials Security**: Use **GitHub Secrets** or local `.env` files to protect your GCP/Gemini keys. Never commit api keys or raw credentials to the repository.

-_-
