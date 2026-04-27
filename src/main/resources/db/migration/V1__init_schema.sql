CREATE TABLE creators (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)                NOT NULL,
    email      VARCHAR(200)                NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE courses (
    id         BIGSERIAL PRIMARY KEY,
    creator_id BIGINT                      NOT NULL REFERENCES creators(id),
    title      VARCHAR(200)                NOT NULL,
    price      BIGINT                      NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE TABLE sale_records (
    id         BIGSERIAL PRIMARY KEY,
    creator_id BIGINT                      NOT NULL REFERENCES creators(id),
    course_id  BIGINT                      NOT NULL REFERENCES courses(id),
    amount     BIGINT                      NOT NULL,
    sold_at    TIMESTAMP WITH TIME ZONE    NOT NULL
);

CREATE TABLE cancel_records (
    id             BIGSERIAL PRIMARY KEY,
    sale_record_id BIGINT                      NOT NULL UNIQUE REFERENCES sale_records(id),
    refund_amount  BIGINT                      NOT NULL,
    cancelled_at   TIMESTAMP WITH TIME ZONE    NOT NULL
);

CREATE TABLE fee_policies (
    id             BIGSERIAL PRIMARY KEY,
    fee_rate       DECIMAL(5,4)    NOT NULL,
    effective_from DATE            NOT NULL
);

CREATE TABLE settlements (
    id                BIGSERIAL PRIMARY KEY,
    creator_id        BIGINT                      NOT NULL REFERENCES creators(id),
    year              INT                         NOT NULL,
    month             INT                         NOT NULL,
    total_sales       BIGINT                      NOT NULL DEFAULT 0,
    total_refunds     BIGINT                      NOT NULL DEFAULT 0,
    net_sales         BIGINT                      NOT NULL DEFAULT 0,
    fee_rate          DECIMAL(5,4)                NOT NULL,
    fee_amount        BIGINT                      NOT NULL DEFAULT 0,
    settlement_amount BIGINT                      NOT NULL DEFAULT 0,
    status            VARCHAR(20)                 NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID')),
    confirmed_at      TIMESTAMP WITH TIME ZONE,
    paid_at           TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    UNIQUE (creator_id, year, month)
);
