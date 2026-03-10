package com.careconnect.controller;

import com.careconnect.dto.schedule.ScheduledVisitRequest;
import com.careconnect.dto.schedule.ScheduledVisitResponse;
import com.careconnect.dto.schedule.ScheduledVisitSummary;
import com.careconnect.model.schedule.ScheduledVisit;
import com.careconnect.service.schedule.ScheduledVisitService;
import lombok.RequiredArgsConstructor;
import com.careconnect.dto.schedule.AuditDiffResponse;
import com.careconnect.dto.schedule.CalendarViewDto;
import com.careconnect.dto.schedule.ConflictCheckResponse;
import com.careconnect.dto.schedule.MonthViewDto;
import com.careconnect.dto.schedule.ScheduledVisitAuditResponse;
import com.careconnect.service.schedule.ScheduleConflictService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/scheduled-visits")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ScheduledVisitController {

    private final ScheduledVisitService scheduledVisitService;

    /**
     * Create a new scheduled visit
     */
    @PostMapping("/caregiver/{caregiverId}")
    public ResponseEntity<ScheduledVisitResponse> createScheduledVisit(
            @PathVariable Long caregiverId,
            @Valid @RequestBody ScheduledVisitRequest request) {
        ScheduledVisitResponse response = scheduledVisitService.createScheduledVisit(caregiverId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all scheduled visits for a caregiver
     */
    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<ScheduledVisitResponse>> getScheduledVisits(
            @PathVariable Long caregiverId) {
        List<ScheduledVisitResponse> visits = scheduledVisitService.getScheduledVisits(caregiverId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get scheduled visits for a specific date
     */
    @GetMapping("/caregiver/{caregiverId}/date/{date}")
    public ResponseEntity<List<ScheduledVisitResponse>> getScheduledVisitsByDate(
            @PathVariable Long caregiverId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ScheduledVisitResponse> visits = scheduledVisitService.getScheduledVisitsByDate(caregiverId, date);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get scheduled visits between dates
     */
    @GetMapping("/caregiver/{caregiverId}/range")
    public ResponseEntity<List<ScheduledVisitResponse>> getScheduledVisitsBetweenDates(
            @PathVariable Long caregiverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ScheduledVisitResponse> visits = scheduledVisitService
                .getScheduledVisitsBetweenDates(caregiverId, startDate, endDate);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get visit summary statistics
     */
    @GetMapping("/caregiver/{caregiverId}/summary")
    public ResponseEntity<ScheduledVisitSummary> getVisitSummary(
            @PathVariable Long caregiverId) {
        ScheduledVisitSummary summary = scheduledVisitService.getVisitSummary(caregiverId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get overdue visits
     */
    @GetMapping("/caregiver/{caregiverId}/overdue")
    public ResponseEntity<List<ScheduledVisitResponse>> getOverdueVisits(
            @PathVariable Long caregiverId) {
        List<ScheduledVisitResponse> visits = scheduledVisitService.getOverdueVisits(caregiverId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get ready visits
     */
    @GetMapping("/caregiver/{caregiverId}/ready")
    public ResponseEntity<List<ScheduledVisitResponse>> getReadyVisits(
            @PathVariable Long caregiverId) {
        List<ScheduledVisitResponse> visits = scheduledVisitService.getReadyVisits(caregiverId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get upcoming visits
     */
    @GetMapping("/caregiver/{caregiverId}/upcoming")
    public ResponseEntity<List<ScheduledVisitResponse>> getUpcomingVisits(
            @PathVariable Long caregiverId) {
        List<ScheduledVisitResponse> visits = scheduledVisitService.getUpcomingVisits(caregiverId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get a specific scheduled visit
     */
    @GetMapping("/{visitId}")
    public ResponseEntity<ScheduledVisitResponse> getScheduledVisit(
            @PathVariable Long visitId) {
        ScheduledVisitResponse visit = scheduledVisitService.getScheduledVisit(visitId);
        return ResponseEntity.ok(visit);
    }

    /**
     * Update a scheduled visit
     */
    @PutMapping("/{visitId}")
    public ResponseEntity<ScheduledVisitResponse> updateScheduledVisit(
            @PathVariable Long visitId,
            @Valid @RequestBody ScheduledVisitRequest request) {
        ScheduledVisitResponse response = scheduledVisitService.updateScheduledVisit(visitId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a scheduled visit
     */
    @PutMapping("/{visitId}/cancel")
    public ResponseEntity<Void> cancelScheduledVisit(
            @PathVariable Long visitId) {
        scheduledVisitService.cancelScheduledVisit(visitId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update visit status
     */
    @PutMapping("/{visitId}/status")
    public ResponseEntity<ScheduledVisitResponse> updateVisitStatus(
            @PathVariable Long visitId,
            @RequestParam String status) {
        ScheduledVisitResponse response = scheduledVisitService.updateVisitStatus(visitId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a scheduled visit
     */
    @DeleteMapping("/{visitId}")
    public ResponseEntity<Void> deleteScheduledVisit(
            @PathVariable Long visitId) {
        scheduledVisitService.deleteScheduledVisit(visitId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/caregiver/{caregiverId}/calendar/month")
    public ResponseEntity<MonthViewDto> getMonthCalendar(
            @PathVariable Long caregiverId,
            @RequestParam int year,
            @RequestParam int month) {
        // TODO: Implement month calendar retrieval
        return ResponseEntity.ok(new MonthViewDto());
    }

    @GetMapping("/caregiver/{caregiverId}/calendar/week")
    public ResponseEntity<Map<LocalDate, CalendarViewDto>> getWeekCalendar(
            @PathVariable Long caregiverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        // TODO: Implement week calendar retrieval
        return ResponseEntity.ok(new java.util.HashMap<>());
    }

    @GetMapping("/{visitId}/audit-history")
    public ResponseEntity<List<ScheduledVisitAuditResponse>> getAuditHistory(
            @PathVariable Long visitId) {
        // TODO: Implement audit history retrieval
        List<ScheduledVisitAuditResponse> audits = scheduledVisitService.getVisitAuditHistory(visitId);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/{visitId}/audit/{auditId}/details")
    public ResponseEntity<AuditDiffResponse> getAuditDetails(
            @PathVariable Long visitId,
            @PathVariable Long auditId) {
        // TODO: Implement audit details retrieval
        AuditDiffResponse auditDiff = scheduledVisitService.getVisitAuditDetails(visitId, auditId);
        return ResponseEntity.ok(auditDiff);
    }

    @PostMapping("/caregiver/{caregiverId}/check-conflicts")
    public ResponseEntity<ConflictCheckResponse> checkConflicts(
            @PathVariable Long caregiverId,
            @Valid @RequestBody ScheduledVisitRequest request) {
        ScheduleConflictService.ConflictSummary summary = scheduledVisitService.analyzeConflicts(
                caregiverId,
                request.getPatientId(),
                request.getScheduledDate(),
                request.getScheduledTime(),
                request.getDurationMinutes());

        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setHasConflicts(
                summary.hasConflicts() || summary.isExceedsDailyLimit() || summary.isExceedsDailyHours());
        response.setConflictMessages(new ArrayList<>());
        response.setWarnings(summary.getWarnings());
        
        List<ScheduledVisitResponse> conflictingVisits = new ArrayList<>();
        if (summary.getCaregiverConflicts() != null) {
            conflictingVisits = summary.getCaregiverConflicts().stream()
                    .map(v -> new ScheduledVisitResponse(v, getPatientName(v.getPatientId())))
                    .collect(Collectors.toList());
        }
        response.setConflictingVisits(conflictingVisits);
        response.setExceedsDailyLimit(summary.isExceedsDailyLimit());
        response.setExceedsDailyHours(summary.isExceedsDailyHours());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{visitId}/analyze-conflicts")
    public ResponseEntity<ConflictCheckResponse> analyzeConflictsBefore(
            @PathVariable Long visitId,
            @Valid @RequestBody ScheduledVisitRequest request) {
        ScheduledVisitResponse existingVisit = scheduledVisitService.getScheduledVisit(visitId);

        List<String> warnings = new ArrayList<>();
        List<ScheduledVisit> conflicts = new ArrayList<>();

        // Analyze and return summary
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setHasConflicts(!conflicts.isEmpty());
        response.setConflictMessages(warnings);

        return ResponseEntity.ok(response);
    }

    /**
     * Get patient name by ID
     */
    private String getPatientName(Long patientId) {
        return patientId != null ? patientId.toString() : "Unknown";
    }

    /**
     * Handle scheduling conflicts and validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleConflictException(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
