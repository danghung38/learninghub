package com.dxh.learninghub.service.payment;

import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

/** Strategy extension for gateways that send a signed server-to-server webhook. */
public interface WebhookPaymentGatewayStrategy extends PaymentGatewayStrategy {

    WebhookData verifyWebhook(Webhook webhook);
}
