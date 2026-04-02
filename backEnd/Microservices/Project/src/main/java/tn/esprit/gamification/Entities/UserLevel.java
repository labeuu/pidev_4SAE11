package tn.esprit.gamification.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.catalina.User;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private int xp = 0;
    private int level = 1;

    // 🆕 Nouveaux champs
    private int fastResponderStreak = 0;  // compteur de réponses rapides
    private boolean isTopFreelancer = false; // badge actif ou non
}
