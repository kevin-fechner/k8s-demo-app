package com.demo.productservice.util;

import java.util.Base64;

public class CursorUtil {

    private CursorUtil() {}

    public static String encode(Long id) {
        return Base64.getEncoder().encodeToString(
                ("{\"id\":" + id + "}").getBytes()
        );
    }

    public static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            String idStr = decoded.replaceAll("\\D", "");
            return Long.parseLong(idStr);
        } catch (Exception e) {
            return null;
        }
    }
}