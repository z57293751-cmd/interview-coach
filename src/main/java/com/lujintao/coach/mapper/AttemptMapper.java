package com.lujintao.coach.mapper;

import com.lujintao.coach.model.Attempt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AttemptMapper {

    @Insert("INSERT INTO attempt (question_id, user_answer, status) VALUES (#{questionId}, #{userAnswer}, 'pending')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Attempt attempt);

    @Select("SELECT * FROM attempt WHERE id = #{id}")
    Attempt getById(@Param("id") Long id);

    @Update("""
        UPDATE attempt
        SET score = #{score},
            ai_comment = #{aiComment},
            missed_points = #{missedPoints},
            followup = #{followup},
            status = #{status}
        WHERE id = #{id}
        """)
    int updateResult(Attempt attempt);

    @Update("UPDATE attempt SET status = 'failed' WHERE id = #{id}")
    int markFailed(@Param("id") Long id);
}