# SonarQube (SAST)

SonarQube runs in the **`sonar` job of `.github/workflows/ci-security.yml`**
using the official SonarQube actions:

- `SonarSource/sonarqube-scan-action@v5` — runs the analysis (multi-language).
- `SonarSource/sonarqube-quality-gate-action@v1` — checks the quality gate
  (currently **advisory**: `continue-on-error: true`).

The job **self-skips when `SONAR_TOKEN` is not configured**, so the pipeline is
green until you turn it on.

## What's here

- `sonar-project.properties` — multi-module scanner config (Java + Angular +
  Python). The scan job passes it via `-Dproject.settings=...`.

## Enable it

1. Add the secrets:
   - **Self-hosted SonarQube:** repo secrets `SONAR_TOKEN` **and** `SONAR_HOST_URL`.
   - **SonarQube Cloud / SonarCloud:** repo secret `SONAR_TOKEN`, set
     `SONAR_HOST_URL` to `https://sonarcloud.io`, and uncomment
     `sonar.organization` in `sonar-project.properties`.

   That's it — the `sonar` job detects the token and runs on the next push.

2. **Make the gate blocking (optional):** remove `continue-on-error: true` on the
   `SonarQube quality gate` step in `ci-security.yml`.

## How the backend is analysed

The job compiles the backend (`mvn -B -DskipTests compile`) before scanning so
Sonar sees real Java bytecode — `sonar-project.properties` points
`backend.sonar.java.binaries` at `backend/target/classes`.

## Coverage inputs (optional, recommended)

- Backend: JaCoCo XML at `backend/target/site/jacoco/jacoco.xml`.
- Frontend: enable `karma-coverage` → `frontend/coverage/.../lcov.info`.
- Data-engineering: `uv run pytest --cov --cov-report=xml` → `coverage.xml`.

The properties file already points at these paths.
