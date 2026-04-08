package tn.esprit.gamification.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.gamification.Entities.Enums.conditionType;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private int xpReward;

    @Enumerated(EnumType.STRING)
    private conditionType conditionType;

    /** Valeur cible à atteindre pour débloquer cet achievement (ex: 5 projets). */
    @Column(columnDefinition = "int default 1")
    private int conditionThreshold = 1;

    /** Emoji affiché côté frontend pour représenter visuellement l'achievement. */
    @Column(length = 16)
    private String iconEmoji = "🏅";

    /** Rôle cible pour lequel cet achievement est valide. */
    @Enumerated(EnumType.STRING)
    private tn.esprit.gamification.Entities.Enums.TargetRole targetRole = tn.esprit.gamification.Entities.Enums.TargetRole.ALL;
}
