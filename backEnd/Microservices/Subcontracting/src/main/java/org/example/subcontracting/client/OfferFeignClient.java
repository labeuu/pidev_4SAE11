package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.OfferRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "offerMs", url = "${service.offer.url:http://localhost:8082}", path = "/api/offers")
public interface OfferFeignClient {

    @GetMapping("/{id}")
    OfferRemoteDto getOfferById(@PathVariable("id") Long id);
}
