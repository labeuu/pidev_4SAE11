package tn.esprit.project.Dto;

import lombok.Data;
import java.util.List;

@Data
public class Skills {
    private Long id;
    private String name;
    private List<String> domains; // 🛡️ Official plural list for Portfolio microservice
    private String description;
    private Long userId;
}
