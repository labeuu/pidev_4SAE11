package tn.esprit.freelanciajob.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight projection of a user returned by the USER microservice.
 * Only the fields needed for email notifications are declared here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role; // "CLIENT" | "FREELANCER" | "ADMIN"
}
