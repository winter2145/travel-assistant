package com.xin.aiagentbackend.app.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
@Slf4j
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    /**
     * tools 包下 9 个 @Tool 工具的方法名（即注册给 LLM 的工具名）
     */
    private static final List<String> ALL_TOOL_NAMES = List.of(
            "searchAttractions",   // AttractionSearchTool
            "searchFood",          // FoodCuisineSearchTool
            "searchWeather",       // WeatherSearchTool
            "queryRoute",          // MapRouteTool
            "calculateBudget",     // BudgetCalculatorTool
            "checkBudget",         // BudgetCheckTool
            "checkConflicts",      // ConflictCheckTool
            "generateItinerary",   // ItineraryGenerateTool
            "adjustItinerary"      // ItineraryAdjustTool
    );

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我规划一次旅行";

        String answer = loveApp.doChat(message, chatId);
        // 第二轮
        message = "帮我规划大连3天2晚，预算3000，情侣出行";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "预算为0，给我规划豪华旅游";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

    }
    @Test
    void testMultiTurnWithRag() {
        String chatId = "test_multi_002";
        //第一轮
        String r1 = loveApp.doChat("帮我做旅行计划", chatId);
        log.info("第一轮：{}", r1);

        //第二轮补充信息
        String r2 = loveApp.doChat("去青岛，4天，家庭亲子，预算5000", chatId);
        log.info("第二轮补充信息，期待输出亲子适配行程：{}", r2);
    }

    @Test
    void testLocalRag() {

        String answer = loveApp.doChat(
                "预算极低但我又想住高档酒店，应该怎么规划？",
                "test-local-rag-001"
        );

        System.out.println("\n==============================");
        System.out.println("AI回答：");
        System.out.println(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

    }

    /**
     * 集成测试：通过多轮真实对话让 LLM 依次调用各工具，
     * 验证 tools 包下 10 个类（9 个 @Tool + AmapWebClient）都被实际调用。
     * <p>
     * LLM 调用工具有随机性（可能跳过某个工具），因此采用“多轮补齐”策略：
     * 每轮只重发尚未被调用的工具对应消息（每次用新会话，避免上下文干扰），
     * 直到 9 个工具全部被调用，最多 3 轮。
     */
//    @Test
//    void doChatWithTools_invokesAllTools() {
//        // 工具名 -> 触发消息（显式点名工具并枚举全部入参）
//        Map<String, String> toolTriggers = new LinkedHashMap<>();
//        toolTriggers.put("searchAttractions",
//                "请调用景点搜索工具 searchAttractions 联网搜索上海外滩附近适合情侣约会的景点：city 传\"上海\"，keyword 传\"外滩 情侣 约会\"，pageSize 传 5，列出最新推荐");
//        toolTriggers.put("searchFood",
//                "请调用美食搜索工具 searchFood 搜索上海好评杭帮菜餐厅：city 传\"上海\"，keyword 传\"杭帮菜\"，area 传空字符串，pageSize 传 5，推荐3家");
//        toolTriggers.put("searchWeather",
//                "请调用天气查询工具 searchWeather 查询上海天气预报：cityOrAdcode 传\"上海\"，extensions 传\"all\"，不要凭常识回答");
//        toolTriggers.put("queryRoute",
//                "请调用路线规划工具 queryRoute 查询从上海虹桥火车站（121.3237,31.1985）驾车到外滩（121.4895,31.2400）的路线：origin 传\"121.3237,31.1985\"，destination 传\"121.4895,31.2400\"，mode 传\"driving\"，city 传\"上海\"，输出距离和耗时");
//        toolTriggers.put("calculateBudget",
//                "请调用预算计算工具 calculateBudget 计算一次上海3天2人旅行的花费，参数如下：accommodationPerNight=500，nights=2，foodPerPersonPerDay=150，days=3，transportTotal=800，ticketTotal=200，otherTotal=100，travelers=2，budget=5000。9个参数都要传，一个都不能少");
//        toolTriggers.put("generateItinerary",
//                "请调用行程生成工具 generateItinerary 生成上海2天1晚行程初稿，参数如下：city=\"上海\"，startDate=\"2026-10-13\"，days=2，attractionsJson=[{\"name\":\"外滩\",\"cost\":0},{\"name\":\"东方明珠\",\"cost\":220},{\"name\":\"城隍庙\",\"cost\":10}]，pace=\"normal\"，budget=3000。6个参数都要传");
//        toolTriggers.put("checkBudget",
//                "请调用预算检查工具 checkBudget 检查我的预算是否超支，必须调用该工具、禁止自己估算。参数如下：itineraryJson={\"days\":[{\"day\":1,\"activities\":[{\"name\":\"外滩\",\"estimatedCost\":100},{\"name\":\"城隍庙\",\"estimatedCost\":80}]}]}，budget=3000，additionalCosts=500，把工具返回的结论告诉我");
//        toolTriggers.put("checkConflicts",
//                "请调用时间冲突检查工具 checkConflicts 检查以下行程是否冲突：itineraryJson={\"days\":[{\"day\":1,\"activities\":[{\"name\":\"外滩\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"},{\"name\":\"城隍庙\",\"startTime\":\"10:30\",\"endTime\":\"13:00\"}]}]}，defaultTravelMinutes=30");
//        toolTriggers.put("adjustItinerary",
//                "请调用行程调整工具 adjustItinerary 把行程中活动“外滩”改名为“外滩夜景”：itineraryJson={\"days\":[{\"day\":1,\"activities\":[{\"name\":\"外滩\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}]}]}，operation=\"RENAME\"，targetName=\"外滩\"，newName=\"外滩夜景\"，newStartTime=\"\"，newEndTime=\"\"，targetDay=0");
//
//        // 多轮补齐：每轮只发尚未触发的工具，累计到全部覆盖（最多 3 轮）
//        Set<String> invokedAll = new HashSet<>();
//        for (int round = 1; round <= 3 && !invokedAll.containsAll(ALL_TOOL_NAMES); round++) {
//            for (Map.Entry<String, String> e : toolTriggers.entrySet()) {
//                if (invokedAll.contains(e.getKey())) {
//                    continue; // 已触发过，不再重复
//                }
//                String chatId = UUID.randomUUID().toString();
//                testMessage(e.getValue(), chatId);
//                invokedAll.addAll(loveApp.getInvokedTools(chatId));
//            }
//            log.info("第 {} 轮后已调用工具: {}", round, invokedAll);
//        }
//
//        // 1) 9 个 @Tool 工具都被调用
//        for (String tool : ALL_TOOL_NAMES) {
//            Assertions.assertTrue(invokedAll.contains(tool), "工具未被调用: " + tool);
//        }
//
//    }


    private void testMessage(String message) {
        testMessage(message, UUID.randomUUID().toString());
    }

    private void testMessage(String message, String chatId) {
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
        log.info("question: {}\nanswer: {}", message, answer);
    }

    @Test
    void doChatWithMCP() {
        String chatId = "test_chat_MCP_001";
        // 测试MCP 服务
        String result = loveApp.doChatWithMCP("周末想带女朋友去上海约会，推荐2个适合情侣的小众打卡地", chatId);
        Assertions.assertNotNull(result);
    }

    @Test
    void doChatWithPexelSearchMCP() {
        String chatId = "test_chat_wikimedia_MCP_001";
        // 测试 Pexels MCP 服务（SSE 连接，需先手动启动 ai-agent-pexelssearch-mcp-server）
        String result = loveApp.doChatWithPexelsSearchMCP(
                "周末想去上海外滩约会，推荐2个适合情侣的小众打卡地并返回他们的图片链接",
                chatId);
        Assertions.assertNotNull(result);
        log.info("wikimedia MCP answer: {}", result);
    }
}
