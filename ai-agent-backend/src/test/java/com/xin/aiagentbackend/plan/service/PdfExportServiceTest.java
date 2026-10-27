package com.xin.aiagentbackend.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.plan.dto.PdfExportRequest;
import com.xin.aiagentbackend.plan.dto.PlanDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfExportServiceTest {
    @Test
    void generatesReadableChinesePdfFromPlan() throws Exception {
        Path font = Path.of("C:/Windows/Fonts/simhei.ttf");
        Assumptions.assumeTrue(Files.isRegularFile(font), "Set TRAVEL_PDF_FONT to a TTF font for this environment");
        ObjectMapper mapper = new ObjectMapper();
        var itinerary = mapper.readTree("""
                {"city":"杭州","startDate":"2026-11-01","pace":"normal","budget":2000,
                 "estimatedActivityCost":120,"days":[{"day":1,"date":"2026-11-01","activities":[
                   {"name":"西湖","location":"杭州市西湖区","startTime":"09:00","endTime":"11:00","estimatedCost":120,"notes":"建议提前查询开放时间"}
                 ]}]}
                """);
        PlanDto plan = new PlanDto("test-plan", null, "杭州旅行计划", "杭州", "2026-11-01", 1,
                2000, "normal", itinerary, mapper.readTree("{\"estimatedTotal\":500,\"remainingBudget\":1500}"),
                1, 1, 1);
        PdfExportService service = new PdfExportService(font.toString());

        byte[] bytes = service.generate(plan, new PdfExportRequest("simple", true, true, false, true, true));

        assertTrue(bytes.length > 1000);
        assertEquals('%', (char) bytes[0]);
        try (var document = Loader.loadPDF(bytes)) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("杭州旅行攻略"));
            assertTrue(text.contains("西湖"));
            assertTrue(text.contains("预算概览"));
        }
    }

    @Test
    void magazineLayoutUsesDistinctCoverAndSectionDesign() throws Exception {
        Path font = Path.of("C:/Windows/Fonts/simhei.ttf");
        Assumptions.assumeTrue(Files.isRegularFile(font), "Set TRAVEL_PDF_FONT to a TTF font for this environment");
        ObjectMapper mapper = new ObjectMapper();
        var itinerary = mapper.readTree("""
                {"city":"杭州","startDate":"2026-11-01","pace":"normal","budget":2000,
                 "estimatedActivityCost":120,"days":[{"day":1,"date":"2026-11-01","activities":[
                   {"name":"西湖","location":"杭州市西湖区","startTime":"09:00","endTime":"11:00","estimatedCost":120,"notes":"建议提前查询开放时间","photoUrl":"https://images.pexels.com/photos/123/photo.jpeg","photoPageUrl":"https://www.pexels.com/photo/123/","photoPhotographer":"摄影师"}
                 ]}]}
                """);
        PlanDto plan = new PlanDto("test-plan", null, "杭州旅行计划", "杭州", "2026-11-01", 1,
                2000, "normal", itinerary, null, 1, 1, 1);
        byte[] image = sampleImage();
        PdfExportService service = new PdfExportService(font.toString(), url -> {
            assertEquals("https://images.pexels.com/photos/123/photo.jpeg", url);
            return image;
        });

        byte[] simple = service.generate(plan, new PdfExportRequest("simple", true, true, false, false, true));
        byte[] magazine = service.generate(plan, new PdfExportRequest("magazine", true, true, true, false, true));
        assertFalse(java.util.Arrays.equals(simple, magazine));
        try (var document = Loader.loadPDF(magazine)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("杭州旅行攻略"));
            assertTrue(text.contains("西湖"));
            assertTrue(document.getPage(0).getResources().getXObjectNames().iterator().hasNext(), "saved photo URL should be embedded in the PDF");
        }
    }

    private byte[] sampleImage() throws Exception {
        var image = new java.awt.image.BufferedImage(360, 180, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(94, 146, 119)); graphics.fillRect(0, 0, 360, 180);
        graphics.setColor(new java.awt.Color(227, 211, 151)); graphics.fillOval(220, 20, 90, 90);
        graphics.dispose();
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
