package tn.esprit.project.Services;

import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Repository.ProjectRepository;
import tn.esprit.project.Client.SkillClient;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Dto.Skills;
import tn.esprit.project.Entities.Enums.ProjectStatus;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;


import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class ProjectService implements IProjectService{

    private final ProjectRepository projectRepository;
    private final SkillClient skillClient;

    // ------------------- CRUD -------------------

    public Project addProject(Project project) {
        return projectRepository.save(project);
    }

    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // ------------------- ProjectResponse avec Skills -------------------
    public ProjectResponse getProjectResponse(Long id) {

        Project project = getProjectById(id);

        List<Skills> skills =
                (project.getSkillIds() == null || project.getSkillIds().isEmpty())
                        ? List.of()
                        : skillClient.getSkillsByIds(project.getSkillIds());

        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setClientId(project.getClientId());
        response.setTitle(project.getTitle());
        response.setDescription(project.getDescription());
        response.setBudget(project.getBudget());
        response.setDeadline(project.getDeadline());
        response.setStatus(project.getStatus().name());
        response.setCategory(project.getCategory());
        response.setSkills(skills);

        return response;
    }

    public List<ProjectResponse> getAllProjectResponses() {

        List<Project> projects = projectRepository.findAll();

        return projects.stream().map(project -> {

            List<Skills> skills =
                    (project.getSkillIds() == null || project.getSkillIds().isEmpty())
                            ? List.of()
                            : skillClient.getSkillsByIds(project.getSkillIds());

            ProjectResponse response = new ProjectResponse();
            response.setId(project.getId());
            response.setClientId(project.getClientId());
            response.setTitle(project.getTitle());
            response.setDescription(project.getDescription());
            response.setBudget(project.getBudget());
            response.setDeadline(project.getDeadline());
            response.setStatus(project.getStatus().name());
            response.setCategory(project.getCategory());
            response.setSkills(skills);

            return response;

        }).toList();
    }

    public List<Project> getProjectsByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    @Override
    public List<Project> getRecommendedProjects(Long freelancerId) {
        // 1️⃣ Fetch freelancer skills from PORTFOLIO microservice
        List<Skills> freelancerSkills = skillClient.getSkillsByUserId(freelancerId);

        if (freelancerSkills == null || freelancerSkills.isEmpty()) {
            return List.of(); // No skills → no recommendations
        }

        // 2️⃣ Extract skill IDs
        List<Long> freelancerSkillIds = freelancerSkills.stream()
                .map(Skills::getId)
                .toList();

        // 3️⃣ Use existing recommendation logic
        return getRecommendedProjects(freelancerSkillIds);
    }



    // ------------------- Recommandation -------------------

    public List<Project> getRecommendedProjects(List<Long> freelancerSkillIds) {
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN);

        return openProjects.stream()
                .filter(p -> p.getSkillIds() != null && !p.getSkillIds().isEmpty())
                .map(p -> new ProjectScore(p, calculateScore(p, freelancerSkillIds)))
                .filter(ps -> ps.getScore() > 0)
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(3)
                .map(ProjectScore::getProject)
                .collect(Collectors.toList());
    }

    private int calculateScore(Project project, List<Long> freelancerSkillIds) {
        if (project.getSkillIds() == null || project.getSkillIds().isEmpty()) return 0;
        return (int) project.getSkillIds()
                .stream()
                .filter(freelancerSkillIds::contains)
                .count();
    }

    // ------------------- Statistiques -------------------

    public Map<String, Object> getProjectStatistics() {
        List<Project> projects = projectRepository.findAll();

        long total = projects.size();
        long open = projects.stream().filter(p -> p.getStatus() == ProjectStatus.OPEN).count();
        long inProgress = projects.stream().filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS).count();
        long completed = projects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long cancelled = projects.stream().filter(p -> p.getStatus() == ProjectStatus.CANCELLED).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", total);
        stats.put("openProjects", open);
        stats.put("inProgressProjects", inProgress);
        stats.put("completedProjects", completed);
        stats.put("cancelledProjects", cancelled);

        return stats;
    }

    // ------------------- PDF Export ---------------
    @Override
    public byte[] exportProjectsToPdf() {
        List<Project> projects = projectRepository.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);

        // Metadata for professionalism
        pdf.getDocumentInfo().setTitle("Projects Report");
        pdf.getDocumentInfo().setAuthor("Your System");

        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        // ====================== COLOR PALETTE ======================
        DeviceRgb headerColor = new DeviceRgb(0, 51, 102);     // Professional navy
        DeviceRgb alternateRowColor = new DeviceRgb(245, 245, 245); // Soft light gray

        // ====================== TITLE & SUBTITLE ======================
        Paragraph title = new Paragraph("PROJECTS REPORT")
                .setFontSize(28)
                .setBold()
                .setFontColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        String generatedDate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss"));

        Paragraph subtitle = new Paragraph("Generated automatically on " + generatedDate)
                .setFontSize(12)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(subtitle);

        document.add(new Paragraph("\n"));

        // ====================== TABLE ======================
        Table table = new Table(5);
        table.setWidth(UnitValue.createPercentValue(100));

        // Header row (professional dark style)
        String[] headerTitles = {"Title", "Category", "Budget", "Status", "Deadline"};
        TextAlignment[] headerAlignments = {
                TextAlignment.LEFT, TextAlignment.LEFT,
                TextAlignment.RIGHT, TextAlignment.CENTER, TextAlignment.CENTER
        };

        for (int i = 0; i < 5; i++) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(headerTitles[i])
                            .setBold()
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(headerAlignments[i])
                    .setPadding(10)
                    .setBorder(new SolidBorder(ColorConstants.WHITE, 1));
            table.addHeaderCell(headerCell);
        }

        // Data rows with zebra striping + proper alignment
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        boolean alternate = false;

        for (Project project : projects) {
            DeviceRgb rowColor = alternate ? alternateRowColor : null;

            // Title
            table.addCell(createStyledCell(
                    project.getTitle() != null ? project.getTitle() : "-",
                    TextAlignment.LEFT, rowColor));

            // Category
            table.addCell(createStyledCell(
                    project.getCategory() != null ? project.getCategory() : "-",
                    TextAlignment.LEFT, rowColor));

            // Budget (right aligned)
            table.addCell(createStyledCell(
                    project.getBudget() != null ? project.getBudget().toString() : "-",
                    TextAlignment.RIGHT, rowColor));

            // Status (center)
            table.addCell(createStyledCell(
                    project.getStatus() != null ? project.getStatus().name() : "-",
                    TextAlignment.CENTER, rowColor));

            // Deadline (center)
            table.addCell(createStyledCell(
                    project.getDeadline() != null ? project.getDeadline().format(dateFormatter) : "-",
                    TextAlignment.CENTER, rowColor));

            alternate = !alternate;
        }

        document.add(table);

        // ====================== FOOTER SUMMARY ======================
        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph("Total Projects: " + projects.size())
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    // ====================== HELPER METHOD ======================
// Add this private method in the same service class
    private Cell createStyledCell(String content, TextAlignment alignment, DeviceRgb backgroundColor) {
        Cell cell = new Cell()
                .add(new Paragraph(content))
                .setTextAlignment(alignment)
                .setPadding(8)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.8f));

        if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }
        return cell;
    }

    // ------------------- Helper -------------------

    private static class ProjectScore {
        private final Project project;
        private final int score;

        public ProjectScore(Project project, int score) {
            this.project = project;
            this.score = score;
        }

        public Project getProject() { return project; }
        public int getScore() { return score; }
    }
}