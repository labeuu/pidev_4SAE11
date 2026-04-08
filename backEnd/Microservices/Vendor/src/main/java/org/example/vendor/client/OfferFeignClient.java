package org.example.vendor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for the Offer microservice — used for emergency revocation
 * to block active applications linked to a suspended vendor agreement.
 */
@FeignClient(name = "offerMs", url = "${service.offer.url:http://localhost:8086}", path = "/api")
public interface OfferFeignClient {

    @GetMapping("/applications/count-active")
    int countActiveApplications(
            @RequestParam("organizationId") Long organizationId,
            @RequestParam("freelancerId") Long freelancerId);

    @PatchMapping("/applications/block-by-vendor")
    int blockApplicationsByVendor(
            @RequestParam("organizationId") Long organizationId,
            @RequestParam("freelancerId") Long freelancerId,
            @RequestParam("reason") String reason);
}
