package com.example.settlement;

import com.example.settlement.domain.settlement.SettlementRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class SettlementIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SettlementRepository settlementRepository;

    // creator-1 March 2025:
    //   sales:   sale-1(50000) + sale-2(50000) + sale-3(80000) + sale-4(80000) = 260000
    //   cancels: cancel-1(80000) + cancel-2(30000) = 110000  (both cancelled in March KST)
    //   net:     150000  fee: 30000  payout: 120000
    @Test
    @Order(1)
    void creator1_march_settlement() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").value(260000))
                .andExpect(jsonPath("$.data.totalRefunds").value(110000))
                .andExpect(jsonPath("$.data.netSales").value(150000))
                .andExpect(jsonPath("$.data.feeAmount").value(30000))
                .andExpect(jsonPath("$.data.payoutAmount").value(120000))
                .andExpect(jsonPath("$.data.saleCount").value(4))
                .andExpect(jsonPath("$.data.cancelCount").value(2))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    @Order(2)
    void partial_refund_reflected() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRefunds").value(110000))
                .andExpect(jsonPath("$.data.netSales").value(150000));
    }

    // sale-5 paidAt = 2025-01-31T14:30Z  →  within Jan KST [2024-12-31T15Z, 2025-01-31T15Z)
    // cancel-3 cancelledAt = 2025-02-03T01Z  →  in Feb KST, not counted here
    @Test
    @Order(3)
    void month_boundary_sale_january() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").value(60000))
                .andExpect(jsonPath("$.data.totalRefunds").value(0))
                .andExpect(jsonPath("$.data.netSales").value(60000))
                .andExpect(jsonPath("$.data.payoutAmount").value(48000));
    }

    // Feb KST range: [2025-01-31T15Z, 2025-02-28T15Z)
    // sale-5 paidAt 14:30Z is before the 15:00Z boundary → 0 sales
    // cancel-3 cancelledAt 02-03T01Z → in Feb KST → 60000 refund
    // netSales = -60000, fee = -12000 (DOWN: towards zero), payout = -48000
    @Test
    @Order(4)
    void month_boundary_cancel_february() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").value(0))
                .andExpect(jsonPath("$.data.totalRefunds").value(60000))
                .andExpect(jsonPath("$.data.netSales").value(-60000))
                .andExpect(jsonPath("$.data.feeAmount").value(-12000))
                .andExpect(jsonPath("$.data.payoutAmount").value(-48000));
    }

    // creator-3 has only sale-7 (Feb KST), so March returns all zeros
    @Test
    @Order(5)
    void empty_month_returns_zeros() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-3")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").value(0))
                .andExpect(jsonPath("$.data.totalRefunds").value(0))
                .andExpect(jsonPath("$.data.netSales").value(0))
                .andExpect(jsonPath("$.data.feeAmount").value(0))
                .andExpect(jsonPath("$.data.payoutAmount").value(0))
                .andExpect(jsonPath("$.data.saleCount").value(0))
                .andExpect(jsonPath("$.data.cancelCount").value(0));
    }

    @Test
    @Order(6)
    void status_transition_pending_to_confirmed() throws Exception {
        mockMvc.perform(post("/api/settlements/confirm")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @Order(7)
    void status_transition_confirmed_to_paid() throws Exception {
        mockMvc.perform(post("/api/settlements/pay")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @Order(8)
    void confirm_on_paid_returns_409() throws Exception {
        mockMvc.perform(post("/api/settlements/confirm")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(9)
    void pay_without_confirm_returns_409() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/settlements/pay")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(10)
    void invalid_yearmonth_format_returns_400() throws Exception {
        for (String invalid : new String[]{"2025-3", "202503", "2025/03", "abcd"}) {
            mockMvc.perform(get("/api/settlements/monthly")
                            .param("creatorId", "creator-1")
                            .param("yearMonth", invalid))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @Order(11)
    void admin_summary_full_range() throws Exception {
        mockMvc.perform(get("/api/admin/settlements")
                        .param("from", "2025-01-01")
                        .param("to", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.grandTotal").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(12)
    void admin_csv_export() throws Exception {
        mockMvc.perform(get("/api/admin/settlements/export")
                        .param("from", "2025-01-01")
                        .param("to", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("크리에이터ID")))
                .andExpect(content().string(containsString("합계")));
    }

    @Test
    @Order(25)
    void concurrent_settlement_creation_no_duplicate() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    int status = mockMvc.perform(get("/api/settlements/monthly")
                                    .param("creatorId", "creator-3")
                                    .param("yearMonth", "2025-02"))
                            .andReturn().getResponse().getStatus();
                    statusCodes.add(status);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(statusCodes).allMatch(code -> code == 200);
        assertThat(settlementRepository.findByCreatorIdAndYearAndMonth("creator-3", 2025, 2)).isPresent();
    }
}
