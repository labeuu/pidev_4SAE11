package org.example.vendor.client.dto;

import lombok.Data;

@Data
public class UserNameRemoteDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
