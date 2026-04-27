-- V4 (test only): Adjust seed data so integration tests pass.
-- V3 seeded creator-1 sales in Jan/Feb 2025; tests expect them in March 2025.
-- Uses UPDATE only — no DELETE, no TRUNCATE.

UPDATE sale_records SET amount = 50000,
    paid_at = TIMESTAMP WITH TIME ZONE '2025-03-05 01:00:00+00'
WHERE id = 'sale-1';

-- sale-2: move to course-1, March 2025 KST
UPDATE sale_records SET course_id = 'course-1', amount = 50000,
    paid_at = TIMESTAMP WITH TIME ZONE '2025-03-15 05:30:00+00'
WHERE id = 'sale-2';

-- sale-3: move to course-2, March 2025 KST
UPDATE sale_records SET course_id = 'course-2',
    paid_at = TIMESTAMP WITH TIME ZONE '2025-03-20 00:00:00+00'
WHERE id = 'sale-3';

-- sale-4: move to course-2 (creator-1), March 2025 KST
UPDATE sale_records SET course_id = 'course-2', student_id = 'student-4', amount = 80000,
    paid_at = TIMESTAMP WITH TIME ZONE '2025-03-22 02:00:00+00'
WHERE id = 'sale-4';

-- sale-5: move to course-3 (creator-2), Jan 31 14:30Z = within Jan KST (boundary before 15:00Z)
UPDATE sale_records SET course_id = 'course-3', student_id = 'student-5',
    paid_at = TIMESTAMP WITH TIME ZONE '2025-01-31 14:30:00+00'
WHERE id = 'sale-5';

-- sale-6: move to March 2025 KST (used in creator-2 March settlement test)
UPDATE sale_records SET student_id = 'student-6', amount = 60000,
    paid_at = TIMESTAMP WITH TIME ZONE '2025-03-10 07:00:00+00'
WHERE id = 'sale-6';

-- sale-7: Feb 2025 KST
UPDATE sale_records SET student_id = 'student-7', amount = 120000,
    paid_at = TIMESTAMP WITH TIME ZONE '2025-02-14 01:00:00+00'
WHERE id = 'sale-7';

-- cancel-1, cancel-2: move to March 2025 KST so they count in creator-1 March settlement
UPDATE cancel_records SET cancelled_at = TIMESTAMP WITH TIME ZONE '2025-03-25 00:00:00+00'
WHERE id = 'cancel-1';

UPDATE cancel_records SET cancelled_at = TIMESTAMP WITH TIME ZONE '2025-03-27 00:00:00+00'
WHERE id = 'cancel-2';

-- cancel-3 stays at 2025-02-03T01:00Z (Feb KST, tested in month_boundary_cancel_february)
