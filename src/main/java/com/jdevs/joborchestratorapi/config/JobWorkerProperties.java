package com.jdevs.joborchestratorapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "job-worker")
public class JobWorkerProperties {

    private boolean enabled = true;

    private int batchSize = 5;

    private long fixedDelayMs = 10000;

    private long retryDelaySeconds = 15;

    private long staleTimeoutSeconds = 60;

    private long recoveryFixedDelayMs = 30000;

    private String workerId = "local-worker-1";
}
