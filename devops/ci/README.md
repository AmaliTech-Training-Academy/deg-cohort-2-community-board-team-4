# CI/CD Pipeline

Post-merge pipeline that runs on **push to `dev`, `test`, `main`** (and manual
`workflow_dispatch`). PRs are gated separately by `.github/workflows/pr-checks.yml`.

## Layout (split, reusable workflows)

| File | Role |
|------|------|
| `.github/workflows/ci.yml` | **Orchestrator** — change detection + fail-fast ordering |
| `.github/workflows/ci-security.yml` | gitleaks + Trivy-fs + Snyk (advisory, parallel) |
| `.github/workflows/ci-unit-tests.yml` | backend / frontend / data-eng unit tests (parallel) |
| `.github/workflows/ci-qa.yml` | docker compose stack + API & UI suites (parallel) |
| `.github/workflows/ci-images.yml` | build → Trivy image scan → stage-tagged push (parallel) |

## Flow

```
changes ─► security        (advisory; never blocks)
        └► unit-tests ─► qa ───────────────┐
           (GATE)        (GATE)            ├─► images ─► ci-gate
           per-area      compose + api/ui  │   build+scan+push
           parallel      parallel          │   per-service parallel
                                           ┘
```

- **Folder-scoped triggers:** `dorny/paths-filter` detects `backend/**`,
  `frontend/**`, `data-engineering/**`; only changed areas run. QA runs only when
  `backend` or `frontend` changed.
- **Fail-fast:** a failed unit test stops QA and image build/push for that run.
- **Parallelism:** unit-test areas, the two QA suites, and the per-service image
  jobs all run concurrently.
- **Caching:** Maven (`~/.m2`), npm (lockfile-keyed), uv, and docker buildx
  layers (GHA cache) are all cached.

## Security posture — ADVISORY

gitleaks, Trivy (fs + image) and Snyk **report but do not block** (team decision).
Each scan has a documented one-line toggle to become a hard gate:
- gitleaks / Snyk: remove `continue-on-error: true`.
- Trivy fs: `exit-code: '0'` → `'1'`.
- Trivy image: set repo variable `TRIVY_IMAGE_EXIT_CODE=1`.
- SonarQube quality gate: remove `continue-on-error: true` on the quality-gate step.

SAST (SonarQube) runs in the `sonar` job of `ci-security.yml` via the official
SonarQube actions and **self-skips when `SONAR_TOKEN` is not set** — see
`devops/sonar/README.md`.

## Concurrency

The orchestrator (`ci.yml`) uses group `ci-${{ github.ref }}` (cancel-in-progress)
so a newer push to a branch supersedes the older run. Each reusable workflow has
its **own** group with a distinct name (`ci-security-*`, `ci-unit-tests-*`,
`ci-qa-*`, `ci-images-*`, plus a job-level `sonar-*`) — distinct names are
required to avoid a caller/callee deadlock. `ci-images` uses
`cancel-in-progress: false` so an in-flight image push is never interrupted.

## Image tags (push)

| Branch | Tags pushed | Notes |
|--------|-------------|-------|
| `dev`  | `:dev`, `:dev-<shortsha>` | rolling + traceable |
| `test` | `:test-latest`, `:test-<shortsha>` | matches `docker-compose-staging.yml` |
| `main` | `:<full-sha>`, `:latest` | **immutable** deploy tag = full sha |

Image names: `communityboard-backend`, `communityboard-frontend`,
`communityboard-data-engineering`.

## Required configuration

### Secrets
| Secret | Required? | Used by |
|--------|-----------|---------|
| `GITLEAKS_LICENSE` | yes (org-owned repo) | gitleaks-action v2 |
| `DOCKERHUB_TOKEN` | yes (for image push) | Docker Hub login (Account → Security → access token) |
| `SNYK_TOKEN` | optional | Snyk SCA (skipped if unset) |
| `SONAR_TOKEN`, `SONAR_HOST_URL` | only when enabling Sonar | SonarQube |
| `DEV_API_URL` / `TEST_API_URL` / `PROD_API_URL` | for the frontend image | Backend API URL baked into the Angular build per branch (`dev`/`test`/`main`). Injected via `--build-arg API_URL` → replaces `__API_URL__` in `environment.prod.ts`. |

### Variables
| Variable | Required? | Default | Purpose |
|----------|-----------|---------|---------|
| `DOCKERHUB_USERNAME` | yes (for image push) | — | Docker Hub account/org: image namespace **and** login username |
| `REGISTRY` | no | `docker.io` | registry host |
| `IMAGE_NAMESPACE` | no | `DOCKERHUB_USERNAME` | override the namespace |
| `TRIVY_IMAGE_EXIT_CODE` | no | `0` | set `1` to make image CVEs block the push |

Images are pushed to **Docker Hub** as
`docker.io/<DOCKERHUB_USERNAME>/communityboard-<service>:<tag>`. The image build
runs but **skips the push** on any branch other than `dev`/`test`/`main`.

## CD — deploy to Dev (`cd-dev.yml`)

Runs automatically **after the CI Pipeline succeeds on `dev`** (via `workflow_run`),
once the `:dev` images are on Docker Hub. It:
1. **validate** — fails fast if any EC2 / Docker Hub config is missing.
2. **deploy** — copies **only** `docker-compose-dev.yml` to the EC2 (no source
   code), then over SSH: `sed`s `__REGISTRY__` → the Docker Hub namespace,
   `docker login`, `docker compose pull` (grabs the new `:dev` tag), `up -d`.

The dev stack runs from the **pulled** `:dev` images; `docker-compose-dev.yml`
is static (pull-only, `:dev` tag, dummy dev env values).

### Required CD configuration
| Kind | Name | Purpose |
|------|------|---------|
| Secret | `EC2_DEV_HOST` | dev EC2 hostname / IP |
| Secret | `EC2_DEV_SSH_KEY` | private SSH key for the EC2 user |
| Variable | `EC2_DEV_USER` | SSH user (e.g. `ubuntu` / `ec2-user`) |
| Variable | `DOCKERHUB_USERNAME` | Docker Hub namespace + login (shared with CI) |
| Secret | `DOCKERHUB_TOKEN` | Docker Hub token (shared with CI) |
| Variable | `EC2_SSH_PORT` | optional, default `22` |
| Variable | `EC2_DEV_APP_DIR` | optional, default `~/communityboard` |

The EC2 must have Docker + the Compose plugin installed; the deploy creates/uses
`EC2_DEV_APP_DIR` and only ever places `docker-compose-dev.yml` there.

> For the `docker-compose-staging.yml` / `docker-compose-prod.yml` files, set their
> `REGISTRY` env to `<DOCKERHUB_USERNAME>` (or `docker.io/<DOCKERHUB_USERNAME>`) so
> `${REGISTRY}/communityboard-<service>` resolves to the same images this pipeline pushes.

## ⚠️ Known prerequisites before the pipeline goes green

1. **`data-engineering/Dockerfile` is stale.** It `COPY requirements.txt`, but the
   project moved to `uv` (`pyproject.toml` + `uv.lock`) and has **no
   `requirements.txt`**. The data-engineering image build — and therefore
   `docker compose up --build` in the QA job — **fails** until the Dockerfile is
   switched to `uv` (or a `requirements.txt` is generated via
   `uv export --no-hashes -o requirements.txt`).
2. **`PostServiceTest` must exist.** The backend unit-test step runs
   `mvn test -Dtest=PostServiceTest`; until that test class is committed the step
   fails with "No tests matching". Use `mvn -B test` to run the whole suite instead.
