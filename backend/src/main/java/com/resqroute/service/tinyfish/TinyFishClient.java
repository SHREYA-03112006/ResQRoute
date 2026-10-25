package com.resqroute.service.tinyfish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resqroute.dto.TinyFishDto;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service client for TinyFish Web Intelligence API (Search, Fetch, AI Agent) (Phase 2).
 *
 * <p>Operates in dual mode:
 * <ul>
 *   <li>Live mode: uses OkHttp to communicate with TinyFish API endpoints when valid key is provided</li>
 *   <li>Mock mode: uses {@link TinyFishMockProvider} when API key is unset, empty, or mock-mode is true</li>
 * </ul>
 *
 * <p>Never hardcodes or leaks private API keys. Handles streaming, JSON, and network errors gracefully.
 */
@Service
public class TinyFishClient {

    private static final Logger log = LoggerFactory.getLogger(TinyFishClient.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TinyFishMockProvider mockProvider;

    @Value("${resqroute.tinyfish.api-key:}")
    private String apiKey;

    @Value("${resqroute.tinyfish.base-url:https://api.tinyfish.io}")
    private String baseUrl;

    @Value("${resqroute.tinyfish.mock-mode:true}")
    private boolean mockMode;

    public TinyFishClient(TinyFishMockProvider mockProvider, ObjectMapper objectMapper) {
        this.mockProvider = mockProvider;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public boolean isLiveMode() {
        return !mockMode && apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your_tinyfish_api_key");
    }

    /**
     * Search public disaster announcements, weather advisories, and emergency alerts.
     */
    public TinyFishDto.SearchResponse search(String query) {
        if (!isLiveMode()) {
            log.info("TinyFish client operating in MOCK/SAMPLE mode for search query: '{}'", query);
            return mockProvider.mockSearch(query);
        }

        log.info("Calling live TinyFish search API for query: '{}'", query);
        String searchEndpoint = baseUrl != null && !baseUrl.contains("api.tinyfish.io")
                ? baseUrl + "/v1/search"
                : "https://api.search.tinyfish.ai";

        HttpUrl parsed = HttpUrl.parse(searchEndpoint);
        if (parsed == null) {
            return mockProvider.mockSearch(query);
        }

        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("query", query)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("TinyFish search API returned HTTP {}: {}, falling back to sample mode", response.code(), response.message());
                return mockProvider.mockSearch(query);
            }
            ResponseBody body = response.body();
            if (body != null) {
                return objectMapper.readValue(body.string(), TinyFishDto.SearchResponse.class);
            }
        } catch (Exception e) {
            log.warn("TinyFish search API call failed ({}), falling back to sample mode", e.getMessage());
        }
        return mockProvider.mockSearch(query);
    }

    /**
     * Fetch readable text content of a news or advisory web page.
     */
    public TinyFishDto.FetchResponse fetch(String targetUrl) {
        if (!isLiveMode()) {
            log.info("TinyFish client operating in MOCK/SAMPLE mode for fetch URL: '{}'", targetUrl);
            return mockProvider.mockFetch(targetUrl);
        }

        log.info("Calling live TinyFish fetch API for URL: '{}'", targetUrl);
        String fetchEndpoint = baseUrl != null && !baseUrl.contains("api.tinyfish.io")
                ? baseUrl + "/v1/fetch"
                : "https://api.fetch.tinyfish.ai";

        HttpUrl parsed = HttpUrl.parse(fetchEndpoint);
        if (parsed == null) {
            return mockProvider.mockFetch(targetUrl);
        }

        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("url", targetUrl)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("X-API-Key", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("TinyFish fetch API returned HTTP {}: {}, falling back to sample mode", response.code(), response.message());
                return mockProvider.mockFetch(targetUrl);
            }
            ResponseBody body = response.body();
            if (body != null) {
                return objectMapper.readValue(body.string(), TinyFishDto.FetchResponse.class);
            }
        } catch (Exception e) {
            log.warn("TinyFish fetch API call failed ({}), falling back to sample mode", e.getMessage());
        }
        return mockProvider.mockFetch(targetUrl);
    }

    /**
     * Use TinyFish AI Agent to extract structured disaster records from a target page.
     */
    public TinyFishDto.AgentExtractionResponse agentExtract(String targetUrl) {
        if (!isLiveMode()) {
            log.info("TinyFish client operating in MOCK/SAMPLE mode for agent extraction from: '{}'", targetUrl);
            return mockProvider.mockAgentExtraction(targetUrl);
        }

        log.info("Calling live TinyFish agent extraction API for: '{}'", targetUrl);
        String agentEndpoint = baseUrl + "/v1/agent/extract";
        RequestBody jsonBody = RequestBody.create(
                "{\"url\":\"" + targetUrl + "\",\"schema\":\"disaster_incident_v2\"}",
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(agentEndpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Key", apiKey)
                .post(jsonBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("TinyFish agent extraction returned HTTP {}: {}, falling back to sample mode", response.code(), response.message());
                return mockProvider.mockAgentExtraction(targetUrl);
            }
            ResponseBody body = response.body();
            if (body != null) {
                return objectMapper.readValue(body.string(), TinyFishDto.AgentExtractionResponse.class);
            }
        } catch (Exception e) {
            log.warn("TinyFish agent extraction call failed ({}), falling back to sample mode", e.getMessage());
        }
        return mockProvider.mockAgentExtraction(targetUrl);
    }
}
