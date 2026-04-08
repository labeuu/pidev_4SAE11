package tn.esprit.gamification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "review-service",
        url = "${review.service.url:http://localhost:8085}", // 🛠 Port corrigé (8085)
        path = "/api/reviews"
)
public interface ReviewClient {

    @GetMapping("/reviewer/{reviewerId}")
    java.util.List<Object> getReviewsByReviewerId(@PathVariable("reviewerId") Long reviewerId);
}
