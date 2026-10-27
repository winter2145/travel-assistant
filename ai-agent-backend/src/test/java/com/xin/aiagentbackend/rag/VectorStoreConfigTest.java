package com.xin.aiagentbackend.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;



@SpringBootTest
public class VectorStoreConfigTest {

    @Resource
    private VectorStore vectorStore;

    @Test
    void testSimilaritySearch() {

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("Spring AI 是什么？")
                                .topK(2)
                                .build()
                );

        documents.forEach(document -> {
            System.out.println(document.getText());
        });
    }

    @Test
    void testSimilaritySearchMD() {

        testSearch("预算极低但要全程高端住宿");
        testSearch("我没钱但是想住五星级酒店怎么办？");
        testSearch("旅游预算和住宿档次发生冲突怎么办？");
    }

    private void testSearch(String query) {

        System.out.println("\n================================");
        System.out.println("Query：" + query);
        System.out.println("================================");

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(3)
                                .similarityThreshold(0.7) // 相似度低于 0.7 的 Document，不要
                                .build()
                );

        for (int i = 0; i < documents.size(); i++) {

            Document document = documents.get(i);

            System.out.println("\n----- Top " + (i + 1) + " -----");

            System.out.println("内容：");
            System.out.println(document.getText());

            System.out.println("Metadata：");
            System.out.println(document.getMetadata());
        }
    }

    @Test
    void testSimilaritySearchMetadataFilter() {

        testSearchMetadataFilter("预算极低但要全程高端住宿");
        testSearchMetadataFilter("我没钱但是想住五星级酒店怎么办？");
        testSearchMetadataFilter("旅游预算和住宿档次发生冲突怎么办？");
    }

    private void testSearchMetadataFilter(String query) {

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(3)
                                .filterExpression(
                                        "filename == '旅行规划智能体-常见问题FAQ与异常处理规范.md'"
                                )
                                .build()
                );

        for (int i = 0; i < documents.size(); i++) {

            Document document = documents.get(i);

            System.out.println("\n----- Top " + (i + 1) + " -----");

            System.out.println("内容：");
            System.out.println(document.getText());

            System.out.println("Metadata：");
            System.out.println(document.getMetadata());
        }
    }
}