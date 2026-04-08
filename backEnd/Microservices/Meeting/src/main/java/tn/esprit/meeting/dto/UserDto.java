package tn.esprit.meeting.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String avatarUrl;

    public String getFullName() {
        if (firstName == null && lastName == null) return "User #" + id;
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}
