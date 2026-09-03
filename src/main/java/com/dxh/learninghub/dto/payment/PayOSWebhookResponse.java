package com.dxh.learninghub.dto.payment;

/**
 * Minimal acknowledgement returned to payOS after a webhook is processed.
 * Keeping the response small mirrors the provider's webhook contract.
 */
public record PayOSWebhookResponse(String code, String desc) {
}
