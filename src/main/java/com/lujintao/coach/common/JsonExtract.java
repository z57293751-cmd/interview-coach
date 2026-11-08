package com.lujintao.coach.common;

/** 从模型返回的文本里抠出 JSON 主体（去掉 ```json 围栏、前后的废话）。 */
public final class JsonExtract {

    private JsonExtract() {
    }

    public static String pick(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int lineEnd = text.indexOf('\n', fence);
            if (lineEnd > 0) {
                text = text.substring(lineEnd + 1);
            }
            int close = text.lastIndexOf("```");
            if (close > 0) {
                text = text.substring(0, close);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }
}
