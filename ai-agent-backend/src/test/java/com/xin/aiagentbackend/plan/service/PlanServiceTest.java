package com.xin.aiagentbackend.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.conversation.service.ConversationService;
import com.xin.aiagentbackend.plan.dto.PlanRequest;
import com.xin.aiagentbackend.plan.repository.PlanRepository;
import com.xin.aiagentbackend.plan.service.impl.PlanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 旅行计划业务服务单元测试。
 * <p>验证 {@link PlanServiceImpl} 在行程入库前对非法行程（缺少目的地、超长天数）做参数校验。</p>
 */
class PlanServiceTest {
    private PlanRepository repository;
    private PlanService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlanRepository.class);
        service = new PlanServiceImpl(repository, mock(ConversationService.class));
    }

    /** 缺少目的地城市时应在入库前被拒绝 */
    @Test
    void rejectsPlanWithoutDestinationBeforeStorage() throws Exception {
        var itinerary = new ObjectMapper().readTree("""
                {"city":"","days":[{"day":1,"activities":[]}],"budget":1500}
                """);
        var request = new PlanRequest(null, "杭州旅行", itinerary, null);

        ApiException error = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(400, error.getStatus().value());
        verifyNoInteractions(repository);
    }

    /** 超过 14 天的行程应在入库前被拒绝 */
    @Test
    void rejectsMoreThanFourteenDaysBeforeStorage() throws Exception {
        var itinerary = new ObjectMapper().readTree("""
                {"city":"杭州","days":[
                  {"activities":[]},{"activities":[]},{"activities":[]},{"activities":[]},
                  {"activities":[]},{"activities":[]},{"activities":[]},{"activities":[]},
                  {"activities":[]},{"activities":[]},{"activities":[]},{"activities":[]},
                  {"activities":[]},{"activities":[]},{"activities":[]}
                ],"budget":1500}
                """);
        var request = new PlanRequest(null, "杭州旅行", itinerary, null);

        ApiException error = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(400, error.getStatus().value());
        verifyNoInteractions(repository);
    }
}
