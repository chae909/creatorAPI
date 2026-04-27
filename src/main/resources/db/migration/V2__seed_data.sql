-- Creators
INSERT INTO creators (id, name, email) VALUES
    (1, '김앨리스', 'alice@example.com'),
    (2, '이밥',    'bob@example.com'),
    (3, '박찰리',  'charlie@example.com');

-- Courses
INSERT INTO courses (id, creator_id, title, price) VALUES
    (1, 1, 'Java 입문',          100000),
    (2, 1, 'Spring Boot 심화',    80000),
    (3, 2, '데이터베이스 기초',   120000),
    (4, 3, '파이썬 머신러닝',     150000);

-- Sale records (KST → UTC, KST = UTC+9)
-- sale-1: 2025-01-10T10:00:00+09:00 → 2025-01-10T01:00:00Z
-- sale-2: 2025-01-20T15:00:00+09:00 → 2025-01-20T06:00:00Z
-- sale-3: 2025-01-25T12:00:00+09:00 → 2025-01-25T03:00:00Z  (cancel-1 전액 80000)
-- sale-4: 2025-01-28T09:00:00+09:00 → 2025-01-28T00:00:00Z  (cancel-2 부분 30000)
-- sale-5: 2025-02-01T11:00:00+09:00 → 2025-02-01T02:00:00Z  (cancel-3)
-- sale-6: 2025-02-10T16:00:00+09:00 → 2025-02-10T07:00:00Z
-- sale-7: 2025-02-15T13:00:00+09:00 → 2025-02-15T04:00:00Z
INSERT INTO sale_records (id, creator_id, course_id, amount, sold_at) VALUES
    (1, 1, 1, 100000, TIMESTAMP WITH TIME ZONE '2025-01-10 01:00:00+00'),
    (2, 1, 2,  50000, TIMESTAMP WITH TIME ZONE '2025-01-20 06:00:00+00'),
    (3, 1, 1,  80000, TIMESTAMP WITH TIME ZONE '2025-01-25 03:00:00+00'),
    (4, 2, 3, 120000, TIMESTAMP WITH TIME ZONE '2025-01-28 00:00:00+00'),
    (5, 1, 2,  60000, TIMESTAMP WITH TIME ZONE '2025-02-01 02:00:00+00'),
    (6, 2, 3,  90000, TIMESTAMP WITH TIME ZONE '2025-02-10 07:00:00+00'),
    (7, 3, 4, 150000, TIMESTAMP WITH TIME ZONE '2025-02-15 04:00:00+00');

-- Cancel records
-- cancel-1: sale-3 전액 80000, cancelled_at: 2025-01-26T10:00:00+09:00 → 2025-01-26T01:00:00Z
-- cancel-2: sale-4 부분 30000, cancelled_at: 2025-01-29T14:00:00+09:00 → 2025-01-29T05:00:00Z
-- cancel-3: sale-5 전액 60000, cancelled_at: 2025-02-03T10:00:00+09:00 → 2025-02-03T01:00:00Z
INSERT INTO cancel_records (id, sale_record_id, refund_amount, cancelled_at) VALUES
    (1, 3, 80000, TIMESTAMP WITH TIME ZONE '2025-01-26 01:00:00+00'),
    (2, 4, 30000, TIMESTAMP WITH TIME ZONE '2025-01-29 05:00:00+00'),
    (3, 5, 60000, TIMESTAMP WITH TIME ZONE '2025-02-03 01:00:00+00');

-- Fee policy: 20% from 2020-01-01
INSERT INTO fee_policies (id, fee_rate, effective_from) VALUES
    (1, 0.2000, DATE '2020-01-01');
