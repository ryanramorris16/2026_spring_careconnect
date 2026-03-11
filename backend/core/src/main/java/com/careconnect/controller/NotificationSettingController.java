package com.careconnect.controller;

import com.careconnect.security.Permission;
import com.careconnect.security.RequirePermission;

import com.careconnect.dto.NotificationSettingDTO;
import com.careconnect.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/notification-settings")
@Tag(name = "Notification Settings", description = "Manage notification preferences for users")
public class NotificationSettingController {

    @Autowired
    private NotificationSettingService notificationSettingService;

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)


    @GetMapping("/{userId}")
    @Operation(summary = "Get notification settings for a user")
    public ResponseEntity<NotificationSettingDTO> getSettings(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationSettingService.getByUserId(userId));
    }

    @RequirePermission(Permission.CREATE_TASKS)


    @PostMapping
    @Operation(summary = "Create or update notification settings for a user")
    public ResponseEntity<NotificationSettingDTO> createOrUpdate(@RequestBody NotificationSettingDTO dto) {
        return ResponseEntity.ok(notificationSettingService.createOrUpdate(dto));
    }
}
