package com.lujintao.coach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lujintao.coach.common.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public DeepSeekClient(ObjectMapper objectMapper,
                          @Value("${coach.deepseek.base-url}") String baseUrl,
                          @Value("${coach.deepseek.api-key}") String apiKey,
                          @Value("${coach.deepseek.model}") String model,
                          @Value("${coach.deepseek.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String chat(String system, String user) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("AI 未配置：环境变量 DEEPSEEK_API_KEY 为空");
        }
        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    ),
                    "temperature", 0.3,
                    "response_format", Map.of("type", "json_object")
            );
            String requestJson = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();
            JsonNode root = responseBody == null || responseBody.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(responseBody);

            if (root.has("error")) {
                String msg = root.path("error").path("message").asText("AI 接口返回错误");
                log.warn("DeepSeek 返回 error 字段: {}", msg);
                throw new AiServiceException("AI 调用失败：" + msg);
            }
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (AiServiceException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new AiServiceException(describeHttpError(e), e);
        } catch (ResourceAccessException e) {
            throw new AiServiceException(describeAccessError(e), e);
        } catch (RestClientException e) {
            log.warn("调用 DeepSeek 失败", e);
            throw new AiServiceException("调用 DeepSeek 失败：" + e.getMessage(), e);
        } catch (java.io.IOException e) {
            log.warn("解析 DeepSeek 响应失败", e);
            throw new AiServiceException("AI 响应解析失败：" + e.getMessage(), e);
        }
    }

    /** 把 HTTP 状态码翻译成人能看懂的一句话。 */
    private String describeHttpError(HttpStatusCodeException e) {
        int code = e.getStatusCode().value();
        String raw = e.getResponseBodyAsString();
        if (raw != null && raw.length() > 200) {
            raw = raw.substring(0, 200) + "…";
        }
        String hint;
        switch (code) {
            case 400 -> hint = "请求被拒绝，最常见原因是模型名不对（当前配置：" + model + "）";
            case 401 -> hint = "API Key 无效或已过期";
            case 402 -> hint = "账户余额不足";
            case 403 -> hint = "无权限访问该模型";
            case 404 -> hint = "接口地址不存在，检查 DEEPSEEK_BASE_URL（当前：" + baseUrl + "）";
            case 429 -> hint = "请求太频繁或被限流";
            default -> hint = code >= 500 ? "DeepSeek 服务端错误" : "请求失败";
        }
        log.warn("DeepSeek HTTP {} -> {}", code, raw);
        return "AI 调用失败（HTTP " + code + "）：" + hint + (raw == null || raw.isBlank() ? "" : "｜返回：" + raw);
    }

    /** 区分「超时」和「连不上」，这两种处理方式完全不同。 */
    private String describeAccessError(ResourceAccessException e) {
        Throwable cause = e.getCause();
        boolean timeout = cause instanceof SocketTimeoutException
                || (cause != null && cause.getCause() instanceof SocketTimeoutException)
                || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timed out"));
        if (timeout) {
            return "AI 评分超时（超过 " + timeoutSeconds + " 秒）：换 deepseek-flash 或调大 DEEPSEEK_TIMEOUT_SECONDS";
        }
        String detail = cause == null ? e.getMessage() : cause.getMessage();
        return "连不上 DeepSeek：" + detail + "（检查网络或代理）";
    }
}
