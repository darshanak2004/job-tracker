-- users 1 --- * job_postings 1 --- * applications * --- 1 users
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(255)  NOT NULL UNIQUE,
    password    VARCHAR(100)  NOT NULL,           -- BCrypt hash, never plain text
    role        VARCHAR(20)   NOT NULL CHECK (role IN ('CANDIDATE', 'RECRUITER')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE job_postings (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200)  NOT NULL,
    company       VARCHAR(200)  NOT NULL,
    location      VARCHAR(200)  NOT NULL,
    description   TEXT          NOT NULL,
    salary_range  VARCHAR(100),
    status        VARCHAR(20)   NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    recruiter_id  BIGINT        NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_postings_recruiter ON job_postings (recruiter_id);
CREATE INDEX idx_job_postings_status    ON job_postings (status);

CREATE TABLE applications (
    id            BIGSERIAL PRIMARY KEY,
    job_id        BIGINT       NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    candidate_id  BIGINT       NOT NULL REFERENCES users (id),
    cover_letter  TEXT,
    status        VARCHAR(20)  NOT NULL
                  CHECK (status IN ('APPLIED', 'UNDER_REVIEW', 'INTERVIEW', 'OFFERED', 'REJECTED')),
    applied_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- The database itself guarantees "one application per candidate per job"
    CONSTRAINT uq_application_job_candidate UNIQUE (job_id, candidate_id)
);
CREATE INDEX idx_applications_candidate ON applications (candidate_id);
