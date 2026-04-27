-- V4: Replace seed data so timestamps and course ownership match integration test expectations.
-- V3 seed put all creator-1 sales in Jan/Feb. The DataLoader (and tests) expect them in March 2025.
-- Safe delete order respects FK constraints.

DELETE FROM cancel_records;
DELETE FROM settlements;
DELETE FROM sale_records;
DELETE FROM courses;
DELETE FROM creators;
DELETE FROM fee_policies;

INSERT INTO creators (id, name) VALUES
    ('creator-1', '김앨리스'),
    ('creator-2', '이밥'),
    ('creator-3', '박찰리');

INSERT INTO courses (id, creator_id, title) VALUES
    ('course-1', 'creator-1', 'Java 입문'),
    ('course-2', 'creator-1', 'Spring Boot 심화'),
    ('course-3', 'creator-2', '데이터베이스 기초'),
    ('course-4', 'creator-3', '파이썬 머신러닝');

-- creator-1: 4 sales in March 2025 KST (range 2025-02-28T15:00Z ~ 2025-03-31T15:00Z)
-- sale-1: 2025-03-05T10:00+09 = 01:00Z  sale-2: 2025-03-15T14:30+09 = 05:30Z
-- sale-3: 2025-03-20T09:00+09 = 00:00Z  sale-4: 2025-03-22T11:00+09 = 02:00Z
-- creator-2: sale-5 on 2025-01-31T23:30+09 = 14:30Z (within Jan KST, 14:30 < 15:00 boundary)
-- creator-2: sale-6 in March 2025 KST (used for creator-2 March settlement in @Order(9))
-- creator-3: sale-7 in Feb 2025 KST
INSERT INTO sale_records (id, course_id, student_id, amount, paid_at) VALUES
    ('sale-1', 'course-1', 'student-1',  50000, TIMESTAMP WITH TIME ZONE '2025-03-05 01:00:00+00'),
    ('sale-2', 'course-1', 'student-2',  50000, TIMESTAMP WITH TIME ZONE '2025-03-15 05:30:00+00'),
    ('sale-3', 'course-2', 'student-3',  80000, TIMESTAMP WITH TIME ZONE '2025-03-20 00:00:00+00'),
    ('sale-4', 'course-2', 'student-4',  80000, TIMESTAMP WITH TIME ZONE '2025-03-22 02:00:00+00'),
    ('sale-5', 'course-3', 'student-5',  60000, TIMESTAMP WITH TIME ZONE '2025-01-31 14:30:00+00'),
    ('sale-6', 'course-3', 'student-6',  60000, TIMESTAMP WITH TIME ZONE '2025-03-10 07:00:00+00'),
    ('sale-7', 'course-4', 'student-7', 120000, TIMESTAMP WITH TIME ZONE '2025-02-14 01:00:00+00');

-- cancel-1, cancel-2: cancelled in March 2025 KST → counted in creator-1 March settlement
-- cancel-3: cancelled 2025-02-03T10:00+09 = 01:00Z (Feb KST) on sale-5 (Jan sale) → creator-2 Feb refund
INSERT INTO cancel_records (id, sale_record_id, refund_amount, cancelled_at) VALUES
    ('cancel-1', 'sale-3', 80000, TIMESTAMP WITH TIME ZONE '2025-03-25 00:00:00+00'),
    ('cancel-2', 'sale-4', 30000, TIMESTAMP WITH TIME ZONE '2025-03-27 00:00:00+00'),
    ('cancel-3', 'sale-5', 60000, TIMESTAMP WITH TIME ZONE '2025-02-03 01:00:00+00');

INSERT INTO fee_policies (fee_rate, effective_from) VALUES
    (0.2000, '2020-01-01');
