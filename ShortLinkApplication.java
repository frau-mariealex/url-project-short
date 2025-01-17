package com.example.urlprojectshort;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@EnableScheduling
public class ShortLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShortLinkApplication.class, args);
    }

    @Bean
    public Map<String, ShortLink> shortLinkStore() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConfigProperties configProperties() {
        return new ConfigProperties();
    }

    @Bean
    public Map<String, String> userStore() {
        return new ConcurrentHashMap<>();
    }
}

@RestController
@RequestMapping("/api")
class ShortLinkController {

    private final Map<String, ShortLink> shortLinkStore;
    private final Map<String, String> userStore;
    private final ConfigProperties configProperties;

    public ShortLinkController(Map<String, ShortLink> shortLinkStore, Map<String, String> userStore, ConfigProperties configProperties) {
        this.shortLinkStore = shortLinkStore;
        this.userStore = userStore;
        this.configProperties = configProperties;
    }

    @PostMapping("/shorten")
    public Map<String, String> shortenUrl(@RequestParam String originalUrl,
                                          @RequestParam(required = false) Integer maxClicks,
                                          @RequestParam(required = false) Integer expiryDurationInHours,
                                          @RequestHeader(value = "userId", required = false) String userId) {
        if (userId == null || !userStore.containsKey(userId)) {
            userId = UUID.randomUUID().toString();
            userStore.put(userId, "");
        }

        int finalMaxClicks = maxClicks != null ? maxClicks : configProperties.getMaxClicks();
        int finalExpiryDurationInHours = expiryDurationInHours != null ? expiryDurationInHours : configProperties.getExpiryDurationInHours();

        String id = UUID.randomUUID().toString().substring(0, 8);
        shortLinkStore.put(id, new ShortLink(originalUrl, id, userId, LocalDateTime.now().plusHours(finalExpiryDurationInHours), finalMaxClicks));

        return Map.of("shortUrl", "http://localhost:8080/api/" + id, "userId", userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Void> redirectToOriginal(@PathVariable String id) {
        ShortLink shortLink = shortLinkStore.get(id);
        if (shortLink == null || LocalDateTime.now().isAfter(shortLink.getExpiryTime())) {
            return ResponseEntity.status(404).build();
        }
        if (shortLink.getClickCount() >= shortLink.getMaxClicks()) {
            return ResponseEntity.status(403).build();
        }
        shortLink.incrementClickCount();
        return ResponseEntity.status(302).location(URI.create(shortLink.getOriginalUrl())).build();
    }

    @PutMapping("/update/{id}")
    public Map<String, String> updateMaxClicks(@PathVariable String id, @RequestParam int newMaxClicks,
                                               @RequestHeader(value = "userId") String userId) {
        ShortLink shortLink = shortLinkStore.get(id);
        if (shortLink == null) {
            return Map.of("message", "Link does not exist.");
        }
        if (!shortLink.getUserId().equals(userId)) {
            return Map.of("message", "You do not have permission to update this link.");
        }
        if (newMaxClicks <= shortLink.getClickCount()) {
            return Map.of("message", "New max clicks must be greater than current click count.");
        }
        shortLink.setMaxClicks(newMaxClicks);
        return Map.of("message", "Max clicks updated successfully.");
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, String> deleteLink(@PathVariable String id, @RequestHeader(value = "userId") String userId) {
        ShortLink shortLink = shortLinkStore.get(id);
        if (shortLink == null) {
            return Map.of("message", "Link does not exist.");
        }
        if (!shortLink.getUserId().equals(userId)) {
            return Map.of("message", "You do not have permission to delete this link.");
        }
        shortLinkStore.remove(id);
        return Map.of("message", "Link deleted successfully.");
    }

    @GetMapping("/{id}/stats")
    public Map<String, Object> getLinkStats(@PathVariable String id, @RequestHeader(value = "userId") String userId) {
        ShortLink shortLink = shortLinkStore.get(id);
        if (shortLink == null) {
            return Map.of("message", "Link does not exist.");
        }
        if (!shortLink.getUserId().equals(userId)) {
            return Map.of("message", "You do not have permission to view this link.");
        }
        return Map.of(
                "originalUrl", shortLink.getOriginalUrl(),
                "expiryTime", shortLink.getExpiryTime(),
                "clickCount", shortLink.getClickCount(),
                "maxClicks", shortLink.getMaxClicks()
        );
    }

    @GetMapping("/user-links")
    public Map<String, List<String>> getUserLinks(@RequestHeader(value = "userId") String userId) {
        List<String> userLinks = new ArrayList<>();
        for (Map.Entry<String, ShortLink> entry : shortLinkStore.entrySet()) {
            if (entry.getValue().getUserId().equals(userId)) {
                userLinks.add(entry.getKey());
            }
        }
        return Map.of("links", userLinks);
    }
}

@Component
class LinkCleanupTask {

    private final Map<String, ShortLink> shortLinkStore;

    public LinkCleanupTask(Map<String, ShortLink> shortLinkStore) {
        this.shortLinkStore = shortLinkStore;
    }

    @Scheduled(fixedRate = 60000) // Запускается каждые 60 секунд
    public void cleanupExpiredLinks() {
        shortLinkStore.entrySet().removeIf(entry -> LocalDateTime.now().isAfter(entry.getValue().getExpiryTime()));
    }
}

class ShortLink {
    private final String originalUrl;
    private final String id;
    private final String userId;
    private final LocalDateTime expiryTime;
    private int maxClicks;
    private int clickCount;

    public ShortLink(String originalUrl, String id, String userId, LocalDateTime expiryTime, int maxClicks) {
        this.originalUrl = originalUrl;
        this.id = id;
        this.userId = userId;
        this.expiryTime = expiryTime;
        this.maxClicks = maxClicks;
        this.clickCount = 0;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public void setMaxClicks(int maxClicks) {
        this.maxClicks = maxClicks;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void incrementClickCount() {
        this.clickCount++;
    }
}

class ConfigProperties {
    private int maxClicks = 10;
    private int expiryDurationInHours = 24;

    public int getMaxClicks() {
        return maxClicks;
    }

    public void setMaxClicks(int maxClicks) {
        this.maxClicks = maxClicks;
    }

    public int getExpiryDurationInHours() {
        return expiryDurationInHours;
    }

    public void setExpiryDurationInHours(int expiryDurationInHours) {
        this.expiryDurationInHours = expiryDurationInHours;
    }
}
