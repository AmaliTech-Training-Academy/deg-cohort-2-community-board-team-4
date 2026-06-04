# Deployment Guide — CommunityBoard

This document lets anyone reproduce, run, and deploy CommunityBoard from a clean
machine in a few minutes. It covers:

1. [Prerequisites](#1-prerequisites)
2. [Environment variables](#2-environment-variables)
3. [Run locally (one command)](#3-run-locally-one-command)
4. [The environments](#4-the-environments-dev--test--prod)
5. [CI/CD pipeline design](#5-cicd-pipeline-design) — **how the pipeline is designed**
6. [Provisioning the cloud (Terraform + Ansible)](#6-provisioning-the-cloud-terraform--ansible)
7. [Seed data runbook (before a demo)](#7-seed-data-runbook-before-a-demo)
8. [Verification & smoke tests](#8-verification--smoke-tests)
9. [Troubleshooting](#9-troubleshooting)

> Architecture diagram (AWS components): see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## 1. Prerequisites

### To run / demo locally
| Tool | Version | Notes |
|------|---------|-------|
| Docker Engine | 24+ | the only hard requirement for a local run |
| Docker Compose | v2 (`docker compose`, plugin) | bundled with Docker Desktop |
| Git | any | to clone the repo |

Optional, only if you build/test a single service outside Docker:
- **JDK 17** (Temurin) + Maven — backend & QA suites
- **Node 18+** — frontend
- **[uv](https://docs.astral.sh/uv/)** + Python 3.11 — data-engineering ETL

### To provision cloud infrastructure (DevOps only)
| Tool | Version | Notes |
|------|---------|-------|
| Terraform | ≥ 1.5 | infra in `devops/infra` (AWS + S3 backend) |
| Ansible | core ≥ 2.15 | installs Docker on the EC2 host |
| AWS CLI | v2 | credentials for the target account |

### Accounts / access
- **AWS account** with permission to create EC2, security groups, key pairs, and an S3 state bucket.
- **Docker Hub** account/org — CI pushes images here; the hosts pull from here.
- **GitHub repo admin** — to configure Actions secrets/variables (see §5).

---

## 2. Environment variables

Copy [.env.example](.env.example) to `.env` for a local run. The compose files
ship with safe **dummy** values for dev/test; production requires real secrets
(the prod compose fails fast if they are missing).

### Application variables
| Variable | Used by | Local default | Notes |
|----------|---------|---------------|-------|
| `FRONTEND_PORT` | frontend | `3000` | host port for the UI |
| `FRONTEND_API_URL` | frontend | `http://localhost:8080/api` | backend URL baked into the SPA |
| `BACKEND_PORT` | backend | `8080` | host port for the REST API |
| `BACKEND_CONTEXT_PATH` | backend | `/api` | REST base path |
| `JWT_SECRET` | backend | dummy (dev) | **HS256 key, ≥ 32 chars — set a real one in prod** |
| `DB_HOST` | backend, data-eng | `postgres` | the compose service name |
| `DB_PORT` | postgres | `5432` | |
| `DB_NAME` | all | `communityboard` | |
| `DB_USER` | all | `postgres` (dev) | |
| `DB_PASSWORD` | all | `postgres` (dev) | **set a real one in prod** |

### Per-environment compose variables
| Variable | File | Required? | Purpose |
|----------|------|-----------|---------|
| `REGISTRY` | staging / prod | **yes** | Docker Hub namespace, e.g. `docker.io/<org>` or `<org>` |
| `IMAGE_TAG` | prod | no (default `latest`) | immutable deploy tag = the main-branch full SHA |
| `SPRING_PROFILES_ACTIVE` | all | no | `dev` / `test` / `prod` |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | staging / prod | prod: **yes** | DB credentials |
| `AWS_REGION` | staging | no (default `eu-west-1`) | CloudWatch Logs region for the `awslogs` driver |
| `RUN_SEED` | data-engineering | no | `true` seeds sample data (dev/test only — **never prod**) |
| `APP_ENV` | data-engineering | no | `development` / `staging` / `production`; `production` hard-blocks seeding |

### CI/CD secrets & variables (GitHub → Settings → Secrets and variables → Actions)
These live in GitHub, **not** in the repo. See §5 for what each one feeds.

| Kind | Name | Purpose |
|------|------|---------|
| Variable | `DOCKERHUB_USERNAME` | Docker Hub namespace **and** login user (image push) |
| Secret | `DOCKERHUB_TOKEN` | Docker Hub access token |
| Secret | `EC2_DEV_HOST` | dev EC2 host/IP (CD target) |
| Variable | `EC2_DEV_USER` | SSH user (`ec2-user`) |
| Secret | `EC2_DEV_SSH_KEY` | private SSH key for the deploy user |
| Variable | `EC2_DEV_APP_DIR` | optional, default `~/communityboard` |
| Variable | `EC2_SSH_PORT` | optional, default `22` |
| Secret | `DEV_API_URL` / `TEST_API_URL` / `PROD_API_URL` | backend URL baked into the frontend image per branch |
| Secret | `GITLEAKS_LICENSE` | gitleaks-action (org-owned repos) |
| Secret | `SNYK_TOKEN` | optional SCA (self-skips if unset) |
| Secret/Var | `SONAR_TOKEN` / `SONAR_HOST_URL` | optional SAST (self-skips if unset) |
| Variable | `TRIVY_IMAGE_EXIT_CODE` | optional, set `1` to make image CVEs block the push |

---

## 3. Run locally (one command)

```bash
git clone https://github.com/AmaliTech-Training-Academy/deg-cohort-2-community-board-team-4.git
cd deg-cohort-2-community-board-team-4
docker compose up --build
```

This boots the full stack from source via [docker-compose.yml](docker-compose.yml):

| Service | URL / port | Notes |
|---------|-----------|-------|
| Frontend (UI) | http://localhost:3000 | |
| Backend (API) | http://localhost:8080 | Swagger: `/swagger-ui.html`, OpenAPI: `/api-docs` |
| PostgreSQL | `localhost:5432` | db `communityboard`, user/pass `postgres` |
| data-engineering | (no port) | runs Flyway-seeded schema → seeds sample data → ETL loop |

Startup ordering is enforced by healthchecks: **postgres healthy → backend
healthy (Flyway migrations done) → frontend + data-engineering**. The
data-engineering container then **auto-seeds** sample data because `RUN_SEED=true`
in the local compose (see §7).

Default seeded logins:

| Email | Password | Role |
|-------|----------|------|
| `admin@amalitech.com` | `password123` | ADMIN |
| `user@amalitech.com` | `password123` | USER |

Tear down (and wipe the DB volume):
```bash
docker compose down -v
```

---

## 4. The environments (dev · test · prod)

The same image set runs in every environment; only the **compose file**, the
**image tag**, and the **config values** differ.

| Env | Compose file | Image tag | Source code on host? | Seeding | Secrets |
|-----|--------------|-----------|----------------------|---------|---------|
| **local** | `docker-compose.yml` | built from source | yes (build) | `RUN_SEED=true` | dummy |
| **dev** | `docker-compose-dev.yml` | `:dev` | **no** (pull only) | `RUN_SEED=true` | static dummy |
| **test/staging** | `docker-compose-staging.yml` | `:test-latest` | no (pull only) | `RUN_SEED=true` | dummy + CloudWatch logs |
| **prod** | `docker-compose-prod.yml` | `:${IMAGE_TAG}` (= main SHA) | no (pull only) | **disabled** (`RUN_SEED=false`, `APP_ENV=production`) | **required** (fail-fast) |

Key design point: the **dev/test/prod hosts never see source code**. CI builds and
pushes images to Docker Hub; CD copies *only* the relevant `docker-compose-*.yml`
to the host and runs `docker compose pull && up -d`. This keeps the host minimal
and the deploy fast and reproducible.

---

## 5. CI/CD pipeline design

> Full rationale and the exact secret/variable list also live in
> [devops/ci/README.md](devops/ci/README.md). This section explains **how it is
> designed and why**.

### 5.1 Branch flow

```
feature/* ──PR──► dev ──PR──► test ──PR──► main
   │               │            │            │
 pr-checks       CI + CD      CI + CD      CI + (CD prod)
 (gate PRs)      to Dev EC2   to Staging   immutable :sha
```

- **Pull requests** are gated by [`pr-checks.yml`](.github/workflows/pr-checks.yml) — fast feedback before merge.
- **Post-merge pushes** to `dev`, `test`, `main` trigger the **CI Pipeline**
  ([`ci.yml`](.github/workflows/ci.yml)), which on success triggers **CD** via a
  `workflow_run` hook.

### 5.2 CI Pipeline — orchestrator + reusable workflows

`ci.yml` is the **orchestrator**. Each concern is a small reusable workflow so it
reads independently, while fail-fast ordering is expressed through `needs:`:

```
changes ─► security        (advisory: gitleaks + Trivy-fs + Snyk + Sonar; never blocks)
        └► unit-tests ─► qa ───────────────┐
           (GATE)        (GATE)            ├─► images ─► ci-gate
           per-area       compose+API/UI   │   build+scan+push
           parallel       parallel         │   per-service parallel
                                           ┘
```

| Stage | File | Type | What it does |
|-------|------|------|--------------|
| **changes** | `ci.yml` | — | `dorny/paths-filter` decides which areas (`backend/`, `frontend/`, `data-engineering/`) changed. Everything downstream is gated on this, so an unchanged area never builds or tests. |
| **security** | [`ci-security.yml`](.github/workflows/ci-security.yml) | **advisory** | gitleaks (secrets), Trivy filesystem (deps/CVEs), Snyk (SCA), SonarQube (SAST). Reports to the run summary; **does not block** (team decision). Each has a documented one-line toggle to become a hard gate. |
| **unit-tests** | [`ci-unit-tests.yml`](.github/workflows/ci-unit-tests.yml) | **GATE** | One parallel job per changed area: backend (Maven/JUnit), frontend (npm), data-engineering (pytest/uv). |
| **qa** | [`ci-qa.yml`](.github/workflows/ci-qa.yml) | **GATE** | Boots the whole stack with `docker compose` on the runner, waits for the backend, runs the API (and UI) suites against it. Only runs when backend/frontend changed. |
| **images** | [`ci-images.yml`](.github/workflows/ci-images.yml) | build/publish | Per changed service, in parallel: build once (buildx + GHA layer cache) → Trivy image scan (advisory) → smoke-check the image starts → push **stage-tagged** images to Docker Hub. |
| **ci-gate** | `ci.yml` | aggregate | Single required status. Real gates (unit-tests, qa, images) must not fail/cancel; `skipped` is allowed; `security` is intentionally ignored (advisory). |

**Design principles baked in:**
- **Folder-scoped** — a frontend-only push never builds the backend or data-eng.
- **Fail-fast** — a failed unit test stops QA and the image build for that run.
- **Parallel where safe** — unit-test areas, the two QA suites, and the per-service
  image jobs all run concurrently.
- **Caching everywhere** — Maven (`~/.m2`), npm (lockfile-keyed), uv, and Docker
  buildx layers (GHA cache).
- **Concurrency isolation** — the orchestrator and each reusable workflow use
  *distinct* concurrency groups to avoid a caller/callee deadlock; image pushes use
  `cancel-in-progress: false` so an in-flight push is never interrupted.

### 5.3 Stage-based image tags

The branch a push lands on decides the tags (see `ci-images.yml`):

| Branch | Tags pushed | Meaning |
|--------|-------------|---------|
| `dev` | `:dev`, `:dev-<shortsha>` | rolling + traceable |
| `test` | `:test-latest`, `:test-<shortsha>` | matches `docker-compose-staging.yml` |
| `main` | `:<full-sha>`, `:latest` | **immutable** deploy tag = full SHA |
| other | `:<ref>-<shortsha>` | built & scanned, **not** pushed |

Image names: `communityboard-backend`, `communityboard-frontend`,
`communityboard-data-engineering`, pushed to
`docker.io/<DOCKERHUB_USERNAME>/communityboard-<service>:<tag>`.

### 5.4 CD — deploy to Dev

[`cd-dev.yml`](.github/workflows/cd-dev.yml) runs automatically **after the CI
Pipeline succeeds on `dev`** (a `workflow_run` trigger), once the `:dev` images
are on Docker Hub:

1. **validate** — fails fast if any EC2 / Docker Hub config is missing.
2. **deploy** —
   - checks out **only** `docker-compose-dev.yml` at the exact validated commit,
   - `scp`s that one file to the EC2 (no source code),
   - over SSH: `sed`s `__REGISTRY__` → the Docker Hub namespace, `docker login`,
     `docker compose pull` (grabs the new `:dev` tag), `up -d`, then prunes.

The dev stack therefore always runs the exact images CI just built and scanned.
Staging/prod follow the same pull-based pattern with their own compose files and tags.

### 5.5 Confirming the pipeline is green (CI badge)

The README carries a live status badge that reads the latest `CI Pipeline` run on
the default branch:

```markdown
[![CI Pipeline](https://github.com/AmaliTech-Training-Academy/deg-cohort-2-community-board-team-4/actions/workflows/ci.yml/badge.svg)](https://github.com/AmaliTech-Training-Academy/deg-cohort-2-community-board-team-4/actions/workflows/ci.yml)
```

- **Green** = the most recent run of `ci.yml` passed.
- The badge image is served live by GitHub; open the linked Actions page to see
  per-stage detail and the run history.

> **Known prerequisites for green** (from `devops/ci/README.md`): the backend
> unit-test step pins `-Dtest=PostServiceTest`, so that test class must exist
> (or broaden to `mvn -B test`); and the data-engineering Dockerfile must build
> via `uv` (it now does). If a badge is red, start there.

---

## 6. Provisioning the cloud (Terraform + Ansible)

Infrastructure as code lives in [devops/infra](devops/infra) (Terraform) and
[devops/ansible](devops/ansible) (config management).

### 6.1 One-time: remote-state backend

```bash
cd devops/infra/backend-bootstrap
terraform init && terraform apply   # creates the S3 bucket for remote state
```

### 6.2 Provision an environment

```bash
cd devops/infra
terraform init -backend-config=modules/dev/backend.hcl
terraform apply -var-file=modules/dev/dev.tfvars   # or test / prod
```

Terraform creates (all in the account's **default VPC / subnets**):
- an **EC2** instance (Amazon Linux 2, `t2.micro` by default) with a public IP,
- a **security group** — SSH (22) from `allowed_ssh_cidr`, plus the frontend
  (3000) and backend (8080) ports open to the world,
- a generated **EC2 key pair** (private key written locally for Ansible),
- a generated **Ansible inventory** from the instance's public IP.

Useful outputs: `instance_public_ip`, `frontend_url`, `backend_url`,
`ssh_private_key_path`, `ansible_inventory_path`.

### 6.3 Install Docker on the host

```bash
cd devops/ansible
ansible-playbook -i inventory.ini main.yml
```

This installs Docker Engine + the Compose V2 plugin (the only thing the host
needs — CD ships the compose file and pulls images). After this, the first push
to `dev` will deploy automatically via `cd-dev.yml`.

---

## 7. Seed data runbook (before a demo)

Sample data is generated by [data-engineering/scripts/seed.py](data-engineering/scripts/seed.py):
**15 users (1 ADMIN), 4 categories, 52 posts, 220 comments**, with `created_at`
spread across the last 30 days so the analytics/trend views have data. Output is
**deterministic** (`random_seed=42`) so every demo looks identical.

### How seeding runs automatically
The data-engineering container seeds on startup **only when `RUN_SEED=true`**
(set in local/dev/staging compose; **off** in prod). It is **idempotent**: it
checks the `comments` count and skips if the bulk seed already ran, so container
restarts never double-seed. `seed.py` also **refuses to run when
`APP_ENV=production`** — prod is guarded twice.

So for the **dev demo host**, seeding already happened on the last `cd-dev`
deploy. The steps below are how to **verify** it and **force a fresh seed**
before a presentation.

### Verify the demo env is seeded (run on the host)
```bash
cd ~/communityboard                      # EC2_DEV_APP_DIR
docker compose -f docker-compose-dev.yml ps          # all services Up/healthy?
docker compose -f docker-compose-dev.yml logs data-engineering | grep -i seed
# Expect: "seeding sample data" + "Seed complete." (or "already seeded; skipping")

# Direct DB check:
docker compose -f docker-compose-dev.yml exec postgres \
  psql -U postgres -d communityboard \
  -c "select (select count(*) from users)    as users,
             (select count(*) from posts)    as posts,
             (select count(*) from comments) as comments;"
```

### Force a clean re-seed before the demo (dev/test only)
```bash
cd ~/communityboard
# Reset the seeded tables and re-insert deterministic data:
docker compose -f docker-compose-dev.yml exec data-engineering \
  python scripts/seed.py --reset
```
`--reset` truncates `comments, posts, categories, users` (restart identity) and
re-seeds in a single transaction. To start from a completely empty volume
instead: `docker compose -f docker-compose-dev.yml down -v && up -d` (re-runs
Flyway migrations, then auto-seeds).

> ⚠️ Never run `seed.py` against production — it writes Faker rows with a shared,
> publicly-known password hash. The script blocks this unless `--force` is given.

---

## 8. Verification & smoke tests

After any deploy:

```bash
# Backend up & schema migrated (200 = Spring context + Flyway ready):
curl -fsS http://<host>:8080/api-docs >/dev/null && echo "backend OK"

# Frontend reachable:
curl -fsS http://<host>:3000 >/dev/null && echo "frontend OK"

# Log in with a seeded account:
curl -fsS -X POST http://<host>:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@amalitech.com","password":"password123"}'
```

Locally, the same checks run against `localhost`. CI runs an equivalent backend
readiness gate in the `qa` job before the API suite.

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Backend container restarts / never healthy | Postgres not ready or Flyway failed | `docker compose logs backend postgres`; ensure DB creds match |
| `manifest blob unknown` on push | provenance/attestation manifest | already disabled in `ci-images.yml` (`provenance:false, sbom:false`) |
| CI badge red on backend | `-Dtest=PostServiceTest` matches nothing | add the test class or use `mvn -B test` |
| QA job fails building data-engineering | stale Dockerfile (`requirements.txt`) | Dockerfile now uses `uv`; re-pull latest |
| CD "Missing required CD config" | a secret/variable absent | set the missing item from the §2 CI/CD table |
| No seed data in demo | `RUN_SEED` off or already-seeded skip | run the §7 re-seed command |
| Prod compose won't start | required secret unset | set `REGISTRY`, `POSTGRES_USER/PASSWORD`, `JWT_SECRET` |

---

_Maintained by the DevOps function. For pipeline internals see
[devops/ci/README.md](devops/ci/README.md); for the AWS topology see
[ARCHITECTURE.md](ARCHITECTURE.md)._
