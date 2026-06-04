// apiUrl is injected at image-build time from a GitHub secret (per environment)
// via `sed` in the Dockerfile — see frontend/Dockerfile. Do NOT hardcode a real
// URL here; __API_URL__ is the placeholder that gets replaced.
export const environment = {
  production: true,
  apiUrl: '__API_URL__'
};
