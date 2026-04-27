package com.example.settlement.domain.common.exception;

public enum ErrorCode {

    CREATOR_NOT_FOUND("크리에이터를 찾을 수 없습니다."),
    COURSE_NOT_FOUND("강의를 찾을 수 없습니다."),
    SALE_NOT_FOUND("판매 내역을 찾을 수 없습니다."),
    ALREADY_CANCELLED("이미 취소된 판매입니다."),
    REFUND_EXCEEDS_PAYMENT("환불 금액이 결제 금액을 초과합니다."),
    INVALID_YEAR_MONTH("유효하지 않은 연월입니다."),
    SETTLEMENT_ALREADY_EXISTS("해당 월 정산이 이미 존재합니다."),
    INVALID_STATUS_TRANSITION("유효하지 않은 상태 전환입니다."),
    SETTLEMENT_NOT_FOUND("정산 내역을 찾을 수 없습니다."),
    INVALID_DATE_RANGE("유효하지 않은 날짜 범위입니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
