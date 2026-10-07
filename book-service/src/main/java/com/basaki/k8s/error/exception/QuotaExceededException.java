package com.basaki.k8s.error.exception;

/**
 * {@code QuotaExceededException} is thrown when a client has used up its
 * allowance of a business operation for the current period.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
