package tn.esprit.project.Dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewSkillRequest {
    private String name;
    private List<String> domains; // 🛡️ Les domaines (ex: ["WEB_DEVELOPMENT"])
}
