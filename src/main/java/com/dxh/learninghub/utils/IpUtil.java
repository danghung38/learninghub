package com.dxh.learninghub.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Lấy từ Cloudflare Proxy header
        String ip = request.getHeader("CF-Connecting-IP");

        // Fallback sang X-Forwarded-For nếu không có
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                // X-Forwarded-For có dạng "client_ip, proxy1, proxy2" -> lấy IP đầu tiên
                ip = xForwardedFor.split(",")[0].trim();
            }
        }

        // Fallback cuối cùng về remote address của Nginx gọi vào
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}