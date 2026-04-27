-- 판매 내역 월별 조회 최적화 (가장 빈번한 쿼리)
CREATE INDEX idx_sale_records_course_paid
    ON sale_records(course_id, paid_at);

-- 취소 내역 월별 조회 최적화
CREATE INDEX idx_cancel_records_cancelled_at
    ON cancel_records(cancelled_at);

-- 정산 조회 최적화
CREATE INDEX idx_settlements_creator_year_month
    ON settlements(creator_id, year, month);

-- 수수료 정책 이력 조회 최적화
CREATE INDEX idx_fee_policies_effective_from
    ON fee_policies(effective_from DESC);
