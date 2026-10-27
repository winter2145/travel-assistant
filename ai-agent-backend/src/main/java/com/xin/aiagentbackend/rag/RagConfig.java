package com.xin.aiagentbackend.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

//    private static final String INDEX_NAME = "旅行规划智能体知识库";
//
//    @Bean
//    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(DashScopeApi dashScopeApi) {
//
//        // 1. 创建百炼知识库检索器
//        DocumentRetriever documentRetriever =
//                new DashScopeDocumentRetriever(
//                        dashScopeApi,
//                        DashScopeDocumentRetrieverOptions.builder()
//                                .withIndexName(INDEX_NAME)
//                                .build()
//                );
//
//        // 2. 创建 RAG Advisor
//        return RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(documentRetriever)
//                .build();
//    }

    @Bean
    public RetrievalAugmentationAdvisor localRetrievalAugmentationAdvisor(
            VectorStore vectorStore) {

        // 1. 创建本地 VectorStore 检索器
        DocumentRetriever documentRetriever =
                VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(3)
                        .build();

        // 2. 创建本地 RAG Advisor
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }


}
