# How this project works (a guided tour)

Read this top to bottom once, then keep it open while you read the code. Every section names the
files to open. The goal: after reading you can explain **any** line of this project in a review.

---

## 1. The big picture

```
Browser (Next.js, :3000)  ──HTTP + JSON + "Authorization: Bearer <JWT>"──►  Spring Boot API (:8080)
                                                                                   │ JPA / Hibernate
                                                                                   ▼
                                                                            PostgreSQL (:5432)
```

* The **frontend** is only a client. It never touches the database; it calls the REST API.
* The **backend** owns all rules: who you are, what you may do, what is valid.
  *Never trust the frontend* - hiding a button is UX, not security. Every rule is enforced again on the server.
* The **database** stores the data and also guards integrity (unique + foreign-key constraints).

---

## 2. Backend: layered architecture

```
Controller  →  Service  →  Repository  →  Database
 (HTTP)       (business     (SQL via
              rules)         Spring Data)
```

| Layer | Package | Job | Must NOT |
|-------|---------|-----|----------|
| Controller | `controller` | Parse the HTTP request, validate the body (`@Valid`), call a service, choose the status code | contain business rules |
| Service | `service` | Business logic, ownership checks, transactions | know about HTTP |
| Repository | `repository` | Talk to the DB | contain business rules |
| Entity | `entity` | Java objects mapped to tables | be returned from the API |
| DTO | `dto` | The *shape of JSON* going in and out | |
| Mapper | `mapper` | Entity → DTO | |
| Exception | `exception` | Domain errors + one handler turning them into HTTP responses | |
| Security / Config | `security`, `config` | JWT, filter chain, CORS, BCrypt | |

**Why layers?** Each layer can be tested alone (see §8) and changed without breaking the others.
**Why DTOs?** If you returned the `User` entity you would leak the password hash, and any DB change would
silently change your API. DTOs (`record`s here) are an explicit contract.

---

## 3. The database

Open `backend/src/main/resources/db/migration/V1__init_schema.sql`.

```
users (id, name, email UNIQUE, password, role)
   │ 1
   │            recruiter_id
   └──────< job_postings (id, title, company, location, description, status, ...)
   │                       │ 1
   │ 1       candidate_id  └──────< applications (id, job_id, candidate_id, status, cover_letter,
   └──────────────────────────────<                UNIQUE(job_id, candidate_id))
```

* **One-to-many everywhere**: a recruiter has many jobs; a job has many applications; a candidate has many applications.
* **`UNIQUE (job_id, candidate_id)`** is the real "no duplicate applications" guarantee. Code can have bugs or race
  conditions; a database constraint cannot be bypassed.
* **Flyway** runs `V1__...sql` on startup and records it. Schema changes become new files (`V2__...`) - never edit an
  old one. Hibernate is set to `ddl-auto: validate`, which means "check the entities match the tables, but don't
  touch the tables". Schema is *code-reviewed SQL*, not something Hibernate guesses.
* Entities (`entity/*.java`): `@ManyToOne(fetch = LAZY)` means the related row is loaded only when used. Repositories use
  `@EntityGraph` to load what we know we'll need in the **same** query - this avoids the classic **N+1 query** problem.

---

## 4. A request, step by step

Follow `POST /api/jobs/10/applications` (a candidate applies):

1. **Browser** → `ApplyForm.tsx` calls `applyToJob()` in `lib/api.ts`, which sends the JWT in the `Authorization` header.
2. **`JwtAuthFilter`** (runs first on every request) reads the header, `JwtService` verifies the signature + expiry,
   loads the user, and stores an `AuthUser` in the `SecurityContext` ("this request is user #2, ROLE_CANDIDATE").
   Bad/missing token → nothing is stored → Spring answers **401**.
3. **`SecurityConfig`** rules: `/api/auth/login|register` are public; everything else needs authentication.
4. **`ApplicationController.apply`** has `@PreAuthorize("hasRole('CANDIDATE')")`. A recruiter calling this gets **403**.
   `@AuthenticationPrincipal AuthUser user` hands the controller the logged-in user. `@Valid` checks the body.
5. **`ApplicationService.apply`** (the business logic):
   * job exists? else `ResourceNotFoundException` → 404
   * job is OPEN? else `BusinessRuleException` → 422
   * already applied? else `DuplicateResourceException` → 409
   * save. If two requests race, the DB unique constraint throws `DataIntegrityViolationException`;
     we catch it and translate to the same 409.
6. **`GlobalExceptionHandler`** converts any exception into the same JSON error shape.
7. The service returns an `ApplicationResponse` DTO → Jackson turns it into JSON → **201 Created**.

### Ownership checks (the interesting security idea)

*Roles* answer "may this **kind** of user do this?" (`@PreAuthorize`). *Ownership* answers "may **this** user touch
**this** row?" - that needs the data, so it lives in the service:

```java
if (!job.getRecruiter().getId().equals(recruiterId)) throw new ForbiddenException(...)
```
Without it, any recruiter could edit any other recruiter's job just by guessing an id (an **IDOR** bug).
See `JobPostingService.assertOwner`, `ApplicationService.listForJob / updateStatus / withdraw`.

### Validation vs. business rules
* **Validation** = is the input well-formed? Annotations on DTOs (`@NotBlank`, `@Email`, `@Size`) → **400** with `fieldErrors`.
* **Business rules** = is the request *allowed* given current state? Done in services → **404 / 409 / 422 / 403**.

---

## 5. Authentication & JWT

**Register** (`AuthService.register`): email lower-cased, password hashed with **BCrypt**
(`passwordEncoder.encode`) - a slow, salted one-way hash. We store only the hash; on login Spring compares
the typed password against it. Plain-text passwords are never stored or logged.

**Login** (`AuthService.login`): `AuthenticationManager.authenticate(...)` loads the user through
`CustomUserDetailsService` and checks BCrypt. Wrong password → `BadCredentialsException` → 401.
On success we build a **JWT** with `JwtService`.

**What is a JWT?** Three Base64 parts: `header.payload.signature`. The payload carries claims
(`sub`=email, `uid`, `role`, `exp`). The signature is an HMAC of header+payload with a secret only the server knows.
So the server can verify "I issued this, nobody changed it" **without a session table** → the API is **stateless**
(`SessionCreationPolicy.STATELESS`) and scales horizontally.
*The payload is encoded, not encrypted - never put secrets in it.*

**CSRF is disabled** because we don't use cookies for auth - the browser doesn't attach the token automatically,
our JS does.

**CORS**: the page at `localhost:3000` calls an API at `localhost:8080` - a different *origin*, which browsers block
unless the API says it's OK. `SecurityConfig.corsConfigurationSource` whitelists the frontend origin (env var `CORS_ALLOWED_ORIGINS`).

**Frontend side** (`lib/auth-context.tsx`): after login the token is saved in `localStorage` and the user kept in React
context. On refresh the provider calls `/api/auth/me` to restore (and validate) the session.
`RequireAuth` redirects anonymous users to `/login` and blocks wrong roles.
> Trade-off to know for interviews: `localStorage` is simple but readable by any XSS-injected script.
> Production-grade apps often use an **httpOnly cookie** instead. Good "what would you improve?" answer.

---

## 6. Frontend (Next.js App Router)

* `src/app/**/page.tsx` - each folder is a URL (`app/jobs/[id]/page.tsx` → `/jobs/5`; `[id]` is a dynamic segment read with `useParams`).
* `'use client'` marks components that use state/effects/browser APIs. Our pages are client components because they fetch
  with the user's token from the browser.
* `src/lib/api.ts` is the **only** place that calls `fetch`. It adds headers, parses JSON, and converts error responses
  into `ApiError(status, message, fieldErrors)` so components can show server messages.
* `src/components/` are small, testable pieces: forms (`LoginForm`, `RegisterForm`, `JobForm`, `ApplyForm`), display
  (`JobCard`, `StatusBadge`, `ApplicantCard`) and guards (`RequireAuth`).
* Pattern in forms: **controlled inputs** (`value` + `onChange`), validate on submit, show field errors, disable the button
  while submitting, show the server's error message in a `role="alert"` box.

Role-based UI: candidates see *Apply* and *My applications*; recruiters see *My postings*, *Applicants*, *Edit*.
Remember: this is convenience only; the backend enforces the same rules.

---

## 7. Docker

**Multi-stage build** = use a fat image to *build*, copy only the result into a small image to *run*.

* `backend/Dockerfile`: Maven+JDK image compiles the jar → runtime image has only the JRE + the jar, runs as a non-root user.
  Copying `pom.xml` first lets Docker **cache the dependency download layer** until the POM changes.
* `frontend/Dockerfile`: `deps` → `builder` (`next build` with `output: 'standalone'`) → tiny `runner` with just `server.js`.
  `NEXT_PUBLIC_API_URL` is a **build arg** because Next bakes `NEXT_PUBLIC_*` into the browser bundle at build time.
* `docker-compose.yml` wires three services on one private network (they reach each other by service name: the backend
  connects to `db:5432`, not `localhost`).
  * **healthchecks** (`pg_isready`, `/actuator/health`, `wget`) + `depends_on: condition: service_healthy` = correct start
    order: DB ready → backend starts & migrates → frontend starts.
  * **volume** `pgdata` keeps the database across restarts.
  * **`.env`** supplies passwords/secrets; `${VAR:-default}` gives a fallback. `.env` is git-ignored, `.env.example` is committed.

---

## 8. Testing - what each kind is for

| Kind | Files | Speed | Proves |
|------|-------|-------|--------|
| **Unit** (JUnit 5 + Mockito) | `service/*Test`, `security/JwtServiceTest` | ms | one class's logic, dependencies replaced by mocks |
| **Controller** (`@WebMvcTest` + MockMvc) | `controller/*Test` | fast | URLs, status codes, JSON, validation, **role rules** - without a DB |
| **Integration** (Testcontainers) | `integration/JobTrackerIntegrationTest` | slow | everything together on a **real PostgreSQL** in Docker: Flyway migration, JPA queries, unique constraint, real JWTs |
| **Component** (Jest + RTL) | `frontend/src/__tests__` | fast | UI behaviour the way a user sees it |

* **Mockito** pattern (Arrange / Act / Assert): `when(repo.findById(1L)).thenReturn(...)` → call the service →
  `assertThat(...)` / `verify(repo, never()).save(any())`.
* **MockMvc** lets you "send" HTTP requests to controllers in-process. `.with(user(...))` fakes a logged-in user, so we can
  assert "candidate → POST /api/jobs → 403".
* **Testcontainers** starts a throwaway `postgres:16-alpine` per test run; `@ServiceConnection` points Spring at it.
  This catches problems that mocks can't (e.g. a wrong column type).
* **JaCoCo** measures which lines tests executed. `pom.xml` fails `mvn verify` if **service-layer line coverage < 70%**.
  Open `backend/target/site/jacoco/index.html` after a run.
* **React Testing Library** philosophy: query by what the user sees (`getByLabelText('Email')`,
  `getByRole('button', {name: 'Log in'})`), act with `userEvent`, assert on visible results - not on component internals.

Tip: *Test the behaviour you'd be embarrassed to break* - ownership checks, duplicate prevention, role rules, validation.

---

## 9. CI/CD

`.github/workflows/ci.yml` - GitHub runs this on every push/PR:

1. `Backend` and `Frontend` run in parallel (build → lint → test).
2. `Docker Compose smoke test` runs `docker compose up --build --wait` and curls the live system - proof that the
   *assembled* app works, not just the parts.
3. `Docker images` builds both images; **only on a push to `main`** (i.e., after a PR is merged) it logs in to GHCR with the built-in
   `GITHUB_TOKEN` and pushes `latest` + a commit-SHA tag.
4. **Branch protection** (`scripts/protect-main.sh`) makes the pipeline binding: nobody pushes straight to `main`; a PR
   needs review and the three checks green. That's what turns CI from "advice" into "gate".

Typical workflow: `git switch -c feature/x` → commit → push → open PR → wait for green checks → review → merge → images publish.

---

## 10. Try it yourself (exercises, easiest first)

1. Register a recruiter and a candidate in two browsers; post a job, apply, change the status, watch it update.
2. In Swagger UI (`/swagger-ui.html`) click *Authorize*, paste a token, and call an endpoint that your role can't use. Read the 403.
3. Decode a token at jwt.io. Which claims do you see? Change one character - what does the API answer?
4. Break the rules on purpose: remove the ownership check in `JobPostingService.update`, run `mvn test` - which test fails? (That is the test doing its job.) Put it back.
5. Add a field `experienceYears` to `JobPosting`: new Flyway file `V2__...sql`, entity, DTOs, form field, and a test.
6. Add `GET /api/jobs/mine` pagination, or an application count per job.
7. Add a `Role.ADMIN` who can delete any job (think: where do the ownership checks change?).
8. Move the JWT from `localStorage` to an httpOnly cookie. What else has to change (CORS credentials, CSRF)?

## 11. Questions you should be able to answer

* Why DTOs instead of returning entities? * Authentication vs authorization? Role check vs ownership check?
* Why is the API stateless, and what does the JWT signature guarantee? * Why BCrypt and not SHA-256?
* How do you prevent duplicate applications - and why both a code check *and* a DB constraint?
* What is the N+1 problem and how did we avoid it? * `ddl-auto=validate` + Flyway vs `update`?
* Unit vs controller vs integration test? * What does 70% coverage tell you - and not tell you?
* Why multi-stage Docker builds? * Why `depends_on: service_healthy`? * What does branch protection add on top of CI?
