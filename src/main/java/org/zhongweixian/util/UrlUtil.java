package org.zhongweixian.util;

import java.util.HashMap;
import java.util.Map;

/**
 * URL 参数解析工具类
 */
public final class UrlUtil {

    private UrlUtil() {
    }

    /**
     * 解析 uri 中的 query 参数，例如 {@code /ws?token=abc&name=} 会返回
     * {@code {token=abc, name=}}
     *
     * @param uri 原始 uri
     * @return 参数 map, 不会为 null
     */
    public static Map<String, Object> parseQuery(String uri) {
        Map<String, Object> params = new HashMap<>();
        if (uri == null) {
            return params;
        }
        int questionIdx = uri.indexOf('?');
        if (questionIdx < 0 || questionIdx == uri.length() - 1) {
            return params;
        }
        String query = uri.substring(questionIdx + 1);
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                params.put(pair, "");
            } else {
                String key = pair.substring(0, eq);
                String value = eq < pair.length() - 1 ? pair.substring(eq + 1) : "";
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * 去掉 uri 中的 query 部分，例如 {@code /ws?token=abc} -> {@code /ws}
     *
     * @param uri 原始 uri
     * @return 不含 query 的 uri
     */
    public static String stripQuery(String uri) {
        if (uri == null) {
            return null;
        }
        int questionIdx = uri.indexOf('?');
        return questionIdx < 0 ? uri : uri.substring(0, questionIdx);
    }
}