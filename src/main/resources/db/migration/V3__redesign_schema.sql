-- V3: Redesign — String PKs for business entities, new fields for settlement
-- Drops V1 schema and replaces with entity-aligned schema.
-- H2 in-memory restarts apply V1→V2→V3 in sequence; V3 wins.

DROP TABLE IF EXISTS settlements    CASCADE;
DROP TABLE IF EXISTS cancel_records CASCADE;
DROP TABLE IF EXISTS sale_records   CASCADE;
DROP TABLE IF EXISTS fee_policies   CASCADE;
DROP TABLE IF EXISTS courses        CASCADE;
DROP TABLE IF EXISTS creators       CASCADE;

CREATE TABLE creators (
    id         VARCHAR(36)                 PRIMARY KEY,
    name       VARCHAR(100)                NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE courses (
    id         VARCHAR(36)                 PRIMARY KEY,
    creator_id VARCHAR(36)                 NOT NULL REFERENCES creators(id),
    title      VARCHAR(200)                NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE sale_records (
    id         VARCHAR(36)                 PRIMARY KEY,
    course_id  VARCHAR(36)                 NOT NULL REFERENCES courses(id),
    student_id VARCHAR(36)                 NOT NULL,
    amount     BIGINT                      NOT NULL,
    paid_at    TIMESTAMP WITH TIME ZONE    NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE cancel_records (
    id             VARCHAR(36)                 PRIMARY KEY,
    sale_record_id VARCHAR(36)                 NOT NULL UNIQUE REFERENCES sale_records(id),
    refund_amount  BIGINT                      NOT NULL,
    cancelled_at   TIMESTAMP WITH TIME ZONE    NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE fee_policies (
    id             BIGSERIAL                   PRIMARY KEY,
    fee_rate       DECIMAL(5,4)                NOT NULL,
    effective_from DATE                        NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE settlements (
    id               BIGSERIAL                   PRIMARY KEY,
    creator_id       VARCHAR(36)                 NOT NULL REFERENCES creators(id),
    year             INT                         NOT NULL,
    month            INT                         NOT NULL,
    status           VARCHAR(20)                 NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID')),
    total_sales      BIGINT                      NOT NULL DEFAULT 0,
    total_refunds    BIGINT                      NOT NULL DEFAULT 0,
    net_sales        BIGINT                      NOT NULL DEFAULT 0,
    fee_rate         DECIMAL(5,4)                NOT NULL,
    fee_amount       BIGINT                      NOT NULL DEFAULT 0,
    payout_amount    BIGINT                      NOT NULL DEFAULT 0,
    sale_count       INT                         NOT NULL DEFAULT 0,
    cancel_count     INT                         NOT NULL DEFAULT 0,
    confirmed_at     TIMESTAMP WITH TIME ZONE,
    paid_at          TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    UNIQUE (creator_id, year, month)
);

-- Seed data (String IDs; timestamps preserve V2 KST→UTC conversions)
INSERT INTO creators (id, name) VALUES
    ('creator-1', '김앨리스'),
    ('creator-2', '이밥'),
    ('creator-3', '박찰리');

INSERT INTO courses (id, creator_id, title) VALUES
    ('course-1', 'creator-1', 'Java 입문'),
    ('course-2', 'creator-1', 'Spring Boot 심화'),
    ('course-3', 'creator-2', '데이터베이스 기초'),
    ('course-4', 'creator-3', '파이썬 머신러닝');

-- sale-1: 2025-01-10T10:00+09 → 01:00Z
-- sale-2: 2025-01-20T15:00+09 → 06:00Z
-- sale-3: 2025-01-25T12:00+09 → 03:00Z  (cancel-1 전액 80000)
-- sale-4: 2025-01-28T09:00+09 → 00:00Z  (cancel-2 부분 30000)
-- sale-5: 2025-02-01T11:00+09 → 02:00Z  (cancel-3 전액 60000)
-- sale-6: 2025-02-10T16:00+09 → 07:00Z
-- sale-7: 2025-02-15T13:00+09 → 04:00Z
INSERT INTO sale_records (id, course_id, student_id, amount, paid_at) VALUES
    ('sale-1', 'course-1', 'student-1', 100000, TIMESTAMP WITH TIME ZONE '2025-01-10 01:00:00+00'),
    ('sale-2', 'course-2', 'student-2',  50000, TIMESTAMP WITH TIME ZONE '2025-01-20 06:00:00+00'),
    ('sale-3', 'course-1', 'student-3',  80000, TIMESTAMP WITH TIME ZONE '2025-01-25 03:00:00+00'),
    ('sale-4', 'course-3', 'student-1', 120000, TIMESTAMP WITH TIME ZONE '2025-01-28 00:00:00+00'),
    ('sale-5', 'course-2', 'student-4',  60000, TIMESTAMP WITH TIME ZONE '2025-02-01 02:00:00+00'),
    ('sale-6', 'course-3', 'student-2',  90000, TIMESTAMP WITH TIME ZONE '2025-02-10 07:00:00+00'),
    ('sale-7', 'course-4', 'student-5', 150000, TIMESTAMP WITH TIME ZONE '2025-02-15 04:00:00+00');

-- cancel-1: 2025-01-26T10:00+09 → 01:00Z
-- cancel-2: 2025-01-29T14:00+09 → 05:00Z
-- cancel-3: 2025-02-03T10:00+09 → 01:00Z
INSERT INTO cancel_records (id, sale_record_id, refund_amount, cancelled_at) VALUES
    ('cancel-1', 'sale-3', 80000, TIMESTAMP WITH TIME ZONE '2025-01-26 01:00:00+00'),
    ('cancel-2', 'sale-4', 30000, TIMESTAMP WITH TIME ZONE '2025-01-29 05:00:00+00'),
    ('cancel-3', 'sale-5', 60000, TIMESTAMP WITH TIME ZONE '2025-02-03 01:00:00+00');

INSERT INTO fee_policies (fee_rate, effective_from) VALUES
    (0.2000, DATE '2020-01-01');
