package org.example.vendor.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.example.vendor.dto.response.VendorDecisionInsightResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class VendorDecisionPdfService {

    private static final DateTimeFormatter HEADER_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generate(VendorDecisionInsightResponse d) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font small = new Font(Font.HELVETICA, 9, Font.NORMAL);
            Font section = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph(
                    "Rapport d'aide à la décision — Agrément fournisseur",
                    titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Généré le : " + LocalDateTime.now().format(HEADER_TS),
                    small));
            document.add(new Paragraph(" "));

            document.add(new Paragraph(
                    "Agrément n° " + d.getVendorApprovalId() + " — Statut : " + nullSafe(d.getStatus()),
                    normal));
            document.add(new Paragraph(
                    "Client (organisation) : " + nullSafe(d.getClientDisplayName()) + " [id=" + d.getOrganizationId() + "]",
                    normal));
            document.add(new Paragraph(
                    "Freelancer : " + nullSafe(d.getFreelancerDisplayName()) + " [id=" + d.getFreelancerId() + "]",
                    normal));
            document.add(new Paragraph(
                    "Domaine agrément : " + nullSafe(d.getAgreementDomain()),
                    normal));
            document.add(new Paragraph(
                    "Secteur métier : " + nullSafe(d.getProfessionalSector()),
                    normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("1. Projets communs (offres du client avec candidature du freelancer)", section));
            document.add(new Paragraph(
                    "Nombre de projets concernés : " + d.getSharedProjectCount(),
                    normal));
            if (d.getSharedProjects() != null && !d.getSharedProjects().isEmpty()) {
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.2f, 3f, 1.5f, 1.5f});
                addHeader(table, "ID", normal);
                addHeader(table, "Titre", normal);
                addHeader(table, "Statut", normal);
                addHeader(table, "Catégorie", normal);
                for (VendorDecisionInsightResponse.SharedProjectLine p : d.getSharedProjects()) {
                    addCell(table, String.valueOf(p.getId()), normal);
                    addCell(table, truncate(p.getTitle(), 80), normal);
                    addCell(table, nullSafe(p.getStatus()), normal);
                    addCell(table, nullSafe(p.getCategory()), normal);
                }
                document.add(table);
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("2. Avis du client vers le freelancer (sur projets)", section));
            document.add(new Paragraph(
                    "Nombre d'avis : " + d.getReviewCount() + " — Note moyenne : "
                            + String.format("%.2f", d.getAverageRatingFromClient()) + " / 5",
                    normal));
            if (d.getRatingDistribution() != null && !d.getRatingDistribution().isEmpty()) {
                StringBuilder sb = new StringBuilder("Répartition : ");
                for (Map.Entry<Integer, Long> e : d.getRatingDistribution().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                    sb.append(e.getKey()).append("★=").append(e.getValue()).append("  ");
                }
                document.add(new Paragraph(sb.toString(), normal));
            }
            document.add(new Paragraph(" "));

            if (d.getReviews() != null && !d.getReviews().isEmpty()) {
                PdfPTable rev = new PdfPTable(4);
                rev.setWidthPercentage(100);
                rev.setWidths(new float[]{1f, 1f, 0.8f, 4f});
                addHeader(rev, "Date", normal);
                addHeader(rev, "Projet", normal);
                addHeader(rev, "Note", normal);
                addHeader(rev, "Commentaire", normal);
                for (VendorDecisionInsightResponse.ClientToFreelancerReviewLine r : d.getReviews()) {
                    addCell(rev, nullSafe(r.getCreatedAt()), small);
                    addCell(rev, String.valueOf(r.getProjectId()), small);
                    addCell(rev, r.getRating() != null ? String.valueOf(r.getRating()) : "—", small);
                    addCell(rev, truncate(nullSafe(r.getComment()), 500), small);
                }
                document.add(rev);
            }

            if (d.getDataWarnings() != null && !d.getDataWarnings().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Avertissements", new Font(Font.HELVETICA, 11, Font.BOLD)));
                for (String w : d.getDataWarnings()) {
                    document.add(new Paragraph("• " + truncate(w, 300), small));
                }
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Document produit pour faciliter la validation ou le renouvellement de l'agrément fournisseur.",
                    small));

        } catch (DocumentException e) {
            log.error("PDF generation failed", e);
            throw new IllegalStateException("Impossible de générer le PDF", e);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private static void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(new java.awt.Color(230, 230, 240));
        table.addCell(c);
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new Phrase(text != null ? text : "", font));
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.min(max, s.length())) + "…";
    }
}
