package com.example.settlement.domain.common.exception;

public enum ErrorCode {

    CREATOR_NOT_FOUND(404, "CR001", "크리에이터를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(404, "CO001", "강의를 찾을 수 없습니다."),
    SALE_NOT_FOUND(404, "SA001", "판매 내역을 찾을 수 없습니다."),
    ALREADY_CANCELLED(409, "SA002", "이미 취소된 판매 내역입니다."),
    REFUND_EXCEEDS_PAYMENT(400, "SA003", "환불 금액이 원결제 금액을 초과할 수 없습니다."),
    INVALID_YEAR_MONTH(400, "ST001", "올바르지 않은 연월 형식입니다. (예: 2025-03)"),
    SETTLEMENT_NOT_FOUND(404, "ST002", "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_ALREADY_EXISTS(409, "ST003", "해당 기간의 정산이 이미 존재합니다."),
    INVALID_STATUS_TRANSITION(409, "ST004", "유효하지 않은 상태 전이입니다."),
    INVALID_DATE_RANGE(400, "AD001", "시작일이 종료일보다 늦을 수 없습니다."),
    INVALID_PAID_AT(400, "SA004", "미래 시점의 결제 일시는 등록할 수 없습니다."),
    INVALID_REFUND_AMOUNT(400, "SA005", "환불 금액은 0보다 커야 합니다."),
    FEE_POLICY_NOT_FOUND(500, "FP001", "해당 기간의 수수료 정책을 찾을 수 없습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;

    ErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
