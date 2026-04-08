package tn.esprit.gamification.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gamification.Dto.AchievementDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Mapper.AchievementMapper;
import tn.esprit.gamification.Services.AchievementService;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    @Autowired
    private AchievementService service;

    @Autowired
    private AchievementMapper mapper;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Achievement create(@RequestBody AchievementDTO dto) {
        return service.create(mapper.toEntity(dto));
    }


    @GetMapping
    public List<Achievement> getAll() {
        return service.getAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Achievement update(@PathVariable Long id, @RequestBody AchievementDTO dto) {
        return service.update(id, mapper.toEntity(dto));
    }
}
