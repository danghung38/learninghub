package com.dxh.learninghub.dto;

import java.time.Instant;

public record S3ObjectMetadata(String key, Instant lastModified, long size) {
}