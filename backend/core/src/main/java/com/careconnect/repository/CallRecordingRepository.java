package com.careconnect.repository;

import com.careconnect.model.CallRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallRecordingRepository
        extends JpaRepository<CallRecording, Long> {

    /**
     * Returns the most recent recording for a call.
     *
     * @param callId call identifier
     * @return most recent recording, when present
     */
    Optional<CallRecording> findTopByCallIdOrderByStartedAtDesc(String callId);

    /**
     * Returns all recordings for a call, newest first.
     *
     * @param callId call identifier
     * @return matching recordings
     *     in descending start order
     */
    List<CallRecording> findByCallIdOrderByStartedAtDesc(String callId);

    /**
     * Returns recordings initiated by a user, newest first.
     *
     * @param userId initiating user identifier
     * @return matching recordings
     *     in descending start order
     */
    List<CallRecording> findByInitiatedByUserIdOrderByStartedAtDesc(
            Long userId
    );

    /**
     * Returns recordings with a status, newest first.
     *
     * @param status recording status
     * @return matching recordings
     *     in descending start order
     */
    List<CallRecording> findByStatusOrderByStartedAtDesc(String status);

    /**
     * Returns up to 100 recordings with a status.
     *
     * @param status recording status
     * @return up to 100 matching recordings in
     *     descending start order
     */
    List<CallRecording> findTop100ByStatusOrderByStartedAtDesc(String status);

    /**
     * Deletes recordings for a call.
     *
     * @param callId call identifier
     * @return number of deleted rows
     */
    long deleteByCallId(String callId);
}
