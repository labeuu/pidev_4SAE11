package tn.esprit.project.Services;

import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Repository.ProjectApplicationRepository;
import tn.esprit.project.Repository.ProjectRepository;
import tn.esprit.project.Client.SkillClient;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Dto.Skills;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.Enums.ProjectStatus;

import java.util.*;
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
    private final ProjectApplicationRepository applicationRepository; // 🆕 Added
    private final SkillClient skillClient;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher; // 🆕 Pour les transactions

    // ------------------- CRUD -------------------

    public Project addProject(tn.esprit.project.Dto.request.ProjectRequest request) {
        Project project = new Project();
        project.setClientId(request.getClientId());
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setBudget(request.getBudget());
        project.setDeadline(request.getDeadline());
        project.setCategory(request.getCategory());
        project.setStatus(ProjectStatus.OPEN);

        // 🛡️ List of final IDs
        List<Long> finalSkillIds = new ArrayList<>();
        if (request.getSkillIds() != null) {
            finalSkillIds.addAll(request.getSkillIds());
        }

        // 🆕 Création dynamique de nouveaux skills (Nom + Domaines) dans Portfolio
        if (request.getNewSkills() != null && !request.getNewSkills().isEmpty()) {
            for (tn.esprit.project.Dto.request.NewSkillRequest ns : request.getNewSkills()) {
                try {
                    Skills newSkill = new Skills();
                    newSkill.setName(ns.getName());
                    newSkill.setDomains(ns.getDomains()); // ⚙️ Official plural list
                    newSkill.setUserId(request.getClientId()); 
                    
                    Skills registered = skillClient.createSkill(newSkill);
                    if (registered != null && registered.getId() != null) {
                        finalSkillIds.add(registered.getId());
                    }
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(ProjectService.class)
                        .error("Failed to register skill '{}' in Portfolio: {}", ns.getName(), e.getMessage());
                }
            }
        }
        
        project.setSkillIds(finalSkillIds);

        Project saved = projectRepository.save(project);
        
        // 🎯 Gamification
        if (saved.getClientId() != null) {
            eventPublisher.publishEvent(new ProjectCreatedEvent(saved.getClientId()));
        }
        
        return saved;
    }

    @lombok.Value
    public static class ProjectCreatedEvent {
        Long clientId;
    }

    // 🆕 Evénement pour le Freelancer (Complétion)
    @lombok.Value
    public static class ProjectCompletedEvent {
        Long freelancerId;
    }

    /**
     * Clôture un projet et notifie la gamification pour récompenser le Freelancer.
     */
    public Project completeProject(Long projectId) {
        Project project = getProjectById(projectId);
        project.setStatus(ProjectStatus.COMPLETED);
        Project saved = projectRepository.save(project);

        // 🎯 On cherche le freelancer qui a gagné l'offre pour le récompenser
        applicationRepository.findByProjectId(projectId).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED)
                .findFirst()
                .ifPresent(acceptedApp -> {
                    eventPublisher.publishEvent(new ProjectCompletedEvent(acceptedApp.getFreelanceId()));
                });

        return saved;
    }

    public Project updateProject(Project project) {
        if (project.getId() == null) return projectRepository.save(project);

        Project existing = getProjectById(project.getId());
        
        // 1. Détection changement de statut (pour gamification)
        boolean justCompleted = existing.getStatus() != ProjectStatus.COMPLETED && 
                               project.getStatus() == ProjectStatus.COMPLETED;

        // 2. Mise à jour sélective (Ne JAMAIS écraser avec null les champs critiques)
        if (project.getTitle() != null) existing.setTitle(project.getTitle());
        if (project.getDescription() != null) existing.setDescription(project.getDescription());
        if (project.getBudget() != null) existing.setBudget(project.getBudget());
        if (project.getDeadline() != null) existing.setDeadline(project.getDeadline());
        if (project.getCategory() != null) existing.setCategory(project.getCategory());
        if (project.getStatus() != null) existing.setStatus(project.getStatus());
        if (project.getSkillIds() != null && !project.getSkillIds().isEmpty()) {
            existing.setSkillIds(project.getSkillIds());
        }

        // On préserve explicitement les applications pour ne PAS les supprimer !
        // existing.getApplications() est déjà rempli par Hibernate
        
        Project saved = projectRepository.save(existing); // 🛡️ Sauvegarde de l'objet déjà en base

        // 🎯 Déclenchement APRES sauvegarde
        if (justCompleted) {
            applicationRepository.findByProjectId(saved.getId()).stream()
                    .filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED)
                    .findFirst()
                    .ifPresent(acceptedApp -> {
                        eventPublisher.publishEvent(new ProjectCompletedEvent(acceptedApp.getFreelanceId()));
                    });
        }
        return saved;
    }

    public void deleteProject(Long id) {
        // 🛡️ Nettoyage des applications orphelines avant la suppression du projet
        // pour ne pas avoir d'erreur de clé étrangère (Custom cleanup)
        applicationRepository.deleteByProjectId(id);
        
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
        response.setSkillIds(project.getSkillIds() != null ? project.getSkillIds() : List.of());
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
            response.setSkillIds(project.getSkillIds() != null ? project.getSkillIds() : List.of());
            response.setSkills(skills);

            return response;

        }).toList();
    }

    public List<Project> getProjectsByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    @Override
    public List<ProjectResponse> getRecommendedProjects(Long freelancerId) {
        // 1️⃣ Fetch freelancer skills from PORTFOLIO microservice (with fallback)
        List<Skills> freelancerSkills;
        try {
            freelancerSkills = skillClient.getSkillsByUserId(freelancerId);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ProjectService.class)
                    .warn("Failed to fetch skills from Portfolio for userId={}: {} — returning no recommendations",
                            freelancerId, e.getMessage());
            return List.of();
        }

        if (freelancerSkills == null || freelancerSkills.isEmpty()) {
            return List.of(); // No skills → no recommendations
        }

        // 2️⃣ Extract freelancer skill names (normalized for matching)
        // Match by NAME not ID: projects use skillIds from getAllSkills(), freelancers have
        // their own skill rows with different IDs. Same skill name = match.
        Set<String> freelancerSkillNames = freelancerSkills.stream()
                .map(s -> s.getName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(n -> !n.isEmpty())
                .collect(Collectors.toSet());

        if (freelancerSkillNames.isEmpty()) {
            return List.of();
        }

        // 3️⃣ Get matching open projects (by name overlap) and convert to ProjectResponse
        List<Project> matchingProjects = getRecommendedProjectsBySkillNames(freelancerSkillNames);
        return matchingProjects.stream()
                .map(p -> getProjectResponse(p.getId()))
                .limit(6)
                .toList();
    }

    // ------------------- Recommandation -------------------

    /** Returns projects matching freelancer skills by skill NAME overlap (not ID). */
    private List<Project> getRecommendedProjectsBySkillNames(Set<String> freelancerSkillNames) {
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN);
        List<Project> withSkills = openProjects.stream()
                .filter(p -> p.getSkillIds() != null && !p.getSkillIds().isEmpty())
                .toList();

        if (withSkills.isEmpty()) return List.of();

        // Batch-load all project skill names in one Feign call
        Set<Long> allSkillIds = withSkills.stream()
                .flatMap(p -> p.getSkillIds().stream())
                .collect(Collectors.toSet());
        Map<Long, String> idToName = resolveSkillNames(new ArrayList<>(allSkillIds));

        return withSkills.stream()
                .map(p -> new ProjectScore(p, calculateScoreByName(p, freelancerSkillNames, idToName)))
                .filter(ps -> ps.getScore() > 0)
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(6)
                .map(ProjectScore::getProject)
                .collect(Collectors.toList());
    }

    private Map<Long, String> resolveSkillNames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        try {
            List<Skills> skills = skillClient.getSkillsByIds(ids);
            if (skills == null) return Map.of();
            return skills.stream()
                    .filter(s -> s.getId() != null && s.getName() != null)
                    .collect(Collectors.toMap(Skills::getId, s -> s.getName().trim().toLowerCase(), (a, b) -> a));
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Score = number of project skill names that match freelancer skill names (case-insensitive). */
    private int calculateScoreByName(Project project, Set<String> freelancerSkillNames, Map<Long, String> idToName) {
        if (project.getSkillIds() == null || project.getSkillIds().isEmpty()) return 0;
        return (int) project.getSkillIds().stream()
                .map(idToName::get)
                .filter(Objects::nonNull)
                .filter(freelancerSkillNames::contains)
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

// ------------------- Game Stats 🆕 -------------------
    @Override
    public long countCompletedProjectsByFreelancer(Long freelancerId) {
        return applicationRepository.countByFreelanceIdAndStatusAndProject_Status(
                freelancerId, ApplicationStatus.ACCEPTED, ProjectStatus.COMPLETED);
    }

    @Override
    public long countCreatedProjectsByClient(Long clientId) {
        return projectRepository.countByClientId(clientId);
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