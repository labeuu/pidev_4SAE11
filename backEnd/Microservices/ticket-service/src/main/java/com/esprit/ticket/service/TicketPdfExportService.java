package com.esprit.ticket.service;

import com.esprit.ticket.dto.ticket.MonthlyTicketCount;
import com.esprit.ticket.dto.ticket.TicketStatsResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPdfExportService {

    private static final float MARGIN = 50f;
    private static final float LINE = 16f;

    private final TicketService ticketService;

    public byte[] buildMonthlyReportPdf() {
        TicketStatsResponse stats = ticketService.getStats();
        List<MonthlyTicketCount> monthly = ticketService.getMonthlyStats();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float y = page.getMediaBox().getHeight() - MARGIN;

            PDFont titleBold = PDType1Font.HELVETICA_BOLD;
            PDFont body = PDType1Font.HELVETICA;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                y = drawLine(cs, titleBold, 18, MARGIN, y, "Support tickets - report");
                y -= LINE;
                y = drawLine(cs, body, 10, MARGIN, y, "Generated: " + LocalDateTime.now());
                y -= LINE * 1.5f;

                y = drawLine(cs, titleBold, 12, MARGIN, y, "Summary");
                y -= LINE;
                y = drawLine(cs, body, 10, MARGIN, y, "Total tickets: " + stats.total());
                y -= LINE;
                y = drawLine(cs, body, 10, MARGIN, y, "Open: " + stats.open());
                y -= LINE;
                y = drawLine(cs, body, 10, MARGIN, y, "Closed: " + stats.closed());
                y -= LINE;
                String avg = stats.averageResponseTimeMinutes() != null
                        ? String.format("%.1f minutes", stats.averageResponseTimeMinutes())
                        : "N/A";
                y = drawLine(cs, body, 10, MARGIN, y, "Average first response time: " + avg);
                y -= LINE * 1.5f;

                y = drawLine(cs, titleBold, 12, MARGIN, y, "Tickets per month");
                y -= LINE;
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
                for (MonthlyTicketCount m : monthly) {
                    YearMonth ym = YearMonth.of(m.year(), m.month());
                    String row = String.format(
                            "%s | %d-%02d | %d",
                            ym.format(fmt), m.year(), m.month(), m.count());
                    y = drawLine(cs, body, 10, MARGIN, y, ascii(row));
                    y -= LINE;
                    if (y < MARGIN + LINE * 3) {
                        break;
                    }
                }
                if (monthly.isEmpty()) {
                    drawLine(cs, body, 10, MARGIN, y, "(no monthly data)");
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build PDF", e);
        }
    }

    private static float drawLine(PDPageContentStream cs, PDFont font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(text));
        cs.endText();
        return y - size - 4f;
    }

    /** Helvetica WinAnsi: keep printable ASCII to avoid encoding errors. */
    private static String ascii(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(Math.min(s.length(), 500));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 32 && c < 127 ? c : '?');
        }
        return sb.toString();
    }
}
