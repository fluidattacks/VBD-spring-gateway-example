package com.basaki.k8s.service;

import com.basaki.k8s.error.exception.QuotaExceededException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@code CreationQuota} caps how many books a client may create per calendar
 * day. The allowance is tracked per authenticated principal.
 */
@Component
@Slf4j
public class CreationQuota {

    public static final String ANONYMOUS = "anonymous";

    private final int dailyLimit;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Autowired
    public CreationQuota(@Value("${book.quota.daily-limit:50}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Records one creation for the calling client and rejects the call when
     * the client has already reached its daily allowance.
     */
    public void consume() {
        String client = resolveClient();
        long today = LocalDate.now().toEpochDay();

        Counter counter = counters.compute(client,
                (k, c) -> (c == null || c.day != today) ? new Counter(today) : c);

        int used = counter.count.incrementAndGet();
        if (used > dailyLimit) {
            log.warn("Client {} exceeded the daily creation quota ({})", client, dailyLimit);
            throw new QuotaExceededException(
                    "Daily book creation limit of " + dailyLimit + " reached");
        }
    }

    String resolveClient() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }

        return ANONYMOUS;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    private static final class Counter {
        private final long day;

        private final AtomicInteger count = new AtomicInteger();

        private Counter(long day) {
            this.day = day;
        }
    }
}
