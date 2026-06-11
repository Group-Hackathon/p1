# Security

## Never commit these

- `GEMINI_API_KEY` (Google AI Studio)
- `DATABASE_URL` with real Neon credentials
- `JWT_SECRET` production values
- GCP service account JSON files
- Any `.env` file with real values

## Where secrets live in production

| Secret | Storage |
| --- | --- |
| Gemini API key | GCP Secret Manager (`gemini-api-key`) |
| Database URL | Cloud Run env var (set via `gcloud run deploy`) |
| JWT secret | Cloud Run env var |

## Local development

Copy `global-app-backend/.env.example` to `.env` and fill in your own values locally. The `.env` file is gitignored.

## If a key was exposed

1. Rotate the key immediately in AI Studio / Neon / GCP.
2. Update Secret Manager and Cloud Run.
3. Do not rewrite git history unless instructed — rotate first.
