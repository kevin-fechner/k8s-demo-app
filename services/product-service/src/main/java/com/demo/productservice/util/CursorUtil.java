package com.demo.productservice.util;

import java.util.Base64;

public class CursorUtil {

    public static String encode(Long id) {
        return Base64.getEncoder().encodeToString(
                ("{\"id\":" + id + "}").getBytes()
        );
    }

    public static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            // parse {"id":123}
            String idStr = decoded.replaceAll("[^0-9]", "");
            return Long.parseLong(idStr);
        } catch (Exception e) {
            return null;
        }
    }
}