package com.aiops.context;

/**
 * Holds the durable AI job identity for work that runs outside an HTTP request thread.
 */
public final class AiJobContext {

    private static final ThreadLocal<ContextValue> CURRENT = new ThreadLocal<>();

    private AiJobContext() {
    }

    public static void set(Long jobId, String jobType) {
        CURRENT.set(new ContextValue(jobId, jobType));
    }

    public static Long getJobId() {
        ContextValue value = CURRENT.get();
        return value == null ? null : value.jobId();
    }

    public static String getJobType() {
        ContextValue value = CURRENT.get();
        return value == null ? null : value.jobType();
    }

    public static void remove() {
        CURRENT.remove();
    }

    private record ContextValue(Long jobId, String jobType) {
    }
}
