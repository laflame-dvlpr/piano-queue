package com.pianoqueue.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pianoqueue.dto.SongResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongSearchController {

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public SongSearchController(RestClient.Builder builder, ObjectMapper mapper) {
        this.restClient = builder.baseUrl("https://itunes.apple.com").build();
        this.mapper = mapper;
    }

    @GetMapping("/search")
    public List<SongResult> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("term", q.trim())
                            .queryParam("media", "music")
                            .queryParam("entity", "song")
                            .queryParam("limit", 8)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode results = mapper.readTree(body).get("results");
            if (results == null || !results.isArray()) return List.of();

            var songs = new ArrayList<SongResult>();
            for (JsonNode r : results) {
                songs.add(new SongResult(
                        r.path("trackName").asText(""),
                        r.path("artistName").asText(""),
                        r.path("artworkUrl100").asText("")));
            }
            return songs;
        } catch (Exception e) {
            return List.of();
        }
    }
}
