package com.aciworldwide.eccn_management_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, ClientRequestTracker> clientRequests = new ConcurrentHashMap<>();

    private static class ClientRequestTracker {
        private final long windowStartTime;
        private final AtomicInteger count;

        public ClientRequestTracker(long windowStartTime) {
            this.windowStartTime = windowStartTime;
            this.count = new AtomicInteger(1);
        }

        public boolean tryConsume(long now) {
            if (now - windowStartTime > 60000) {
                return false; // Window expired
            }
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        ClientRequestTracker tracker = clientRequests.compute(clientIp, (ip, existingTracker) -> {
            if (existingTracker == null || !existingTracker.tryConsume(now)) {
                return new ClientRequestTracker(now);
            }
            return existingTracker;
        });

        if (tracker.count.get() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too Many Requests - Rate Limit Exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
