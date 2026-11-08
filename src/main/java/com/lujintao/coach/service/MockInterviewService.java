package com.lujintao.coach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lujintao.coach.client.DeepSeekClient;
import com.lujintao.coach.common.AiServiceException;
import com.lujintao.coach.common.JsonExtract;
import com.lujintao.coach.mapper.MockMapper;
import com.lujintao.coach.model.MockSession;
import com.lujintao.coach.model.MockTurn;
import com.lujintao.coach.model.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 模拟面试：按简历出 10 道题，一题一题问，答完给分并追问。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockInterviewService {

    private static final int QUESTION_COUNT = 10;

    private static final String SYSTEM = """
            你是一名严格的 Java 后端面试官，正在面试一名应聘实习岗的候选人。
            你只输出 JSON，不写解释，不输出鼓励性的废话。
            """;

    private final MockMapper mockMapper;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    /* ---------------- 开始面试 ---------------- */

    public Map<String, Object> start(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("缺简历 id，请先上传简历");
        }
        Resume resume = mockMapper.getResume(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在，请重新上传");
        }

        List<MockTurn> plan = generatePlan(resume);

        MockSession session = new MockSession();
        session.setResumeId(resumeId);
        mockMapper.insertSession(session);
        for (MockTurn turn : plan) {
            turn.setSessionId(session.getId());
            mockMapper.insertTurn(turn);
        }

        MockTurn first = mockMapper.nextPending(session.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("total", plan.size());
        data.put("turn", turnView(first));
        log.info("模拟面试开始 session={} 题数={}", session.getId(), plan.size());
        return data;
    }

    /* ---------------- 回答一题 ---------------- */

    public Map<String, Object> answer(Long sessionId, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().length() < 5) {
            throw new IllegalArgumentException("回答太短了，哪怕写两三句也行");
        }
        MockSession session = mockMapper.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("面试场次不存在");
        }
        if ("finished".equals(session.getStatus())) {
            throw new IllegalArgumentException("这场面试已经结束了，重新开一场吧");
        }

        MockTurn turn = mockMapper.nextPending(sessionId);
        if (turn == null) {
            finish(session);
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("finished", true);
            done.put("report", reportData(session));
            return done;
        }

        EvaluateResult eval = evaluate(turn, userAnswer.trim());
        turn.setUserAnswer(userAnswer.trim());
        turn.setScore(eval.score());
        turn.setComment(eval.comment());
        turn.setMissedPoints(eval.missedPoints());
        turn.setFollowup(eval.followup());
        mockMapper.updateTurn(turn);

        MockTurn next = mockMapper.nextPending(sessionId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("result", resultView(turn));
        data.put("finished", next == null);
        if (next == null) {
            finish(session);
            data.put("report", reportData(session));
        } else {
            data.put("turn", turnView(next));
        }
        return data;
    }

    /* ---------------- 面试报告 ---------------- */

    public Map<String, Object> report(Long sessionId) {
        MockSession session = mockMapper.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("面试场次不存在");
        }
        return reportData(session);
    }

    /* ---------------- 内部 ---------------- */

    private List<MockTurn> generatePlan(Resume resume) {
        String user = """
                【候选人简历要点】
                %s

                【出题要求】
                请出 10 道题，按这个配额：
                - 项目 3 道：必须围绕简历里提到的项目，问"你具体怎么做的 / 为什么这么选 / 遇到什么坑"
                - Java 基础 3 道
                - 数据库 2 道
                - 框架 2 道

                硬约束：
                1. 只能问简历里出现过的技术，简历没写的技术一律不许问；
                2. 难度按初级实习岗：答得出来，但要想一下，不要问源码级细节；
                3. 每题一句话说清，不超过 40 字；
                4. category 只能是：项目 / Java 基础 / 数据库 / 框架；
                5. why 说明为什么问这题，不超过 20 字。

                只输出 JSON：
                {"questions":[{"question":"","category":"","why":""}]}
                """.formatted(nullSafe(resume.getProfileJson()));

        String raw = deepSeekClient.chat(SYSTEM, user);
        String json = JsonExtract.pick(raw);
        try {
            JsonNode arr = objectMapper.readTree(json).path("questions");
            if (!arr.isArray()) {
                throw new AiServiceException("AI 没有按格式出题，请重试一次");
            }
            List<MockTurn> turns = new ArrayList<>();
            int seq = 1;
            for (JsonNode node : arr) {
                String q = node.path("question").asText("").trim();
                if (q.isEmpty()) {
                    continue;
                }
                MockTurn t = new MockTurn();
                t.setSeq(seq++);
                t.setQuestion(truncate(q, 200));
                t.setCategory(truncate(node.path("category").asText("项目"), 32));
                t.setWhy(truncate(node.path("why").asText(""), 200));
                turns.add(t);
                if (turns.size() >= QUESTION_COUNT) {
                    break;
                }
            }
            if (turns.size() < 5) {
                throw new AiServiceException("AI 出题数量不够（只出了 " + turns.size() + " 道），请重试");
            }
            return turns;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("解析题目失败，请重试一次", e);
        }
    }

    private EvaluateResult evaluate(MockTurn turn, String answer) {
        String user = """
                【面试题】%s
                【出题意图】%s
                【候选人回答】%s

                给这题打分。这是【初级实习岗】的面试，标准按下面四档：
                - 主干对、能说出自己的做法和细节：75-90
                - 方向对，但漏掉关键点或说得笼统：55-75
                - 只会背名词、经不起追问：35-55
                - 明显答错、或者答非所问：0-35

                只输出 JSON：
                {"score":0,
                 "missedPoints":["最多 3 条，每条 20 字内"],
                 "followup":"一个追问，不许问这道题之外的新知识",
                 "comment":"点评，60 字内：先说哪部分答对了，再说差在哪"}
                """.formatted(turn.getQuestion(), nullSafe(turn.getWhy()), answer);

        String raw = deepSeekClient.chat(SYSTEM, user);
        String json = JsonExtract.pick(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            int score = Math.max(0, Math.min(100, node.path("score").asInt(0)));

            List<String> missed = new ArrayList<>();
            for (JsonNode p : node.path("missedPoints")) {
                String s = p.asText("").trim();
                if (!s.isEmpty() && missed.size() < 3) {
                    missed.add(truncate(s, 40));
                }
            }
            return new EvaluateResult(
                    score,
                    truncate(node.path("comment").asText(""), 400),
                    objectMapper.writeValueAsString(missed),
                    truncate(node.path("followup").asText(""), 500));
        } catch (Exception e) {
            throw new AiServiceException("AI 评分返回格式异常，请重试一次", e);
        }
    }

    private void finish(MockSession session) {
        List<MockTurn> turns = mockMapper.listTurns(session.getId());
        int sum = 0;
        int n = 0;
        for (MockTurn t : turns) {
            if (t.getScore() != null) {
                sum += t.getScore();
                n++;
            }
        }
        session.setStatus("finished");
        session.setTotalScore(n == 0 ? null : Math.round((float) sum / n));
        mockMapper.finishSession(session);
        log.info("模拟面试结束 session={} 总分={} 题数={}", session.getId(), session.getTotalScore(), n);
    }

    private Map<String, Object> reportData(MockSession session) {
        List<MockTurn> turns = mockMapper.listTurns(session.getId());
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, int[]> byCategory = new LinkedHashMap<>();

        for (MockTurn t : turns) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("seq", t.getSeq());
            m.put("category", t.getCategory());
            m.put("question", t.getQuestion());
            m.put("userAnswer", t.getUserAnswer());
            m.put("score", t.getScore());
            m.put("comment", t.getComment());
            m.put("followup", t.getFollowup());
            items.add(m);

            if (t.getScore() != null) {
                int[] agg = byCategory.computeIfAbsent(
                        t.getCategory() == null ? "其他" : t.getCategory(), k -> new int[]{0, 0});
                agg[0] += t.getScore();
                agg[1]++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        byCategory.forEach((k, v) -> stats.put(k, v[1] == 0 ? 0 : Math.round((float) v[0] / v[1])));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("status", session.getStatus());
        data.put("totalScore", session.getTotalScore());
        data.put("categoryScores", stats);
        data.put("turns", items);
        return data;
    }

    private Map<String, Object> turnView(MockTurn turn) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (turn == null) {
            return m;
        }
        m.put("seq", turn.getSeq());
        m.put("category", turn.getCategory());
        m.put("question", turn.getQuestion());
        m.put("why", turn.getWhy());
        return m;
    }

    private Map<String, Object> resultView(MockTurn turn) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", turn.getSeq());
        m.put("question", turn.getQuestion());
        m.put("score", turn.getScore());
        m.put("comment", turn.getComment());
        m.put("followup", turn.getFollowup());
        m.put("missedPoints", turn.getMissedPoints());
        return m;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record EvaluateResult(int score, String comment, String missedPoints, String followup) {
    }
}
