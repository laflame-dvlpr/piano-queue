package com.pianoqueue.dto;

import java.time.Instant;

public record QueueItemDto(
        Long id,
        int position,
        String trackName,
        String artistName,
        String artworkUrl,
        String requesterName,
        boolean namePublic,
        Instant createdAt
) {}
