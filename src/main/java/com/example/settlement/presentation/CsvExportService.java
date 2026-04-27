package com.example.settlement.presentation;

import com.example.settlement.application.settlement.SettlementUseCase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.StringJoiner;

@Service
public class CsvExportService {

    public String toCsv(SettlementUseCase.AdminSettlementSummary summary) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("크리에이터ID,크리에이터명,연,월,총판매,총환불,순판매,수수료율,수수료,정산예정,상태");
        for (var item : summary.items()) {
            String feeRateStr = item.feeRate()
                    .multiply(BigDecimal.valueOf(100))
                    .stripTrailingZeros()
                    .toPlainString() + "%";
            joiner.add(String.join(",",
                    item.creatorId(),
                    item.creatorName(),
                    String.valueOf(item.year()),
                    String.valueOf(item.month()),
                    String.valueOf(item.totalSales()),
                    String.valueOf(item.totalRefunds()),
                    String.valueOf(item.netSales()),
                    feeRateStr,
                    String.valueOf(item.feeAmount()),
                    String.valueOf(item.payoutAmount()),
                    item.status()
            ));
        }
        joiner.add("합계,,,,,,,,," + summary.grandTotal() + ",");
        return joiner.toString();
    }
}
