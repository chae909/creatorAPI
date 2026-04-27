package com.example.settlement.application.settlement;

import com.example.settlement.domain.settlement.Settlement;
import com.example.settlement.domain.settlement.SettlementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class SettlementSaver {

    private final SettlementRepository settlementRepository;

    SettlementSaver(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Settlement save(Settlement settlement) {
        return settlementRepository.saveAndFlush(settlement);
    }
}
