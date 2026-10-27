package com.xin.aiagentamapmcpserver.tools;

import com.xin.aiagentamapmcpserver.tools.common.ToolResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AttractionSearchToolTest {

    @Resource
    private AttractionSearchTool attractionSearchTool;

    @Test
    void searchAttractions() {
        String city = "上海";
        String keyword = "外滩 情侣 约会";
        int size = 1;

        ToolResult toolResult = attractionSearchTool.searchAttractions(city, keyword, size);
        Assertions.assertNotNull(toolResult);
    }
}