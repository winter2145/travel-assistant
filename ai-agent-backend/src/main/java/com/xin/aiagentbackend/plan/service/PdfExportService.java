package com.xin.aiagentbackend.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xin.aiagentbackend.plan.dto.PdfExportRequest;
import com.xin.aiagentbackend.plan.dto.PlanDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.awt.Color;

/**
 * PDF 攻略导出服务。
 * <p>基于 PDFBox 将旅行计划（{@link PlanDto}）渲染为可下载的 PDF 攻略文档，
 * 支持 simple（简洁）与 magazine（杂志风）两种样式，可开关封面、概览、景点图片、预算、每日行程等章节，
 * 并支持嵌入 Pexels 景点图片。</p>
 */
@Service
public class PdfExportService {
    private static final float LEFT = 48;
    private static final float RIGHT = 547;
    private final String fontPath;
    private final HttpClient downloadClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ImageDownloader imageDownloader;

    @Autowired
    public PdfExportService(@org.springframework.beans.factory.annotation.Value("${travel.pdf.font:}") String fontPath) {
        this.fontPath = fontPath;
        this.imageDownloader = this::downloadImage;
    }

    PdfExportService(String fontPath, ImageDownloader imageDownloader) {
        this.fontPath = fontPath;
        this.imageDownloader = imageDownloader;
    }

    private byte[] downloadImage(String url) throws Exception {
        HttpResponse<byte[]> response = downloadClient.send(
                HttpRequest.newBuilder(java.net.URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("景点图片下载失败：HTTP " + response.statusCode());
        }
        return response.body();
    }

    public byte[] generate(PlanDto plan, PdfExportRequest request) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            boolean magazine = "magazine".equals(request.style());
            Canvas canvas = new Canvas(document, font, magazine);
            JsonNode itinerary = plan.itinerary();
            Map<String, PhotoAsset> photos = request.includePhotos() ? loadSavedPhotos(itinerary) : Map.of();
            if (request.includeCover()) {
                canvas.cover(itinerary.path("city").asText() + "旅行攻略",
                        itinerary.path("startDate").asText() + " · " + itinerary.path("days").size() + " 天");
                canvas.line("旅行节奏：" + itinerary.path("pace").asText("未设置"));
                canvas.gap(22);
            }
            if (request.includeOverview()) {
                canvas.heading("旅行概览", 16);
                canvas.line("目的地：" + itinerary.path("city").asText());
                canvas.line("出发日期：" + itinerary.path("startDate").asText());
                canvas.line("行程天数：" + itinerary.path("days").size() + " 天");
                canvas.line("节奏：" + itinerary.path("pace").asText("未设置"));
                canvas.gap(10);
            }
            if (request.includeItinerary()) {
                canvas.heading("每日行程", request.style().equals("magazine") ? 19 : 16);
                for (JsonNode day : itinerary.path("days")) {
                    canvas.subheading("第" + day.path("day").asInt() + "天 · " + day.path("date").asText());
                    for (JsonNode activity : day.path("activities")) {
                        canvas.activity(activity, request.includePhotos() ? photos.get(activity.path("name").asText()) : null);
                    }
                    canvas.gap(10);
                }
            }
            if (request.includeBudget()) {
                canvas.heading("预算概览", 16);
                canvas.line("计划预算：￥" + money(itinerary.path("budget").asDouble()));
                canvas.line("活动费用估算：￥" + money(itinerary.path("estimatedActivityCost").asDouble()));
                JsonNode summary = plan.budgetSummary();
                if (summary != null && !summary.isNull()) {
                    canvas.line("预估总支出：￥" + money(summary.path("estimatedTotal").asDouble()));
                    canvas.line("预计剩余：￥" + money(summary.path("remainingBudget").asDouble()));
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("PDF 攻略生成失败：" + e.getMessage(), e);
        }
    }

    private PDFont loadFont(PDDocument document) throws Exception {
        String configured = fontPath == null || fontPath.isBlank() ? "C:/Windows/Fonts/simhei.ttf" : fontPath;
        java.nio.file.Path path = java.nio.file.Path.of(configured);
        if (!java.nio.file.Files.isRegularFile(path)) {
            throw new IllegalStateException("未找到可嵌入的中文字体，请通过 travel.pdf.font 配置 TTF 字体文件路径");
        }
        try (var input = java.nio.file.Files.newInputStream(path)) {
            return PDType0Font.load(document, input);
        }
    }

    private Map<String, PhotoAsset> loadSavedPhotos(JsonNode itinerary) {
        Map<String, PhotoAsset> result = new HashMap<>();
        for (JsonNode day : itinerary.path("days")) for (JsonNode activity : day.path("activities")) {
            String name = activity.path("name").asText("");
            if (name.isBlank() || result.containsKey(name)) continue;
            String url = activity.path("photoUrl").asText("");
            if (url.isBlank()) continue;
            try {
                if (!url.startsWith("https://images.pexels.com/")) throw new IllegalStateException("保存的景点图片地址不是有效的 Pexels 图片地址");
                byte[] imageBytes = imageDownloader.download(url);
                if (imageBytes == null || imageBytes.length == 0) throw new IllegalStateException("景点图片下载响应为空");
                String attribution = "图片来源：" + activity.path("photoSource").asText("Pexels") + " · "
                        + activity.path("photoPhotographer").asText("摄影师");
                result.put(name, new PhotoAsset(imageBytes, attribution));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法获取景点图片“" + name + "”：" + e.getMessage(), e);
            }
        }
        return result;
    }

    private String money(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }
    @FunctionalInterface
    interface ImageDownloader { byte[] download(String url) throws Exception; }
    private record PhotoAsset(byte[] bytes, String attribution) { }

    private static class Canvas {
        private final PDDocument doc;
        private final PDFont font;
        private final boolean magazine;
        private Color currentTextColor = Color.BLACK;
        private PDPage page;
        private float y;
        Canvas(PDDocument doc, PDFont font, boolean magazine) { this.doc = doc; this.font = font; this.magazine = magazine; newPage(); }
        void newPage() { page = new PDPage(PDRectangle.A4); doc.addPage(page); y = 790; }
        void ensure(float height) { if (y - height < 48) newPage(); }
        void cover(String title, String subtitle) throws Exception {
            if (!magazine) { heading(title, 24); line(subtitle); return; }
            ensure(150);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                stream.setNonStrokingColor(new Color(34, 67, 54));
                stream.addRect(LEFT, y - 105, RIGHT - LEFT, 118);
                stream.fill();
            }
            textColor(Color.WHITE); text(title, 25, LEFT + 22, y - 40); text(subtitle, 11, LEFT + 22, y - 70);
            textColor(new Color(34, 67, 54)); y -= 140;
        }
        void heading(String value, int size) throws Exception {
            ensure(size + 24);
            if (magazine) {
                try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    stream.setNonStrokingColor(new Color(216, 230, 217)); stream.addRect(LEFT, y - size - 12, RIGHT - LEFT, size + 24); stream.fill();
                    stream.setNonStrokingColor(new Color(49, 100, 72)); stream.addRect(LEFT, y - size - 12, 4, size + 24); stream.fill();
                }
                textColor(new Color(34, 67, 54));
            }
            text(value, size, LEFT + (magazine ? 12 : 0), y); textColor(Color.BLACK); y -= size + (magazine ? 30 : 18);
        }
        void subheading(String text) throws Exception { ensure(34); text(text, 13, LEFT, y); y -= 24; }
        void line(String text) throws Exception { ensure(20); text(text, 10, LEFT, y); y -= 17; }
        void gap(float amount) { y -= amount; }
        void activity(JsonNode activity, PhotoAsset asset) throws Exception {
            PDImageXObject image = asset == null ? null : PDImageXObject.createFromByteArray(doc, asset.bytes(), "attraction");
            float imageW = image == null ? 0 : Math.min(138, 138f * image.getWidth() / Math.max(image.getWidth(), image.getHeight()));
            float imageH = image == null ? 0 : Math.min(82, 82f * image.getHeight() / Math.max(image.getWidth(), image.getHeight()));
            float textWidth = image == null ? RIGHT - LEFT - 20 : RIGHT - LEFT - imageW - 38;
            java.util.List<String> attribution = image == null || asset.attribution().isBlank()
                    ? java.util.List.of() : wrap(asset.attribution(), 6, imageW);
            java.util.List<String> lines = new java.util.ArrayList<>();
            lines.add(activity.path("startTime").asText() + " - " + activity.path("endTime").asText() + "  " + activity.path("name").asText());
            lines.add("地点：" + activity.path("location").asText("未提供"));
            lines.add("费用：￥" + String.format(java.util.Locale.ROOT, "%.2f", activity.path("estimatedCost").asDouble()));
            if (!activity.path("notes").asText("").isBlank()) lines.add(activity.path("notes").asText());
            java.util.List<String> wrapped = lines.stream().flatMap(line -> wrap(line, 9, textWidth).stream()).toList();
            float cardH = Math.max(66, wrapped.size() * 13 + 25);
            if (image != null) cardH = Math.max(cardH, imageH + 24 + attribution.size() * 8);
            ensure(cardH + 12);
            float top = y;
            if (magazine) {
                try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    stream.setNonStrokingColor(new Color(250, 249, 244)); stream.addRect(LEFT, top - cardH, RIGHT - LEFT, cardH - 5); stream.fill();
                    stream.setStrokingColor(new Color(225, 226, 215)); stream.addRect(LEFT, top - cardH, RIGHT - LEFT, cardH - 5); stream.stroke();
                }
            }
            float textX = LEFT + 10, baseline = top - 20;
            for (int i = 0; i < wrapped.size(); i++) {
                text(wrapped.get(i), i == 0 ? 10 : 8, textX, baseline);
                baseline -= 13;
            }
            if (image != null) {
                float x = RIGHT - imageW - 10, imageY = top - imageH - 8;
                try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) { stream.drawImage(image, x, imageY, imageW, imageH); }
                for (int i = 0; i < attribution.size(); i++) text(attribution.get(i), 6, x, imageY - 9 - i * 8);
            }
            y -= cardH + 8;
        }
        private java.util.List<String> wrap(String value, int size, float maxWidth) {
            java.util.List<String> lines = new java.util.ArrayList<>(); StringBuilder current = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i); current.append(c);
                try {
                    if (font.getStringWidth(current.toString()) / 1000 * size > maxWidth && current.length() > 1) {
                        current.setLength(current.length() - 1); lines.add(current.toString()); current = new StringBuilder().append(c);
                    }
                } catch (java.io.IOException error) { throw new IllegalStateException("无法计算 PDF 字体宽度", error); }
            }
            if (!current.isEmpty()) lines.add(current.toString()); return lines;
        }
        private void textColor(Color color) { currentTextColor = color; }
        private void text(String value, int size, float x, float baseline) throws Exception {
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                stream.setNonStrokingColor(currentTextColor);
                stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x, baseline); stream.showText(value); stream.endText();
            }
        }
    }
}
