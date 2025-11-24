package com.gameengine.recording;

import java.util.ArrayList;
import java.util.List;

public final class RecordingJson {
    private RecordingJson() {}

    // 解析 key 对应的值，支持 number/string/array/object
    public static String field(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int c = json.indexOf(':', i);
        if (c < 0) return null;
        int valStart = c + 1;

        // 跳过空白
        while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
        if (valStart >= json.length()) return null;

        char ch = json.charAt(valStart);

        if (ch == '[') {  // 数组
            return "[" + extractArray(json, valStart) + "]";
        } else if (ch == '{') {  // 对象
            return "{" + extractObject(json, valStart) + "}";
        } else {  // number/string
            int comma = json.indexOf(',', valStart);
            int brace = json.indexOf('}', valStart);
            int bracket = json.indexOf(']', valStart);
            int j = minPositive(comma, brace, bracket);
            if (j < 0) j = json.length();
            return json.substring(valStart, j).trim();
        }
    }

    private static int minPositive(int... values) {
        int min = -1;
        for (int v : values) {
            if (v >= 0 && (min < 0 || v < min)) min = v;
        }
        return min;
    }

    // 提取嵌套数组内容，不包含外层 []
    public static String extractArray(String json, int startIdx) {
        if (startIdx >= json.length() || json.charAt(startIdx) != '[') return "";
        int depth = 1;
        int i = startIdx + 1;
        int begin = i;
        for (; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') depth++;
            else if (ch == ']') depth--;
            if (depth == 0) return json.substring(begin, i);
        }
        return "";
    }

    // 提取嵌套对象内容，不包含外层 {}
    public static String extractObject(String json, int startIdx) {
        if (startIdx >= json.length() || json.charAt(startIdx) != '{') return "";
        int depth = 1;
        int i = startIdx + 1;
        int begin = i;
        for (; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') depth--;
            if (depth == 0) return json.substring(begin, i);
        }
        return "";
    }

    public static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    public static double parseDouble(String s) {
        if (s == null) return 0.0;
        try { return Double.parseDouble(stripQuotes(s)); } catch (Exception e) { return 0.0; }
    }

    // 顶层拆分数组，用于 entities 数组
    public static String[] splitTopLevel(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0; 
        int start = 0;
        for (int i = 0; i < arr.length(); i++) {
            char ch = arr.charAt(i);
            if (ch == '{' || ch == '[') depth++;
            else if (ch == '}' || ch == ']') depth--;
            else if (ch == ',' && depth == 0) {
                out.add(arr.substring(start, i));
                start = i + 1;
            }
        }
        if (start < arr.length()) out.add(arr.substring(start));
        return out.stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }
}
