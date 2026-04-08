package org.example.vendor.client;

import org.example.vendor.client.dto.ReviewRemoteDto;
import org.example.vendor.client.dto.ReviewStatsRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "reviewMs", url = "${service.review.url:http://localhost:8085}", path = "/api/reviews")
public interface ReviewFeignClient {

    @GetMapping("/pair/stats")
    ReviewStatsRemoteDto getPairStats(
            @RequestParam("reviewerId") Long reviewerId,
            @RequestParam("revieweeId") Long revieweeId);

    @GetMapping("/pair")
    List<ReviewRemoteDto> getPairReviews(
            @RequestParam("reviewerId") Long reviewerId,
            @RequestParam("revieweeId") Long revieweeId);
}
