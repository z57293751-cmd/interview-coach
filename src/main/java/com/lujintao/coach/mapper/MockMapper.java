package com.lujintao.coach.mapper;

import com.lujintao.coach.model.MockSession;
import com.lujintao.coach.model.MockTurn;
import com.lujintao.coach.model.Resume;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MockMapper {

    /* ---------------- resume ---------------- */

    @Insert("""
        INSERT INTO resume (filename, content_text, profile_json, summary)
        VALUES (#{filename}, #{contentText}, #{profileJson}, #{summary})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertResume(Resume resume);

    @Select("SELECT * FROM resume WHERE id = #{id}")
    Resume getResume(@Param("id") Long id);

    @Select("SELECT id, filename, summary, created_at FROM resume ORDER BY id DESC LIMIT 20")
    List<Resume> listResumes();

    /* ---------------- session ---------------- */

    @Insert("INSERT INTO mock_session (resume_id, status) VALUES (#{resumeId}, 'running')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSession(MockSession session);

    @Select("SELECT * FROM mock_session WHERE id = #{id}")
    MockSession getSession(@Param("id") Long id);

    @Update("""
        UPDATE mock_session
        SET status = #{status}, total_score = #{totalScore}, finished_at = NOW()
        WHERE id = #{id}
        """)
    int finishSession(MockSession session);

    /* ---------------- turn ---------------- */

    @Insert("""
        INSERT INTO mock_turn (session_id, seq, category, question, why)
        VALUES (#{sessionId}, #{seq}, #{category}, #{question}, #{why})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTurn(MockTurn turn);

    @Select("SELECT * FROM mock_turn WHERE session_id = #{sessionId} ORDER BY seq")
    List<MockTurn> listTurns(@Param("sessionId") Long sessionId);

    @Select("""
        SELECT * FROM mock_turn
        WHERE session_id = #{sessionId} AND status = 'pending'
        ORDER BY seq LIMIT 1
        """)
    MockTurn nextPending(@Param("sessionId") Long sessionId);

    @Update("""
        UPDATE mock_turn
        SET user_answer = #{userAnswer}, score = #{score}, comment = #{comment},
            missed_points = #{missedPoints}, followup = #{followup}, status = 'scored'
        WHERE id = #{id}
        """)
    int updateTurn(MockTurn turn);
}
