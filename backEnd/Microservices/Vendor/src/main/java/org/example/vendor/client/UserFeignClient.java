package org.example.vendor.client;

import org.example.vendor.client.dto.UserNameRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userMs", url = "${service.user.url:http://localhost:8090}", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    UserNameRemoteDto getUserById(@PathVariable("id") Long id);
}
