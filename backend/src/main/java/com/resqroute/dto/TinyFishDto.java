package com.resqroute.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTOs for TinyFish API search, fetch, and agent endpoints.
 */
public class TinyFishDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResultItem {
        private String title;
        private String url;
        private String snippet;
        private String publishedDate;

        public SearchResultItem() {}

        public SearchResultItem(String title, String url, String snippet, String publishedDate) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
            this.publishedDate = publishedDate;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }

        public String getPublishedDate() { return publishedDate; }
        public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResponse {
        private String query;
        private List<SearchResultItem> results;

        public SearchResponse() {}

        public SearchResponse(String query, List<SearchResultItem> results) {
            this.query = query;
            this.results = results;
        }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public List<SearchResultItem> getResults() { return results; }
        public void setResults(List<SearchResultItem> results) { this.results = results; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FetchResponse {
        private String url;
        private String title;
        private String readableContent;
        private int statusCode;

        public FetchResponse() {}

        public FetchResponse(String url, String title, String readableContent, int statusCode) {
            this.url = url;
            this.title = title;
            this.readableContent = readableContent;
            this.statusCode = statusCode;
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getReadableContent() { return readableContent; }
        public void setReadableContent(String readableContent) { this.readableContent = readableContent; }

        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AgentExtractionResponse {
        private String sourceUrl;
        private List<RawDisasterReportDto> extractedReports;
        private String extractionStatus;

        public AgentExtractionResponse() {}

        public AgentExtractionResponse(String sourceUrl, List<RawDisasterReportDto> extractedReports, String extractionStatus) {
            this.sourceUrl = sourceUrl;
            this.extractedReports = extractedReports;
            this.extractionStatus = extractionStatus;
        }

        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

        public List<RawDisasterReportDto> getExtractedReports() { return extractedReports; }
        public void setExtractedReports(List<RawDisasterReportDto> extractedReports) { this.extractedReports = extractedReports; }

        public String getExtractionStatus() { return extractionStatus; }
        public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }
    }
}
