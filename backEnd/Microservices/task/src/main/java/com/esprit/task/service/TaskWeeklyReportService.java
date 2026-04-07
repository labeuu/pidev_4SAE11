package com.esprit.task.service;

import com.esprit.task.dto.TaskStatsExtendedDto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskWeeklyReportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_HIGH_PRIORITY_LINES = 20;

    private final TaskService taskService;

    public byte[] buildWeeklyPdf(Optional<Long> projectId, Optional<Long> freelancerId, LocalDate weekStartOrNull) {
        LocalDate monday = normalizeWeekStartMonday(weekStartOrNull);
        LocalDate sunday = monday.plusDays(6);
        TaskStatsExtendedDto stats = taskService.getExtendedStatsForWeeklyWindow(projectId, freelancerId, monday, sunday);
        List<String> highPriorityLines = taskService.getHighPriorityOpenLinesForReport(projectId, freelancerId, MAX_HIGH_PRIORITY_LINES);

        try {
            return renderPdf(
                    monday,
                    sunday,
                    true,
                    projectId,
                    freelancerId,
                    stats,
                    highPriorityLines);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to build PDF", e);
        }
    }

    /**
     * Inclusive rolling window: {@code periodEnd} and the previous {@code lastDays - 1} days (e.g. 7 → 7 calendar days ending on periodEnd).
     */
    public byte[] buildRollingPeriodPdf(
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            LocalDate periodEnd,
            int lastDays) {
        int days = Math.min(Math.max(lastDays, 1), 366);
        LocalDate start = periodEnd.minusDays(days - 1L);
        TaskStatsExtendedDto stats = taskService.getExtendedStatsForWeeklyWindow(projectId, freelancerId, start, periodEnd);
        List<String> highPriorityLines = taskService.getHighPriorityOpenLinesForReport(projectId, freelancerId, MAX_HIGH_PRIORITY_LINES);
        try {
            return renderPdf(
                    start,
                    periodEnd,
                    false,
                    projectId,
                    freelancerId,
                    stats,
                    highPriorityLines);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to build PDF", e);
        }
    }

    public static LocalDate normalizeWeekStartMonday(LocalDate weekStartOrNull) {
        LocalDate ref = weekStartOrNull != null ? weekStartOrNull : LocalDate.now();
        return ref.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static byte[] renderPdf(
            LocalDate periodStart,
            LocalDate periodEnd,
            boolean isoWeekMode,
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            TaskStatsExtendedDto stats,
            List<String> highPriorityLines) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        String title = isoWeekMode ? "Weekly task report" : "Work report (rolling period)";
        document.add(new Paragraph(title, titleFont));
        document.add(new Paragraph(
                String.format("Period: %s – %s (inclusive)", periodStart.format(FMT), periodEnd.format(FMT)),
                normal));
        String scope = "Scope: ";
        if (projectId.isPresent() && freelancerId.isPresent()) {
            scope += "project " + projectId.get() + ", freelancer " + freelancerId.get();
        } else if (projectId.isPresent()) {
            scope += "project " + projectId.get();
        } else if (freelancerId.isPresent()) {
            scope += "freelancer " + freelancerId.get();
        } else {
            scope += "all tasks";
        }
        document.add(new Paragraph(scope, normal));
        document.add(new Paragraph(Chunk.NEWLINE));

        document.add(new Paragraph("Summary", headFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        addRow(table, "Total (tasks + subtasks)", String.valueOf(stats.getTotalTasks()));
        addRow(table, "Done", String.valueOf(stats.getDoneCount()));
        addRow(table, "Todo", String.valueOf(stats.getTodoCount()));
        addRow(table, "In progress", String.valueOf(stats.getInProgressCount()));
        addRow(table, "In review", String.valueOf(stats.getInReviewCount()));
        addRow(table, "Cancelled", String.valueOf(stats.getCancelledCount()));
        String overdueLabel = isoWeekMode ? "Overdue (as of week end)" : "Overdue (as of period end)";
        String createdLabel = isoWeekMode ? "Created in week" : "Created in period";
        String completedLabel = isoWeekMode ? "Completed in week (approx.)" : "Completed in period (approx.)";
        addRow(table, overdueLabel, String.valueOf(stats.getOverdueCount()));
        addRow(table, "Unassigned", String.valueOf(stats.getUnassignedCount()));
        addRow(table, "Completion %", String.format("%.1f", stats.getCompletionPercentage()));
        addRow(table, createdLabel, String.valueOf(stats.getCreatedInRangeCount()));
        addRow(table, completedLabel, String.valueOf(stats.getCompletedInRangeCount()));
        document.add(table);

        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph("Priority breakdown", headFont));
        PdfPTable ptable = new PdfPTable(2);
        ptable.setWidthPercentage(60);
        ptable.setSpacingBefore(6);
        stats.getPriorityBreakdown().forEach(row -> addRow(ptable, row.getPriority().name(), String.valueOf(row.getCount())));
        document.add(ptable);

        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph("High / urgent open items (up to " + MAX_HIGH_PRIORITY_LINES + ")", headFont));
        if (highPriorityLines.isEmpty()) {
            document.add(new Paragraph("None.", normal));
        } else {
            for (String line : highPriorityLines) {
                document.add(new Paragraph("• " + line, normal));
            }
        }

        document.add(new Paragraph(Chunk.NEWLINE));
        String note = isoWeekMode
                ? "Note: “Completed in week” counts items in DONE whose updatedAt falls in the period (no dedicated audit log)."
                : "Note: “Completed in period” counts items in DONE whose updatedAt falls in the rolling window (no dedicated audit log).";
        document.add(new Paragraph(note, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY)));

        document.close();
        return out.toByteArray();
    }

    private static void addRow(PdfPTable table, String left, String right) {
        PdfPCell c1 = new PdfPCell(new Phrase(left, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        PdfPCell c2 = new PdfPCell(new Phrase(right, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        c1.setBorderColor(Color.LIGHT_GRAY);
        c2.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(c1);
        table.addCell(c2);
    }
}
