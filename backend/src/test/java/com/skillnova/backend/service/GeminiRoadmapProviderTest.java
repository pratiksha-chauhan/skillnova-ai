package com.skillnova.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.exception.AiGenerationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link GeminiRoadmapProvider} against a mocked HTTP server
 * (no real network/API key needed) - request shape, successful-response
 * parsing, markdown-fence stripping, and provider-failure mapping to
 * {@link AiGenerationException}. Real end-to-end verification against the
 * live Gemini API is done manually with a real GEMINI_API_KEY per the
 * Phase 8 verification checklist.
 */
class GeminiRoadmapProviderTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String MODEL = "gemini-2.0-flash";

    private RoadmapPromptContext sampleContext() {
        return new RoadmapPromptContext(
                "Backend Developer",
                new BigDecimal("2.5"),
                List.of("Java", "Spring Boot"),
                "Staff Engineer",
                new BigDecimal("2.0"),
                12
        );
    }

    private GeminiRoadmapProvider buildProvider(RestClient.Builder builder) {
        return new GeminiRoadmapProvider(builder, new ObjectMapper(), BASE_URL, "test-api-key", MODEL, 5_000L);
    }

    private String wrapAsGeminiResponse(String innerJson) {
        String escaped = innerJson.replace("\"", "\\\"").replace("\n", " ");
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [ { "text": "%s" } ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """.formatted(escaped);
    }

    @Test
    void generatesRoadmapFromSuccessfulGeminiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String innerJson = """
                {"summary":"Great plan","skillGaps":[{"skillName":"Kubernetes","currentLevel":"NONE","requiredLevel":"INTERMEDIATE","priority":"HIGH"}],
                "tasks":[{"monthNumber":1,"weekNumber":1,"title":"Learn Docker","description":"desc","taskType":"LEARNING","sortOrder":1}],
                "certifications":[{"title":"CKA","provider":"CNCF","url":"https://example.com","priority":"MEDIUM"}],
                "projects":[{"title":"Deploy a microservice","description":"desc","difficulty":"INTERMEDIATE","techStack":"Docker, K8s"}]}
                """;

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andExpect(MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("x-goog-api-key", "test-api-key"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        wrapAsGeminiResponse(innerJson), MediaType.APPLICATION_JSON));

        GeminiRoadmapProvider provider = buildProvider(builder);
        RoadmapAiResponse response = provider.generateRoadmap(sampleContext());

        assertThat(response.summary()).isEqualTo("Great plan");
        assertThat(response.skillGaps()).hasSize(1);
        assertThat(response.skillGaps().get(0).skillName()).isEqualTo("Kubernetes");
        assertThat(response.tasks()).hasSize(1);
        assertThat(response.certifications()).hasSize(1);
        assertThat(response.projects()).hasSize(1);

        server.verify();
    }

    @Test
    void stripsMarkdownFencesBeforeParsing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String innerJsonWithFences = "```json\\n{\\\"summary\\\":\\\"fenced\\\",\\\"skillGaps\\\":[],\\\"tasks\\\":[],\\\"certifications\\\":[],\\\"projects\\\":[]}\\n```";
        String wrapped = """
                {"candidates":[{"content":{"parts":[{"text":"%s"}],"role":"model"},"finishReason":"STOP"}]}
                """.formatted(innerJsonWithFences);

        server.expect(MockRestRequestMatchers.anything())
                .andRespond(MockRestResponseCreators.withSuccess(wrapped, MediaType.APPLICATION_JSON));

        GeminiRoadmapProvider provider = buildProvider(builder);
        RoadmapAiResponse response = provider.generateRoadmap(sampleContext());

        assertThat(response.summary()).isEqualTo("fenced");
    }

    @Test
    void throwsAiGenerationExceptionWhenGeminiReturnsNoCandidates() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(MockRestRequestMatchers.anything())
                .andRespond(MockRestResponseCreators.withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        GeminiRoadmapProvider provider = buildProvider(builder);

        assertThatThrownBy(() -> provider.generateRoadmap(sampleContext()))
                .isInstanceOf(AiGenerationException.class)
                .hasMessageContaining("no candidates");
    }

    @Test
    void throwsAiGenerationExceptionWhenResponseBodyIsNotValidJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String wrapped = """
                {"candidates":[{"content":{"parts":[{"text":"this is not json"}],"role":"model"},"finishReason":"STOP"}]}
                """;
        server.expect(MockRestRequestMatchers.anything())
                .andRespond(MockRestResponseCreators.withSuccess(wrapped, MediaType.APPLICATION_JSON));

        GeminiRoadmapProvider provider = buildProvider(builder);

        assertThatThrownBy(() -> provider.generateRoadmap(sampleContext()))
                .isInstanceOf(AiGenerationException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void throwsAiGenerationExceptionWhenGeminiReturnsErrorStatus() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(MockRestRequestMatchers.anything())
                .andRespond(MockRestResponseCreators.withServerError());

        GeminiRoadmapProvider provider = buildProvider(builder);

        assertThatThrownBy(() -> provider.generateRoadmap(sampleContext()))
                .isInstanceOf(AiGenerationException.class);
    }

    @Test
    void constructorRejectsABlankApiKey() {
        RestClient.Builder builder = RestClient.builder();
        assertThatThrownBy(() -> new GeminiRoadmapProvider(
                builder, new ObjectMapper(), BASE_URL, "", MODEL, 5_000L))
                .isInstanceOf(IllegalStateException.class);
    }
}
