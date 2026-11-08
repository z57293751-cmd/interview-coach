package com.lujintao.coach.mapper;

import com.lujintao.coach.model.ReviewItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Select("""
        <script>
        SELECT r.id, r.attempt_id, r.question_id, r.mastered, r.note, r.updated_at,
               q.title, q.category, a.score, a.user_answer, a.ai_comment
        FROM review_item r
        JOIN question q ON q.id = r.question_id
        JOIN attempt a ON a.id = r.attempt_id
        <where>
          <if test="mastered != null">
            r.mastered = #{mastered}
          </if>
        </where>
        ORDER BY a.score ASC, r.id DESC
        </script>
        """)
    List<ReviewItemVO> list(@Param("mastered") Integer mastered);

    @Select("SELECT COUNT(*) FROM review_item WHERE mastered = #{mastered}")
    long countByMastered(@Param("mastered") int mastered);

    @Insert("INSERT IGNORE INTO review_item (attempt_id, question_id) VALUES (#{attemptId}, #{questionId})")
    int insertIgnore(@Param("attemptId") Long attemptId, @Param("questionId") Long questionId);

    @Update("UPDATE review_item SET mastered = COALESCE(#{mastered}, mastered), note = COALESCE(#{note}, note) WHERE id = #{id}")
    int update(@Param("id") Long id, @Param("mastered") Integer mastered, @Param("note") String note);
}