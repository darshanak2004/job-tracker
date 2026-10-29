# Job Application Tracker

Recruiters post jobs. Candidates apply. Recruiters move applicants through a pipeline
(`APPLIED → UNDER_REVIEW → INTERVIEW → OFFERED / REJECTED`).

| Layer      | Tech                                                                  |
|------------|-----------------------------------------------------------------------|
| Backend    | Spring Boot 3.3, Java 21, Spring Data JPA/Hibernate, Flyway, PostgreSQL 16 |
| Security   | Spring Security, JWT (jjwt), BCrypt, role-based access (RECRUITER / CANDIDATE) |
| Frontend   | Next.js 14 (App Router), TypeScript, Tailwind CSS                      |
| Tests      | JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo · Jest + React Testing Library |
| Delivery   | Multi-stage Dockerfiles, docker compose, GitHub Actions → GHCR         |

> New to all this? Read **[docs/LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md)** - it explains how every piece works.

## Run everything (Docker)

```bash
cp .env.example .env          # optional - sensible defaults exist
docker compose up --build
```

* App: http://localhost:3000
* API: http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html · Health: http://localhost:8080/actuator/health

Stop with `Ctrl+C`; `docker compose down` keeps your data (named volume `pgdata`), `docker compose down -v` wipes it.

## Run for development (no Docker for the app)

```bash
# 1. database only
docker compose up -d db            # Postgres on localhost:5432 (user/pass/db: jobtracker)

# 2. backend  (needs Java 21 + Maven)
cd backend && mvn spring-boot:run

# 3. frontend (needs Node 22)
cd frontend
npm install                        # first time only - creates package-lock.json (commit it!)
npm run dev                        # http://localhost:3000
```

## Tests

```bash
cd backend  && mvn verify          # Checkstyle + unit + MockMvc + Testcontainers + JaCoCo (fails < 70% on services)
                                   # coverage report: backend/target/site/jacoco/index.html
cd frontend && npm test            # Jest + React Testing Library   (npm run test:ci for coverage)
cd frontend && npm run lint
```

The Testcontainers integration test needs Docker running; without Docker it is skipped (not failed).

## API at a glance

| Method & path                          | Who                | What                                        |
|----------------------------------------|--------------------|---------------------------------------------|
| `POST /api/auth/register`              | public             | create account (role = CANDIDATE or RECRUITER), returns JWT |
| `POST /api/auth/login`                 | public             | returns JWT                                 |
| `GET  /api/auth/me`                    | any logged-in user | current user                                |
| `GET  /api/jobs?keyword=&page=&size=`  | any logged-in user | browse OPEN jobs (paged)                    |
| `GET  /api/jobs/{id}`                  | any logged-in user | one job                                     |
| `GET  /api/jobs/mine`                  | RECRUITER          | my postings                                 |
| `POST /api/jobs`                       | RECRUITER          | create posting                              |
| `PUT  /api/jobs/{id}`                  | RECRUITER (owner)  | update posting (also open/close it)         |
| `DELETE /api/jobs/{id}`                | RECRUITER (owner)  | delete posting                              |
| `POST /api/jobs/{id}/applications`     | CANDIDATE          | apply (409 if already applied, 422 if closed) |
| `GET  /api/jobs/{id}/applications`     | RECRUITER (owner)  | applicants for my job                       |
| `GET  /api/applications/mine`          | CANDIDATE          | my applications                             |
| `PATCH /api/applications/{id}/status`  | RECRUITER (owner)  | move applicant through the pipeline         |
| `DELETE /api/applications/{id}`        | CANDIDATE (owner)  | withdraw                                    |

Errors always look like `{ "timestamp", "status", "error", "message", "fieldErrors"? }`.

## CI/CD

`.github/workflows/ci.yml` runs on every push and PR:

1. **Backend** - Checkstyle lint, tests, JaCoCo gate (`mvn verify`)
2. **Frontend** - ESLint, Jest, `next build`
3. **Docker Compose smoke test** - `docker compose up --build --wait`, then hits the API and UI
4. **Docker images** - builds both images; **pushes to GitHub Container Registry only on merge to `main`**

### One-time GitHub setup

1. Push the repo to GitHub. Make sure `frontend/package-lock.json` is committed (run `npm install` in `frontend/` once).
2. *Settings → Actions → General → Workflow permissions* → "Read and write permissions" (lets the pipeline push to GHCR).
3. After the first PR run, protect `main` - either run `./scripts/protect-main.sh OWNER/REPO`
   or use *Settings → Branches → Add branch protection rule* for `main`:
   * Require a pull request before merging (1 approval)
   * Require status checks to pass: **Backend**, **Frontend**, **Docker Compose smoke test**
   * Require branches to be up to date, include administrators
   (Branch protection on private repos needs a paid GitHub plan; public repos are free.)
4. Optional: set the repo variable `NEXT_PUBLIC_API_URL` (Settings → Variables) to your deployed API URL for the pushed frontend image.

## Project layout

```
backend/   Spring Boot app  (controller → service → repository → entity, DTOs, security, exception)
frontend/  Next.js app      (app/ pages, components/, lib/ api client + auth context, __tests__/)
docs/      LEARNING_GUIDE.md
scripts/   protect-main.sh
docker-compose.yml  .env.example  .github/workflows/ci.yml
```
