package com.pianoqueue.controller;

import com.pianoqueue.dto.QueueItemDto;
import com.pianoqueue.dto.SongRequestSubmission;
import com.pianoqueue.entity.SongRequest;
import com.pianoqueue.repository.SongRequestRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class SongRequestController {

    private final SongRequestRepository repo;
    private final String adminToken;

    public SongRequestController(SongRequestRepository repo,
                                 @Value("${admin.token}") String adminToken) {
        this.repo = repo;
        this.adminToken = adminToken;
    }

    @GetMapping
    public List<QueueItemDto> getQueue(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        boolean isAdmin = adminToken.equals(token);
        var queued = repo.findByStatusOrderByCreatedAtAsc(SongRequest.Status.QUEUED);
        var items = new ArrayList<QueueItemDto>();
        for (int i = 0; i < queued.size(); i++) {
            var r = queued.get(i);
            items.add(new QueueItemDto(
                    r.getId(), i + 1, r.getTrackName(), r.getArtistName(),
                    r.getArtworkUrl(),
                    (isAdmin || r.isNamePublic()) ? r.getRequesterName() : null,
                    r.isNamePublic(),
                    r.getCreatedAt()));
        }
        return items;
    }

    @GetMapping("/history")
    public List<QueueItemDto> getHistory() {
        var played = repo.findByStatusOrderByCreatedAtDesc(SongRequest.Status.PLAYED);
        var items = new ArrayList<QueueItemDto>();
        for (var r : played) {
            items.add(new QueueItemDto(
                    r.getId(), 0, r.getTrackName(), r.getArtistName(),
                    r.getArtworkUrl(),
                    r.isNamePublic() ? r.getRequesterName() : null,
                    r.isNamePublic(),
                    r.getCreatedAt()));
        }
        return items;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid token"));
        }
        long queued = repo.countByStatus(SongRequest.Status.QUEUED);
        long played = repo.countByStatus(SongRequest.Status.PLAYED);
        return ResponseEntity.ok(Map.of("queued", queued, "played", played, "total", queued + played));
    }

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody SongRequestSubmission sub) {
        if (repo.existsByRequesterNameIgnoreCaseAndTrackNameIgnoreCaseAndArtistNameIgnoreCaseAndStatus(
                sub.requesterName(), sub.trackName(),
                sub.artistName() != null ? sub.artistName() : "",
                SongRequest.Status.QUEUED)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "You already requested this song"));
        }

        var req = new SongRequest();
        req.setRequesterName(sub.requesterName().trim());
        req.setTrackName(sub.trackName().trim());
        req.setArtistName(sub.artistName() != null ? sub.artistName().trim() : null);
        req.setArtworkUrl(sub.artworkUrl());
        req.setNamePublic(sub.namePublic());
        repo.save(req);

        var queued = repo.findByStatusOrderByCreatedAtAsc(SongRequest.Status.QUEUED);
        int position = 0;
        for (int i = 0; i < queued.size(); i++) {
            if (queued.get(i).getId().equals(req.getId())) {
                position = i + 1;
                break;
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("position", position, "id", req.getId()));
    }

    @PostMapping("/{id}/played")
    public ResponseEntity<?> markPlayed(@PathVariable Long id,
                                        @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid token"));
        }
        return repo.findById(id).map(r -> {
            r.setStatus(SongRequest.Status.PLAYED);
            repo.save(r);
            return ResponseEntity.ok(Map.of("status", "played"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid token"));
        }
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
