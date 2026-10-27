package com.xin.aiagentbackend.rag;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownChunkSplitter {

    /**
     * Markdown 标题：
     * # 一级标题
     * ## 二级标题
     * ### 三级标题
     */
    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");

    /**
     * 最大 Chunk 字符数
     */
    private final int maxChunkSize;

    public MarkdownChunkSplitter(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    /**
     * 对 Document 进行 Markdown 结构化切分
     */
    public List<Document> split(List<Document> documents) {

        List<Document> chunks = new ArrayList<>();

        for (Document document : documents) {
            chunks.addAll(splitDocument(document));
        }

        return chunks;
    }

    private List<Document> splitDocument(Document document) {

        List<Document> chunks = new ArrayList<>();

        // 保存当前 Markdown 标题层级
        String[] headings = new String[6];

        StringBuilder content = new StringBuilder();

        String currentHeading = null;
        int currentHeadingLevel = 0;

        String[] lines = document.getText().split("\\R");

        for (String line : lines) {

            Matcher matcher = HEADER_PATTERN.matcher(line);

            if (matcher.matches()) {

                // 先处理之前积累的内容
                if (!content.isEmpty()) {

                    chunks.addAll(
                            createChunks(
                                    content.toString(),
                                    headings,
                                    currentHeading,
                                    currentHeadingLevel,
                                    document
                            )
                    );

                    content.setLength(0);
                }

                int level = matcher.group(1).length();
                String title = matcher.group(2).trim();

                // 清除当前标题以下的层级
                for (int i = level; i < headings.length; i++) {
                    headings[i] = null;
                }

                headings[level - 1] = title;

                currentHeading = title;
                currentHeadingLevel = level;

                continue;
            }

            content.append(line).append("\n");
        }

        // 处理最后一部分
        if (!content.isEmpty()) {

            chunks.addAll(
                    createChunks(
                            content.toString(),
                            headings,
                            currentHeading,
                            currentHeadingLevel,
                            document
                    )
            );
        }

        return chunks;
    }

    /**
     * 一个标题下的内容可能非常大，
     * 因此继续进行递归式拆分。
     */
    private List<Document> createChunks(
            String text,
            String[] headings,
            String currentHeading,
            int currentHeadingLevel,
            Document sourceDocument
    ) {

        text = text.trim();

        if (text.isEmpty()) {
            return List.of();
        }

        List<String> pieces = splitRecursively(text);

        List<Document> result = new ArrayList<>();

        for (String piece : pieces) {

            Map<String, Object> metadata =
                    new HashMap<>(sourceDocument.getMetadata());

            if (currentHeading != null) {
                metadata.put("heading", currentHeading);
            }

            if (currentHeadingLevel > 0) {
                metadata.put("headingLevel", currentHeadingLevel);
            }

            String headingPath = buildHeadingPath(headings);
            if (!headingPath.isEmpty()) {
                metadata.put("headingPath", headingPath);
            }

            result.add(
                    new Document(piece, metadata)
            );
        }

        return result;
    }

    /**
     * 递归切分：
     *
     * 段落
     *   ↓
     * 句子
     *   ↓
     * 强制按长度切
     */
    private List<String> splitRecursively(String text) {

        if (text.length() <= maxChunkSize) {
            return List.of(text);
        }

        // 第一优先级：按照段落切
        String[] paragraphs =
                text.split("\\n\\s*\\n");

        if (paragraphs.length > 1) {
            return mergePieces(paragraphs);
        }

        // 第二优先级：按照句子切
        String[] sentences =
                text.split("(?<=[。！？.!?])\\s*");

        if (sentences.length > 1) {
            return mergePieces(sentences);
        }

        // 最后的兜底策略
        return forceSplit(text);
    }

    /**
     * 将多个小片段尽可能合并，
     * 直到达到 maxChunkSize。
     */
    private List<String> mergePieces(String[] pieces) {

        List<String> result = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String piece : pieces) {

            piece = piece.trim();

            if (piece.isEmpty()) {
                continue;
            }

            if (current.isEmpty()) {

                current.append(piece);

            } else if (current.length()
                    + 1
                    + piece.length()
                    <= maxChunkSize) {

                current.append("\n")
                        .append(piece);

            } else {

                result.add(current.toString());

                current.setLength(0);

                // 单独一个片段就已经超过限制
                if (piece.length() > maxChunkSize) {

                    result.addAll(
                            splitRecursively(piece)
                    );

                } else {

                    current.append(piece);
                }
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * 一个句子/段落本身就特别长时，
     * 最终按照字符长度强制切割。
     */
    private List<String> forceSplit(String text) {

        List<String> result = new ArrayList<>();

        for (int start = 0;
             start < text.length();
             start += maxChunkSize) {

            int end = Math.min(
                    start + maxChunkSize,
                    text.length()
            );

            result.add(
                    text.substring(start, end)
            );
        }

        return result;
    }

    /**
     * 构造完整标题路径：
     *
     * Spring AI > RAG > Document
     */
    private String buildHeadingPath(String[] headings) {

        List<String> path = new ArrayList<>();

        for (String heading : headings) {

            if (heading != null) {
                path.add(heading);
            }
        }

        return String.join(" > ", path);
    }
}