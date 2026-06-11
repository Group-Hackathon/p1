# Deploy Your Own Backend (Self-Hosted Instance)

This folder contains the configurations and instructions to deploy your own private backend node (**VPC** - Virtual Private Cloud) for **Living Patient Memory**.

By deploying your own instance, you ensure that **you are the sole owner and custodian of your medical records**. No central database stores your symptoms, metrics, or photos.

---

## Private Stack Architecture

Each private backend node is fully self-contained and runs inside a **Docker Compose** container network:

1. **Caddy (Reverse Proxy)**: Automatically handles domain/IP binding and SSL/TLS certificate setup (via Let's Encrypt).
2. **Private Go API Server**: Handles uploads from the mobile app, manages internal data storage, and issues secure, scoped API access tokens to authorized Gemini cloud analysis agents.
3. **PostgreSQL (Local DB)**: Stores patient check-in answers, symptom levels, and monitoring timelines for this instance's users only.
4. **MinIO / Local Storage**: Standard S3-compatible local bucket storing client-side encrypted patient photos.

---

## Quickstart: Manual Deployment via Docker Compose

### Prerequisites
- A virtual machine (VPS) running Ubuntu 22.04 LTS (or any Unix-based OS with Docker installed).
- Docker & Docker Compose installed.
- Publicly accessible domain name (optional but recommended for SSL).

### Configuration (`docker-compose.yml`)

Save the following file on your server:

```yaml
version: '3.8'

services:
  caddy:
    image: caddy:2-alpine
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - caddy_data:/data
      - caddy_config:/config
      - ./Caddyfile:/etc/caddy/Caddyfile
    depends_on:
      - api-server

  api-server:
    image: livingpatientmemory/private-backend:latest
    restart: always
    environment:
      - DB_HOST=db
      - DB_USER=patient
      - DB_PASSWORD=secure_password_here
      - DB_NAME=patient_records
      - STORAGE_TYPE=minio
      - MINIO_ENDPOINT=minio:9000
    depends_on:
      - db
      - minio

  db:
    image: postgres:15-alpine
    restart: always
    environment:
      - POSTGRES_USER=patient
      - POSTGRES_PASSWORD=secure_password_here
      - POSTGRES_DB=patient_records
    volumes:
      - db_data:/var/lib/postgresql/data

  minio:
    image: minio/minio:latest
    restart: always
    command: server /data
    environment:
      - MINIO_ROOT_USER=minio_admin
      - MINIO_ROOT_PASSWORD=minio_secret_password
    volumes:
      - minio_data:/data

volumes:
  caddy_data:
  caddy_config:
  db_data:
  minio_data:
```

### Run the Stack
Run the following command to download and start the containers in the background:
```bash
docker compose up -d
```

---

## App Pairing Protocol

Once the stack is running:
1. Log in to the command-line setup wizard on the server to generate a **Pairing QR Code**.
2. Open the **Living Patient Memory** mobile app.
3. Scan the QR Code. The app will extract the connection credentials and API endpoints, establishing a direct connection between your phone and your private server.

---

## Future Tool: Desktop Auto-Deployer (Under Construction)

To assist non-technical users, we are developing a standalone **Desktop Auto-Deployer** wizard. 
- **Goal**: Provision the VPS (DigitalOcean, GCP, AWS, OVH) using provider API keys, configure Docker, deploy the Compose stack, and print the pairing QR Code automatically.
- **Status**: Deferred for initial MVP development, manual installation via Docker Compose is the current standard.
