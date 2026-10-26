package com.pianoqueue.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "song_request")
public class SongRequest {

    public enum Status { QUEUED, PLAYED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String requesterName;

    @Column(nullable = false)
    private boolean namePublic;

    @Column(nullable = false, length = 200)
    private String trackName;

    @Column(length = 200)
    private String artistName;

    @Column(length = 500)
    private String artworkUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.QUEUED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public boolean isNamePublic() { return namePublic; }
    public void setNamePublic(boolean namePublic) { this.namePublic = namePublic; }

    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getArtworkUrl() { return artworkUrl; }
    public void setArtworkUrl(String artworkUrl) { this.artworkUrl = artworkUrl; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
