package com.esprit.ticket.service;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.ticket.MonthlyTicketCount;
import com.esprit.ticket.dto.ticket.TicketStatsResponse;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketPdfExportService {

    private static final float MARGIN = 48f;
    private static final float FOOTER_GAP = 36f;
    private static final float LINE = 14f;
    private static final float BANNER_H = 52f;
    private static final int BACKLOG_ROWS = 18;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter GEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;

    public byte[] buildMonthlyReportPdf() {
        TicketStatsResponse stats = ticketService.getStats();
        List<MonthlyTicketCount> monthly = ticketService.getMonthlyStats();
        Map<TicketPriority, Long> byPriority = loadPriorityCounts();
        long reopened = ticketRepository.countByReopenCountGreaterThan(0);
        List<Ticket> openBacklog = ticketRepository.findByStatusOrderByLastActivityAtDesc(
            TicketStatus.OPEN, PageRequest.of(0, BACKLOG_ROWS));

        try (PDDocument document = new PDDocument()) {
            document.getDocumentInformation().setTitle("Support tickets report");
            document.getDocumentInformation().setSubject("Monthly volume, priorities, and open backlog");
            document.getDocumentInformation().setAuthor("Ticket service");

            PdfWriter w = new PdfWriter(document);
            w.openPage();

            w.drawBanner("Support tickets report", "Volume, SLA snapshot, and open backlog");

            w.sectionHeading("Executive summary");
            w.bodyLine(String.format("Total tickets: %d   Open: %d   Closed: %d", stats.total(), stats.open(), stats.closed()));
            String avg = stats.averageResponseTimeMinutes() != null
                ? String.format("Average first response: %.1f minutes", stats.averageResponseTimeMinutes())
                : "Average first response: N/A (no admin replies yet)";
            w.bodyLine(avg);
            w.bodyLine(String.format("Tickets reopened at least once: %d", reopened));
            w.spacer();

            w.sectionHeading("Tickets by priority");
            w.tableHeader(new String[] {"Priority", "Count", "Share"});
            long total = Math.max(1, stats.total());
            for (TicketPriority p : TicketPriority.values()) {
                long c = byPriority.getOrDefault(p, 0L);
                String share = String.format(Locale.US, "%.1f%%", (100.0 * c) / total);
                w.tableRow(new String[] {p.name(), String.valueOf(c), share});
            }
            w.spacer();

            w.sectionHeading("Tickets created per month");
            w.tableHeader(new String[] {"Period", "Tickets", "% of total"});
            if (monthly.isEmpty()) {
                w.bodyLine("(No ticket creation history yet.)");
            } else {
                for (MonthlyTicketCount m : monthly) {
                    w.ensureSpace(LINE * 1.4f);
                    YearMonth ym = YearMonth.of(m.year(), m.month());
                    String period = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.US)
                        + " " + m.year();
                    String pct = String.format(Locale.US, "%.1f%%", (100.0 * m.count()) / total);
                    w.tableRow(new String[] {period, String.valueOf(m.count()), pct});
                }
            }
            w.spacer();

            w.sectionHeading("Open backlog (most stale activity first)");
            w.tableHeader(new String[] {"ID", "Pri", "User", "Last activity", "Subject"});
            if (openBacklog.isEmpty()) {
                w.bodyLine("(No open tickets.)");
            } else {
                for (Ticket t : openBacklog) {
                    w.ensureSpace(LINE * 1.35f);
                    String subj = truncate(t.getSubject(), 42);
                    w.tableRow(new String[] {
                        String.valueOf(t.getId()),
                        t.getPriority().name(),
                        String.valueOf(t.getUserId()),
                        t.getLastActivityAt() != null ? t.getLastActivityAt().format(TS) : "-",
                        subj
                    });
                }
            }

            w.finishLastPage();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build PDF", e);
        }
    }

    private Map<TicketPriority, Long> loadPriorityCounts() {
        Map<TicketPriority, Long> map = new EnumMap<>(TicketPriority.class);
        for (TicketPriority p : TicketPriority.values()) {
            map.put(p, 0L);
        }
        for (Object[] row : ticketRepository.countGroupedByPriority()) {
            TicketPriority p = (TicketPriority) row[0];
            long c = ((Number) row[1]).longValue();
            map.put(p, c);
        }
        return map;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String a = ascii(s);
        return a.length() <= max ? a : a.substring(0, max - 1) + "...";
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

    private static final class PdfWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;
        private int pageIndex;

        private final float pageW;
        private final float pageH;

        PdfWriter(PDDocument document) {
            this.document = document;
            this.pageW = PDRectangle.A4.getWidth();
            this.pageH = PDRectangle.A4.getHeight();
        }

        void openPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            cs = new PDPageContentStream(document, page);
            pageIndex++;
            y = pageH - MARGIN;
        }

        void finishLastPage() throws IOException {
            drawFooter();
            cs.close();
            cs = null;
        }

        void drawBanner(String title, String subtitle) throws IOException {
            float top = pageH;
            cs.setNonStrokingColor(0.18f, 0.22f, 0.28f);
            cs.addRect(0, top - BANNER_H, pageW, BANNER_H);
            cs.fill();
            cs.setNonStrokingColor(0.95f, 0.96f, 0.98f);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.newLineAtOffset(MARGIN, top - BANNER_H + 22);
            cs.showText(ascii(title));
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.newLineAtOffset(MARGIN, top - BANNER_H + 8);
            cs.showText(ascii(subtitle));
            cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
            y = top - BANNER_H - 18f;
        }

        void sectionHeading(String text) throws IOException {
            ensureSpace(LINE * 2.2f);
            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(ascii(text));
            cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
            y -= LINE * 1.35f;
            float yLine = y + LINE * 0.35f;
            cs.moveTo(MARGIN, yLine);
            cs.lineTo(pageW - MARGIN, yLine);
            cs.setStrokingColor(0.75f, 0.75f, 0.78f);
            cs.setLineWidth(0.6f);
            cs.stroke();
            cs.setStrokingColor(0f, 0f, 0f);
            y -= LINE * 0.5f;
        }

        void bodyLine(String text) throws IOException {
            ensureSpace(LINE * 1.2f);
            drawWrapped(PDType1Font.HELVETICA, 9.5f, MARGIN, pageW - 2 * MARGIN, ascii(text));
        }

        void spacer() throws IOException {
            y -= LINE * 0.65f;
        }

        void tableHeader(String[] cols) throws IOException {
            ensureSpace(LINE * 1.8f);
            float[] widths = columnWidths(cols.length);
            float tableW = pageW - 2 * MARGIN;
            cs.setNonStrokingColor(0.93f, 0.94f, 0.96f);
            cs.addRect(MARGIN, y - LINE * 1.05f, tableW, LINE * 1.15f);
            cs.fill();
            cs.setNonStrokingColor(0f, 0f, 0f);
            float x = MARGIN + 2f;
            for (int i = 0; i < cols.length; i++) {
                float colW = widths[i] * tableW;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
                cs.newLineAtOffset(x, y - 2f);
                cs.showText(ascii(cols[i]));
                cs.endText();
                x += colW;
            }
            y -= LINE * 1.35f;
            rule();
            y -= LINE * 0.25f;
        }

        void tableRow(String[] cells) throws IOException {
            ensureSpace(LINE * 1.25f);
            float[] widths = columnWidths(cells.length);
            float tableW = pageW - 2 * MARGIN;
            float x = MARGIN + 2f;
            for (int i = 0; i < cells.length; i++) {
                float colW = widths[i] * tableW;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8.5f);
                cs.newLineAtOffset(x, y);
                cs.showText(ascii(cells[i]));
                cs.endText();
                x += colW;
            }
            y -= LINE * 1.05f;
        }

        private static float[] columnWidths(int n) {
            return switch (n) {
                case 3 -> new float[] {0.42f, 0.28f, 0.30f};
                case 5 -> new float[] {0.10f, 0.12f, 0.12f, 0.26f, 0.40f};
                default -> {
                    float w = 1f / n;
                    float[] a = new float[n];
                    java.util.Arrays.fill(a, w);
                    yield a;
                }
            };
        }

        void rule() throws IOException {
            cs.setStrokingColor(0.82f, 0.82f, 0.85f);
            cs.setLineWidth(0.4f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(pageW - MARGIN, y);
            cs.stroke();
            cs.setStrokingColor(0f, 0f, 0f);
        }

        void drawWrapped(PDFont font, float size, float left, float maxWidth, String text) throws IOException {
            if (text == null || text.isBlank()) {
                return;
            }
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String trial = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(trial) / 1000f * size > maxWidth && !line.isEmpty()) {
                    flushLine(font, size, left, line.toString(), maxWidth);
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(trial);
                }
            }
            if (!line.isEmpty()) {
                flushLine(font, size, left, line.toString(), maxWidth);
            }
        }

        private void flushLine(PDFont font, float size, float left, String text, float maxWidth) throws IOException {
            ensureSpace(size + 4f);
            String t = text;
            while (t.length() > 1 && font.getStringWidth(t) / 1000f * size > maxWidth) {
                t = t.substring(0, t.length() - 1);
            }
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(left, y);
            cs.showText(t);
            cs.endText();
            y -= size + 3f;
        }

        void ensureSpace(float needed) throws IOException {
            if (y - needed < FOOTER_GAP + MARGIN) {
                drawFooter();
                cs.close();
                openPage();
                drawBanner("Support tickets report (cont.)", "Monthly volume, priorities, and open backlog");
                y -= 6f;
            }
        }

        void drawFooter() throws IOException {
            float fy = MARGIN * 0.65f;
            cs.setNonStrokingColor(0.45f, 0.45f, 0.48f);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 8);
            cs.newLineAtOffset(MARGIN, fy);
            cs.showText("Generated " + LocalDateTime.now().format(GEN) + "   |   Page " + pageIndex);
            cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
        }
    }
}
