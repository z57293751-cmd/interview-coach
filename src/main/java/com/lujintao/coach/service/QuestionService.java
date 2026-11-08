package com.lujintao.coach.service;

import com.lujintao.coach.mapper.QuestionMapper;
import com.lujintao.coach.model.QuestionDetailVO;
import com.lujintao.coach.model.QuestionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;

    public Map<String, Object> page(String keyword, String category, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }
        int offset = (page - 1) * size;
        String kw = trimToNull(keyword);
        String cat = trimToNull(category);

        List<QuestionItemVO> list = questionMapper.listPage(kw, cat, offset, size);
        long total = questionMapper.count(kw, cat);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    public QuestionDetailVO detail(Long id) {
        QuestionDetailVO vo = questionMapper.getDetail(id);
        if (vo == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        return vo;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}