package com.dxh.learninghub.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtil {

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getRemoteAddr();
        return "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
