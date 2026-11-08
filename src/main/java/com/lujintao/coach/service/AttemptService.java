package com.lujintao.coach.service;

import com.lujintao.coach.common.AiServiceException;
import com.lujintao.coach.mapper.AttemptMapper;
import com.lujintao.coach.mapper.QuestionMapper;
import com.lujintao.coach.mapper.ReviewMapper;
import com.lujintao.coach.model.Attempt;
import com.lujintao.coach.model.EvaluateResult;
import com.lujintao.coach.model.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final AttemptMapper attemptMapper;
    private final QuestionMapper questionMapper;
    private final ReviewMapper reviewMapper;
    private final EvaluateService evaluateService;

    public Attempt submit(Long questionId, String userAnswer) {
        if (questionId == null) {
            throw new IllegalArgumentException("questionId 不能为空");
        }
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            throw new IllegalArgumentException("回答不能为空");
        }
        Question question = questionMapper.getById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("题目不存在");
        }

        Attempt attempt = new Attempt();
        attempt.setQuestionId(questionId);
        attempt.setUserAnswer(userAnswer.trim());
        attempt.setStatus("pending");
        attemptMapper.insert(attempt);
        return attempt;
    }

    public Attempt get(Long id) {
        Attempt attempt = attemptMapper.getById(id);
        if (attempt == null) {
            throw new IllegalArgumentException("答题记录不存在");
        }
        return attempt;
    }

    public Attempt evaluate(Long id) {
        Attempt attempt = get(id);
        if ("scored".equals(attempt.getStatus())) {
            return attempt;
        }

        Question question = questionMapper.getById(attempt.getQuestionId());
        EvaluateResult result;
        try {
            result = evaluateService.evaluate(question, attempt.getUserAnswer());
        } catch (AiServiceException e) {
            attemptMapper.markFailed(id);
            throw e;
        }

        attempt.setScore(result.getScore());
        attempt.setAiComment(result.getComment());
        attempt.setMissedPoints(result.getMissedPoints());
        attempt.setFollowup(result.getFollowup());
        attempt.setStatus(result.isParsed() && result.getScore() != null ? "scored" : "failed");
        attemptMapper.updateResult(attempt);

        if (result.getScore() != null && result.getScore() < 60) {
            reviewMapper.insertIgnore(attempt.getId(), attempt.getQuestionId());
        }
        return attempt;
    }
}