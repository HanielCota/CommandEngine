package com.hanielfialho.api.result;

public enum FailureReason {
    NO_PERMISSION,
    INVALID_ARGUMENT,
    MISSING_ARGUMENT,
    INVALID_SENDER,
    EXCEPTION,
    RATE_LIMITED,
    CIRCUIT_OPEN
}
