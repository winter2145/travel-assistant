package com.xin.aiagentbackend.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class MarkdownDocumentLoaderAndSplitTest {

    @Resource
    private MarkdownDocumentLoader markdownDocumentLoader;

    @Test
    void testSplitMarkdown() {

        // 1. 读取 Markdown
        List<Document> documents =
                markdownDocumentLoader.loadMarkdowns();

        System.out.println(
                "原始 Document 数量：" + documents.size()
        );

        // 2. 创建切分器
        MarkdownChunkSplitter splitter =
                new MarkdownChunkSplitter(1000);

        // 3. 执行切分
        List<Document> chunks =
                splitter.split(documents);

        System.out.println(
                "切分后 Chunk 数量：" + chunks.size()
        );

        // 4. 打印结果
        for (int i = 0; i < chunks.size(); i++) {

            Document chunk = chunks.get(i);

            System.out.println("\n==============================");
            System.out.println("Chunk #" + (i + 1));
            System.out.println("==============================");

            System.out.println(
                    "内容：\n" + chunk.getText()
            );

            System.out.println(
                    "Metadata：" + chunk.getMetadata()
            );
        }
    }
}