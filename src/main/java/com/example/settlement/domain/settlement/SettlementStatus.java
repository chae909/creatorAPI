package com.example.settlement.domain.settlement;

import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;

public enum SettlementStatus {

    PENDING {
        @Override
        public SettlementStatus next() { return CONFIRMED; }
    },
    CONFIRMED {
        @Override
        public SettlementStatus next() { return PAID; }
    },
    PAID {
        @Override
        public SettlementStatus next() {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    };

    public abstract SettlementStatus next();
}
