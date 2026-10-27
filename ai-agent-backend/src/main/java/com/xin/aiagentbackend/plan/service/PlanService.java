package com.xin.aiagentbackend.plan.service;

import com.xin.aiagentbackend.plan.dto.PlanDto;
import com.xin.aiagentbackend.plan.dto.PlanPageDto;
import com.xin.aiagentbackend.plan.dto.PlanRequest;
import com.xin.aiagentbackend.plan.dto.UpdatePlanRequest;

/**
 * 旅行计划业务服务接口（Service）。
 * <p>定义旅行计划相关的业务能力：创建、列表、详情、按会话查询、更新、删除与复制。
 * 具体实现见 {@code impl.PlanServiceImpl}。</p>
 */
public interface PlanService {

    /** 创建旅行计划（含行程合法性校验） */
    PlanDto create(PlanRequest request);

    /** 分页查询旅行计划列表，支持关键词过滤 */
    PlanPageDto list(int page, int pageSize, String keyword);

    /** 查询旅行计划详情 */
    PlanDto get(String id);

    /** 按会话标识查询最近保存的旅行计划 */
    PlanDto getByConversation(String conversationId);

    /** 更新旅行计划（乐观锁并发控制） */
    PlanDto update(String id, UpdatePlanRequest request);

    /** 删除旅行计划 */
    void delete(String id);

    /** 复制旅行计划为副本 */
    PlanDto duplicate(String id);
}
