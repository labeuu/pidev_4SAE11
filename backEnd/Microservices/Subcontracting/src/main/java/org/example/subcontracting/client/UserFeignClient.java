package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.UserRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    UserRemoteDto getUserById(@PathVariable("id") Long id);

    @GetMapping
    List<UserRemoteDto> getAllUsers();
}
