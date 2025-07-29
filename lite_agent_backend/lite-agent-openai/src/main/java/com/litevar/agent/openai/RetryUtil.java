package com.litevar.agent.openai;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * @author uncle
 * @since 2025/3/3 10:07
 */
@Slf4j
public class RetryUtil {
    private static final Random RANDOM = new Random();

    public static <T> T withRetry(Callable<T> action, int maxAttempts) {
        int attempt = 1;
        while (true) {
            try {
                return action.call();
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    throw new RuntimeException(ex);
                }
                log.warn("Exception was thrown on attempt %s of %s".formatted(attempt, maxAttempts), ex);

                sleep(attempt);
            }
            attempt++;
        }
    }

    public static void sleep(int attempt) {
        try {
            Thread.sleep(jitterDelayMills(attempt));
        } catch (InterruptedException ex) {

        }
    }

    private static int jitterDelayMills(int attempt) {
        double delay = rawDelayMs(attempt);
        double jitterScale = 0.2;
        double jitter = delay * jitterScale;
        return (int) (delay + RANDOM.nextInt((int) jitter));
    }


    private static double rawDelayMs(int attempt) {
        int delayMillis = 1000;
        double backoffExp = 1.5;
        return ((double) delayMillis) * Math.pow(backoffExp, attempt - 1);
    }
}
