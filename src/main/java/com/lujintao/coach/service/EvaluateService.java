package com.lujintao.coach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lujintao.coach.client.DeepSeekClient;
import com.lujintao.coach.model.EvaluateResult;
import com.lujintao.coach.model.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EvaluateService {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public EvaluateResult evaluate(Question question, String userAnswer) {
        String system = "你是一名 Java 后端面试官，正在考察一名应聘实习岗的候选人。\n"
                + "你只做评分与追问，不要重写候选人的回答，不要输出鼓励性废话。";

        String keyPoints = question.getKeyPoints();
        if (keyPoints == null || keyPoints.isBlank() || "[]".equals(keyPoints.trim())) {
            keyPoints = "未提供";
        }

        String user = "【题目】" + question.getTitle() + "\n"
                + "【考察点】" + keyPoints + "\n"
                + "【候选人回答】" + userAnswer + "\n\n"
                + "请：\n"
                + "1. 按 0-100 打分，80 分以上代表能过一面；\n"
                + "2. 指出最多 3 个漏掉或答错的点，每条不超过 20 字；\n"
                + "3. 基于他回答里的薄弱处提 1 个追问，不许问本题之外的新知识；\n"
                + "4. 点评不超过 60 字，直接说问题。\n\n"
                + "只输出 JSON，不要解释，不要 markdown 代码块：\n"
                + "{\"score\": 0, \"missedPoints\": [\"\"], \"followup\": \"\", \"comment\": \"\"}";

        String raw = deepSeekClient.chat(system, user);
        return parse(raw);
    }

    private EvaluateResult parse(String raw) {
        EvaluateResult result = new EvaluateResult();
        if (raw == null || raw.isBlank()) {
            result.setComment("AI 未返回内容");
            return result;
        }

        String text = raw.trim();
        JsonNode node = tryParse(text);
        if (node == null) {
            Matcher matcher = JSON_BLOCK.matcher(text);
            if (matcher.find()) {
                node = tryParse(matcher.group());
            }
        }
        if (node == null) {
            result.setComment(truncate(text, 500));
            return result;
        }

        result.setParsed(true);
        int score = node.path("score").asInt(-1);
        result.setScore(score < 0 ? null : clamp(score));
        result.setFollowup(emptyIfNull(node.path("followup").asText()));
        result.setComment(truncate(emptyIfNull(node.path("comment").asText()), 60));

        List<String> points = new ArrayList<>();
        JsonNode arr = node.path("missedPoints");
        if (arr.isArray()) {
            for (JsonNode p : arr) {
                if (points.size() >= 3) {
                    break;
                }
                String s = p.asText();
                if (s != null && !s.isBlank()) {
                    points.add(truncate(s, 20));
                }
            }
        }
        try {
            result.setMissedPoints(objectMapper.writeValueAsString(points));
        } catch (Exception e) {
            result.setMissedPoints("[]");
        }
        return result;
    }

    private JsonNode tryParse(String s) {
        try {
            return objectMapper.readTree(s);
        } catch (Exception e) {
            return null;
        }
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String emptyIfNull(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}