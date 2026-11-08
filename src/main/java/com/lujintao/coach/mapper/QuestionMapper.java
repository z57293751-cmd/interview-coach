package com.lujintao.coach.mapper;

import com.lujintao.coach.model.Question;
import com.lujintao.coach.model.QuestionDetailVO;
import com.lujintao.coach.model.QuestionItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper {

    @Select("""
        <script>
        SELECT q.id, q.category, q.title, q.difficulty,
               (SELECT a.score FROM attempt a
                 WHERE a.question_id = q.id AND a.status = 'scored'
                 ORDER BY a.created_at DESC, a.id DESC LIMIT 1) AS last_score
        FROM question q
        WHERE 1 = 1
        <if test="category != null and category != ''">
          AND q.category = #{category}
        </if>
        <if test="keyword != null and keyword != ''">
          AND (q.title LIKE CONCAT('%', #{keyword}, '%')
               OR q.answer_standard LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY q.id DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<QuestionItemVO> listPage(@Param("keyword") String keyword,
                                  @Param("category") String category,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    @Select("""
        <script>
        SELECT COUNT(*) FROM question q
        WHERE 1 = 1
        <if test="category != null and category != ''">
          AND q.category = #{category}
        </if>
        <if test="keyword != null and keyword != ''">
          AND (q.title LIKE CONCAT('%', #{keyword}, '%')
               OR q.answer_standard LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        </script>
        """)
    long count(@Param("keyword") String keyword, @Param("category") String category);

    @Select("""
        SELECT q.id, q.category, q.title, q.answer_standard, q.key_points,
               q.difficulty, q.source,
               (SELECT a.score FROM attempt a
                 WHERE a.question_id = q.id AND a.status = 'scored'
                 ORDER BY a.created_at DESC, a.id DESC LIMIT 1) AS last_score,
               (SELECT a.id FROM attempt a
                 WHERE a.question_id = q.id
                 ORDER BY a.created_at DESC, a.id DESC LIMIT 1) AS last_attempt_id
        FROM question q WHERE q.id = #{id}
        """)
    QuestionDetailVO getDetail(@Param("id") Long id);

    @Select("SELECT * FROM question WHERE id = #{id}")
    Question getById(@Param("id") Long id);
}