package com.pianoqueue.repository;

import com.pianoqueue.entity.SongRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SongRequestRepository extends JpaRepository<SongRequest, Long> {

    List<SongRequest> findByStatusOrderByCreatedAtAsc(SongRequest.Status status);

    List<SongRequest> findByStatusOrderByCreatedAtDesc(SongRequest.Status status);

    long countByStatus(SongRequest.Status status);

    boolean existsByRequesterNameIgnoreCaseAndTrackNameIgnoreCaseAndArtistNameIgnoreCaseAndStatus(
            String requesterName, String trackName, String artistName, SongRequest.Status status);
}
