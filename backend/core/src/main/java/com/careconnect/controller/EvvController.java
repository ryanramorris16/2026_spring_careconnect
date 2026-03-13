package com.careconnect.controller;

import com.careconnect.dto.evv.*;
import com.careconnect.exception.AppException;
import com.careconnect.model.User;
import com.careconnect.model.evv.EvvRecord;
import com.careconnect.model.evv.EvvCorrection;
import com.careconnect.model.evv.EvvOfflineQueue;
import com.careconnect.repository.UserRepository;
import com.careconnect.service.evv.EvvService;
import com.careconnect.service.evv.EvvSubmissionService;
import com.careconnect.service.evv.EvvOfflineSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/v1/api/evv") @RequiredArgsConstructor
public class EvvController {
    private final EvvService evvService;
    private final EvvSubmissionService submitter;
    private final EvvOfflineSyncService offlineSyncService;
    private final UserRepository userRepository;

    /** Resolve the authenticated user's database ID from the Security context. */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    @PostMapping("/records")
    public ResponseEntity<EvvRecord> create(@RequestBody EvvRecordRequestDto req) {
        return ResponseEntity.ok(evvService.createRecord(req, currentUserId()));
    }

    @PostMapping("/records/{id}/review")
    public ResponseEntity<EvvRecord> review(@PathVariable Long id, @RequestBody EvvReviewRequest action) {
        Long actorId = currentUserId();
        var rec = evvService.review(id, action.isApprove(), actorId, action.getComment());
        if (action.isApprove()) submitter.queueForSubmission(rec, actorId);
        return ResponseEntity.ok(rec);
    }

    @PostMapping("/records/offline")
    public ResponseEntity<EvvRecord> createOfflineRecord(@RequestBody EvvRecordRequestDto req,
                                                         @RequestHeader("X-Device-ID") String deviceId) {
        return ResponseEntity.ok(evvService.createOfflineRecord(req, currentUserId(), deviceId));
    }

    @PostMapping("/records/correct")
    public ResponseEntity<EvvRecord> correctRecord(@RequestBody EvvCorrectionRequestDto req) {
        return ResponseEntity.ok(evvService.correctRecord(req, currentUserId()));
    }

    @PostMapping("/records/eor-approve")
    public ResponseEntity<EvvRecord> approveEor(@RequestBody EorApprovalRequestDto req) {
        return ResponseEntity.ok(evvService.approveEor(req, currentUserId()));
    }

    @GetMapping("/records/search")
    public ResponseEntity<Page<EvvRecord>> searchRecords(EvvSearchRequestDto searchRequest) {
        return ResponseEntity.ok(evvService.searchRecords(searchRequest));
    }

    @GetMapping("/records/pending-eor-approvals")
    public ResponseEntity<List<EvvRecord>> getPendingEorApprovals() {
        return ResponseEntity.ok(evvService.getPendingEorApprovals());
    }

    @GetMapping("/corrections/pending")
    public ResponseEntity<List<EvvCorrection>> getPendingCorrections() {
        return ResponseEntity.ok(evvService.getPendingCorrections());
    }

    @PostMapping("/corrections/{id}/approve")
    public ResponseEntity<EvvCorrection> approveCorrection(@PathVariable Long id,
                                                           @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(evvService.approveCorrection(id, currentUserId(), comment));
    }

    @GetMapping("/offline/queue")
    public ResponseEntity<List<EvvOfflineQueue>> getOfflineQueue() {
        return ResponseEntity.ok(evvService.getOfflineQueue(currentUserId()));
    }

    @PostMapping("/offline/sync")
    public ResponseEntity<String> syncOfflineData() {
        offlineSyncService.syncCaregiverOfflineData(currentUserId());
        return ResponseEntity.ok("Offline data sync initiated");
    }

    @GetMapping("/offline/status")
    public ResponseEntity<List<EvvOfflineQueue>> getOfflineStatus() {
        return ResponseEntity.ok(offlineSyncService.getOfflineQueueStatus(currentUserId()));
    }
}