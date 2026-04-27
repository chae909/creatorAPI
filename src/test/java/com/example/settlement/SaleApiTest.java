package com.example.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class SaleApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @Order(1)
    void register_sale_success() throws Exception {
        var body = Map.of(
                "courseId", "course-1",
                "studentId", "student-99",
                "amount", 50000,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.cancelled").value(false));
    }

    @Test
    @Order(2)
    void register_sale_invalid_course_returns_404() throws Exception {
        var body = Map.of(
                "courseId", "course-999",
                "studentId", "student-1",
                "amount", 50000,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // sale-3 is already cancelled by cancel-1 (seed data)
    @Test
    @Order(3)
    void cancel_already_cancelled_returns_409() throws Exception {
        var body = Map.of(
                "refundAmount", 10000,
                "cancelledAt", "2025-04-01T10:00:00+09:00"
        );

        mockMvc.perform(post("/api/sales/sale-3/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // sale-1 amount = 50000; 999999 > 50000 → REFUND_EXCEEDS_PAYMENT
    @Test
    @Order(4)
    void cancel_refund_exceeds_amount_returns_400() throws Exception {
        var body = Map.of(
                "refundAmount", 999999,
                "cancelledAt", "2025-04-01T10:00:00+09:00"
        );

        mockMvc.perform(post("/api/sales/sale-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void cancel_nonexistent_sale_returns_404() throws Exception {
        var body = Map.of(
                "refundAmount", 10000,
                "cancelledAt", "2025-04-01T10:00:00+09:00"
        );

        mockMvc.perform(post("/api/sales/sale-9999/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // creator-1 courses: course-1, course-2
    // from=2025-03-01T00:00+09 (=2025-02-28T15Z) to=2025-04-01T00:00+09 (=2025-03-31T15Z)
    // matches sale-1, sale-2, sale-3, sale-4 (all March KST)  → size 4
    @Test
    @Order(6)
    void list_sales_by_creator() throws Exception {
        mockMvc.perform(get("/api/sales")
                        .param("creatorId", "creator-1")
                        .param("from", "2025-03-01T00:00:00+09:00")
                        .param("to", "2025-04-01T00:00:00+09:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    @Order(7)
    void register_sale_amount_zero_returns_400() throws Exception {
        var body = Map.of(
                "courseId", "course-1",
                "studentId", "student-1",
                "amount", 0,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    void register_sale_amount_negative_returns_400() throws Exception {
        var body = Map.of(
                "courseId", "course-1",
                "studentId", "student-1",
                "amount", -1,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(9)
    void register_sale_blank_courseId_returns_400() throws Exception {
        var body = Map.of(
                "courseId", "",
                "studentId", "student-1",
                "amount", 50000,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    void register_sale_invalid_paidAt_returns_400() throws Exception {
        var body = Map.of(
                "courseId", "course-1",
                "studentId", "student-1",
                "amount", 50000,
                "paidAt", "not-a-date"
        );
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    void register_sale_future_paidAt_returns_400() throws Exception {
        var body = Map.of(
                "courseId", "course-1",
                "studentId", "student-1",
                "amount", 50000,
                "paidAt", "2099-01-01T00:00:00+09:00"
        );
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    void get_settlement_invalid_yearmonth_no_leading_zero_returns_400() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    void get_settlement_invalid_yearmonth_month_13_returns_400() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-13"))
                .andExpect(status().isBadRequest());
    }
}
