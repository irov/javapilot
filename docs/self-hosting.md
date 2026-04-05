# Self-Hosting

Deploy the Pilot dashboard, backend, database, and optionally a LiveKit server on your own infrastructure.

## Architecture

| Component | Port | Description |
|-----------|------|-------------|
| **Backend** (FastAPI) | 8000 | REST API + WebSocket, Google OAuth |
| **Client** (Next.js) | 3000 | Dashboard web app |
| **PostgreSQL** | 5432 | Sessions, logs, metrics, actions |
| **LiveKit** (optional) | 7880/7881/7882 | Real-time video streaming |

## Quick start (development)

```bash
cd webpilot
docker compose up
```

This starts the backend, client, and PostgreSQL with default credentials (`pilot`/`pilot`).

## Production deployment

Use `docker-compose.prod.yml` with an `.env` file. See the GitHub Actions workflow for automated deployment.

### Required environment variables

| Variable | Description |
|----------|-------------|
| `PLT_DATABASE_URL` | PostgreSQL connection string |
| `PLT_SECRET_KEY` | Backend session secret |
| `PLT_GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `PLT_GOOGLE_CLIENT_SECRET` | Google OAuth client secret |
| `PLT_DOMAIN` | Your domain (e.g. `pilot.example.com`) |

### Required GitHub secrets (for CI/CD)

| Secret | Description |
|--------|-------------|
| `SERVER_HOST` | Server IP or hostname |
| `SERVER_SSH_KEY` | SSH private key |
| `GHCR_TOKEN` | GitHub Container Registry token |
| `DOMAIN` | Production domain |
| `SSL_EMAIL` | Email for Let's Encrypt |

## Self-hosted LiveKit

To enable live screen streaming, deploy LiveKit on the same server.

### Additional DNS records

Point all of these to the server IP:

- `<your-domain>`
- `www.<your-domain>`
- `api.<your-domain>`
- `rtc.<your-domain>`

### Additional environment variables

Add these to `PRODUCTION_ENV`:

```env
PLT_LIVEKIT_API_KEY=your-livekit-api-key
PLT_LIVEKIT_API_SECRET=your-livekit-api-secret
PLT_LIVEKIT_ROOM_PREFIX=pilot-session
```

`PLT_LIVEKIT_URL` is injected automatically as `wss://rtc.<your-domain>` by the deploy workflow.

### Firewall rules

Open these ports for LiveKit media:

| Port | Protocol | Description |
|------|----------|-------------|
| 80 | TCP | HTTP (redirect to HTTPS) |
| 443 | TCP | HTTPS |
| 7881 | TCP | LiveKit signaling |
| 7882 | UDP | LiveKit media (WebRTC) |

### Manual deployment note

If you deploy manually instead of via GitHub Actions, you also need:

- A `livekit.yaml` file next to the compose file
- `PLT_LIVEKIT_URL=wss://rtc.<your-domain>` in `.env`
- Nginx reverse proxy and TLS certificate for `rtc.<your-domain>`

### Current limitations

This setup uses LiveKit signaling over `wss://rtc.<your-domain>` and media over raw ports. TURN/TLS is not enabled, so users behind restrictive corporate firewalls may have connectivity issues.

---

**See also:** [Android Integration](android-integration.md) · [Live Streaming](live-streaming.md)
