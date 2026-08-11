package com.careerpilot.presentation.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> matchBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        Bucket bucket;

        if (path.equals("/api/v1/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(ip, this::newLoginBucket);
        } else if (path.equals("/api/v1/auth/register")) {
            bucket = registerBuckets.computeIfAbsent(ip, this::newRegisterBucket);
        } else if (path.equals("/api/v1/resumes/upload")) {
            bucket = uploadBuckets.computeIfAbsent(ip, this::newUploadBucket);
        } else if (path.matches("/api/v1/jobs/[^/]+/match")) {
            bucket = matchBuckets.computeIfAbsent(ip, this::newMatchBucket);
        } else if (path.startsWith("/api/v1/")) {
            bucket = apiBuckets.computeIfAbsent(ip, this::newApiBucket);
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
        }
    }

    private Bucket newLoginBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket newRegisterBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1))))
                .build();
    }

    private Bucket newUploadBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1))))
                .build();
    }

    private Bucket newMatchBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1))))
                .build();
    }

    private Bucket newApiBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }
}
