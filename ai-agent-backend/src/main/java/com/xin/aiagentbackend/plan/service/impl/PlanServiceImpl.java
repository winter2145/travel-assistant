package com.xin.aiagentbackend.plan.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.conversation.service.ConversationService;
import com.xin.aiagentbackend.plan.dto.PlanDto;
import com.xin.aiagentbackend.plan.dto.PlanPageDto;
import com.xin.aiagentbackend.plan.dto.PlanRequest;
import com.xin.aiagentbackend.plan.dto.UpdatePlanRequest;
import com.xin.aiagentbackend.plan.repository.PlanRepository;
import com.xin.aiagentbackend.plan.service.PlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 旅行计划业务服务实现类（ServiceImpl）。
 * <p>实现 {@link PlanService} 接口，承载旅行计划的业务规则：
 * 行程 JSON 合法性校验、分页参数约束、会话存在性校验以及乐观锁更新。</p>
 */
@Service
public class PlanServiceImpl implements PlanService {
    /** 分页查询允许的最大每页条数 */
    private static final int MAX_PAGE_SIZE = 100;
    private final PlanRepository repository;
    private final ConversationService conversations;

    public PlanServiceImpl(PlanRepository repository, ConversationService conversations) {
        this.repository = repository;
        this.conversations = conversations;
    }

    /** 创建旅行计划：先校验行程合法性，再校验关联会话存在，最后入库 */
    @Transactional
    public PlanDto create(PlanRequest request) {
        validateItinerary(request.itinerary());
        String conversationId = normalizeConversationId(request.conversationId());
        if (conversationId != null) conversations.get(conversationId);
        return repository.create(conversationId, request.title().strip(), request.itinerary(), request.budgetSummary());
    }

    /** 分页查询旅行计划列表，先对页码与每页条数做合法性校验 */
    public PlanPageDto list(int page, int pageSize, String keyword) {
        int safePage = validPage(page);
        int safeSize = validPageSize(pageSize);
        return new PlanPageDto(repository.list(safeSize, (safePage - 1) * safeSize, keyword),
                repository.count(keyword), safePage, safeSize);
    }

    /** 查询旅行计划详情，不存在时抛出 404 异常 */
    public PlanDto get(String id) { return repository.get(id).orElseThrow(() -> ApiException.notFound("旅行计划不存在")); }

    /** 按会话标识查询最近保存的旅行计划，不存在时抛出 404 异常 */
    public PlanDto getByConversation(String conversationId) {
        return repository.getByConversation(conversationId).orElseThrow(() -> ApiException.notFound("该会话还没有已保存的行程"));
    }

    /** 更新旅行计划：标题为空时沿用原标题，行程需通过合法性校验 */
    @Transactional
    public PlanDto update(String id, UpdatePlanRequest request) {
        PlanDto current = get(id);
        validateItinerary(request.itinerary());
        String title = request.title() == null || request.title().isBlank() ? current.title() : request.title().strip();
        return repository.update(id, title, request.itinerary(), request.budgetSummary(), request.version());
    }

    /** 删除旅行计划，不存在时抛出 404 异常 */
    @Transactional
    public void delete(String id) {
        if (!repository.delete(id)) throw ApiException.notFound("旅行计划不存在");
    }

    /** 复制旅行计划：基于原计划生成副本，标题追加“（副本）”后缀 */
    @Transactional
    public PlanDto duplicate(String id) {
        PlanDto original = get(id);
        JsonNode itinerary = original.itinerary();
        return repository.create(null, original.title() + "（副本）", itinerary, original.budgetSummary());
    }

    /** 规范化会话标识：空白值统一转 null */
    private String normalizeConversationId(String id) {
        return id == null || id.isBlank() ? null : id.strip();
    }

    /** 校验行程 JSON 的完整性：目的地、天数、预算、日期格式、每日活动等 */
    private void validateItinerary(JsonNode itinerary) {
        if (itinerary == null || !itinerary.isObject()) throw ApiException.badRequest("itinerary必须是JSON对象");
        if (itinerary.path("city").asText().isBlank()) throw ApiException.badRequest("行程目的地不能为空");
        JsonNode days = itinerary.path("days");
        if (!days.isArray() || days.isEmpty() || days.size() > 14) throw ApiException.badRequest("行程天数必须在1到14天之间");
        if (itinerary.path("budget").asDouble(-1) < 0) throw ApiException.badRequest("预算不能为负数");
        String startDate = itinerary.path("startDate").asText("");
        if (!startDate.isBlank()) {
            try { java.time.LocalDate.parse(startDate); }
            catch (Exception e) { throw ApiException.badRequest("startDate必须使用yyyy-MM-dd格式"); }
        }
        for (JsonNode day : days) {
            if (!day.path("activities").isArray()) throw ApiException.badRequest("每日行程必须包含activities数组");
            for (JsonNode activity : day.path("activities")) {
                if (activity.path("name").asText().isBlank()) throw ApiException.badRequest("活动名称不能为空");
                if (activity.path("estimatedCost").asDouble(0) < 0) throw ApiException.badRequest("活动费用不能为负数");
            }
        }
    }

    /** 校验页码：必须大于等于 1 */
    private int validPage(int value) { if (value < 1) throw ApiException.badRequest("page必须大于等于1"); return value; }
    /** 校验每页条数：必须在 1 到 100 之间 */
    private int validPageSize(int value) {
        if (value < 1 || value > MAX_PAGE_SIZE) throw ApiException.badRequest("pageSize必须在1到100之间");
        return value;
    }
}
