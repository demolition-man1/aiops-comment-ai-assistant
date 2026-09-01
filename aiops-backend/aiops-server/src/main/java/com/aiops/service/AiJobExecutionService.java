package com.aiops.service;

public interface AiJobExecutionService {

    void submit(Long jobId);

    boolean claim(Long jobId, String leaseOwner);
}
