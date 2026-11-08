package com.lujintao.coach.service;

import com.lujintao.coach.mapper.ReviewMapper;
import com.lujintao.coach.model.ReviewItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public Map<String, Object> list(Integer mastered) {
        List<ReviewItemVO> items = reviewMapper.list(mastered);
        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        data.put("pending", reviewMapper.countByMastered(0));
        data.put("mastered", reviewMapper.countByMastered(1));
        return data;
    }

    public void update(Long id, Integer mastered, String note) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        reviewMapper.update(id, mastered, note);
    }
}