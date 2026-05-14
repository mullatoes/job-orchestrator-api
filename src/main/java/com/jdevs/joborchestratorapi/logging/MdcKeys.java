package com.jdevs.joborchestratorapi.logging;

public final class MdcKeys {

    private MdcKeys() {
    }

    public static final String CORRELATION_ID = "correlationId";
    public static final String JOB_ID = "jobId";
    public static final String WORKER_ID = "workerId";
}
