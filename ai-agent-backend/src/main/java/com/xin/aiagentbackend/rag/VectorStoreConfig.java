package com.xin.aiagentbackend.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * RAG 向量库配置：
 * <p>
 * VectorStore Bean 由 spring-ai-starter-vector-store-pgvector 自动装配，
 * 表结构（含 pgvector 扩展、embedding 列、HNSW 索引）按
 * application.yml 中 spring.ai.vectorstore.pgvector.* 配置自动初始化。
 * <p>
 * 本类负责在启动时把 Markdown 知识库切分为 Chunk 后写入 pgvector。
 * 写入是幂等的：pgvector 表中已有数据则跳过，避免每次启动重复灌入。
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    /**
     * pgvector 中存储向量数据的表名，需与 application.yml 中
     * spring.ai.vectorstore.pgvector.table-name 保持一致
     */
    private static final String VECTOR_TABLE = "vector_store";

    /**
     * DashScope Embedding API 单次请求最多支持 25 条文本，
     * 这里按 20 条一批分批写入，留出安全余量
     */
    private static final int EMBED_BATCH_SIZE = 20;

    @Resource
    public MarkdownDocumentLoader markdownDocumentLoader;

    /**
     * 启动时把知识库文档切分后写入 pgvector（幂等）
     */
    @Bean
    public ApplicationRunner ragVectorStoreInitializer(
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate) {

        return new ApplicationRunner() {

            @Override
            public void run(ApplicationArguments args) {

                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + VECTOR_TABLE,
                        Long.class
                );

                if (count != null && count > 0) {
                    log.info("pgvector 已存在 {} 条向量，跳过知识库初始化写入", count);
                    return;
                }

                List<Document> chunks = chunks();

                log.info("开始向 pgvector 写入 {} 个 Chunk ...", chunks.size());

                // 分批写入，规避 Embedding API 单次 25 条文本的限制
                for (int i = 0; i < chunks.size(); i += EMBED_BATCH_SIZE) {

                    List<Document> batch = chunks.subList(
                            i,
                            Math.min(i + EMBED_BATCH_SIZE, chunks.size())
                    );

                    vectorStore.add(batch);
                }

                log.info("知识库已写入 pgvector，共 {} 条向量", chunks.size());
            }
        };
    }

    public List<Document> chunks() {
        // 1. 读取 Markdown
        List<Document> documents =
                markdownDocumentLoader.loadMarkdowns();

        // 2. 创建切分器
        MarkdownChunkSplitter splitter =
                new MarkdownChunkSplitter(1000);

        // 3. 执行切分
        List<Document> chunks =
                splitter.split(documents);

        return chunks;
    }


}
