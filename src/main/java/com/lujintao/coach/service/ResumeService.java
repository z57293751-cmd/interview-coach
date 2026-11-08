package com.lujintao.coach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lujintao.coach.client.DeepSeekClient;
import com.lujintao.coach.common.AiServiceException;
import com.lujintao.coach.common.JsonExtract;
import com.lujintao.coach.mapper.MockMapper;
import com.lujintao.coach.model.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    /** 简历正文送去模型的上限，防止超长简历把 token 打爆 */
    private static final int MAX_CHARS = 10000;

    private static final String SYSTEM = """
            你是一名资深 Java 后端面试官，擅长快速读懂简历并提炼面试要点。
            你只输出 JSON，不写解释。
            """;

    private final MockMapper mockMapper;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    /** 最近上传的简历列表 */
    public java.util.List<Resume> list() {
        return mockMapper.listResumes();
    }

    public Resume upload(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String text;
        try {
            text = readText(file, filename);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败：" + e.getMessage());
        }
        if (text == null || text.trim().length() < 30) {
            throw new IllegalArgumentException("没解析出简历内容。如果是扫描版 PDF（图片），请换成文字版 PDF 或直接复制成 txt 上传");
        }
        if (text.length() > MAX_CHARS) {
            text = text.substring(0, MAX_CHARS);
        }

        String profileJson = extractProfile(text);

        Resume resume = new Resume();
        resume.setFilename(filename);
        resume.setContentText(text);
        resume.setProfileJson(profileJson);
        resume.setSummary(readSummary(profileJson));
        mockMapper.insertResume(resume);
        log.info("简历已入库 id={} file={} chars={}", resume.getId(), filename, text.length());
        return resume;
    }

    /** PDF 用 PDFBox 抽文本；txt/md 直接按 UTF-8 读。 */
    private String readText(MultipartFile file, String filename) throws IOException {
        String lower = filename.toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(doc);
            }
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("只支持 PDF 或 TXT 文件，你上传的是：" + filename);
    }

    private String extractProfile(String resumeText) {
        String user = """
                【简历正文】
                %s

                请提炼面试要点，只输出 JSON：
                {"summary":"一句话概括这个人，30 字内",
                 "techStack":["技能1","技能2"],
                 "years":"应届/1年/2年",
                 "projects":[{"name":"项目名","role":"他负责什么","highlights":["可被追问的亮点"]}],
                 "weakSpots":["明显短板"]}

                要求：
                1. techStack 只列简历里真实出现的技术，不要脑补；
                2. projects 最多 3 个，role 用一句话，highlights 每条不超过 20 字；
                3. 简历里没写的字段就留空数组或空字符串，不要编。
                """.formatted(resumeText);

        String raw = deepSeekClient.chat(SYSTEM, user);
        String json = JsonExtract.pick(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new AiServiceException("AI 返回的简历画像不是合法 JSON，请重试一次", e);
        }
    }

    private String readSummary(String profileJson) {
        try {
            String s = objectMapper.readTree(profileJson).path("summary").asText("");
            return s.length() > 500 ? s.substring(0, 500) : s;
        } catch (Exception e) {
            return "";
        }
    }
}
