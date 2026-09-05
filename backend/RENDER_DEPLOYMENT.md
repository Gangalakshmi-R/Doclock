# Render file storage

Attach a persistent disk to the backend service and set `FILE_UPLOAD_DIR` to a
folder on its mount, for example `/var/data/uploads`. Without a persistent disk,
Render can remove uploaded PDFs when the service restarts or redeploys.

The backend requires these environment variables:

```text
DOCLOCK_USERNAME=your-login-name
DOCLOCK_PASSWORD=your-login-password
DOCLOCK_JWT_SECRET=a-random-secret-of-at-least-32-characters
GEMINI_API_KEY=your-google-genai-key
FILE_UPLOAD_DIR=/var/data/uploads
```

Set `VITE_API_BASE_URL=https://doclock-be.onrender.com/api` on the frontend
Static Site and redeploy it after changing the value.
