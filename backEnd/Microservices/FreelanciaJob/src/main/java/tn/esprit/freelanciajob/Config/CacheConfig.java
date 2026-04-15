package tn.esprit.freelanciajob.Config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activates Spring's caching abstraction.
 * Without an explicit provider, Spring Boot uses ConcurrentMapCacheManager
 * (in-memory, no extra dependency required).
 *
 * To add TTL, replace with Caffeine or Redis:
 *   spring.cache.type=caffeine
 *   spring.cache.caffeine.spec=maximumSize=200,expireAfterWrite=5m
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
