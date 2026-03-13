package com.careconnect.controller;

import com.careconnect.security.Permission;
import com.careconnect.security.RequirePermission;

import com.careconnect.dto.TaskDto;
import com.careconnect.model.Task;
import com.careconnect.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.careconnect.model.User;
import com.careconnect.security.AuthorizationService;
import com.careconnect.security.UnauthorizedException;
import com.careconnect.util.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/v1/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() throws UnauthorizedException {
        // RBAC: Only admins and caregivers can view all tasks
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireAdminOrCaregiver(currentUser);
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) throws UnauthorizedException {
        // RBAC: Only admins and caregivers can view individual tasks
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireAdminOrCaregiver(currentUser);
        Task task = taskService.getTaskById(id);
        if (task != null) {
            return ResponseEntity.ok(task);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)


    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Task>> getTasksByPatient(@PathVariable Long patientId) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requirePatientAccess(currentUser, patientId);
        List<Task> tasks = taskService.getTasksByPatient(patientId);
        return ResponseEntity.ok(tasks);
    }

    @RequirePermission(Permission.CREATE_TASKS)


    @PostMapping("/patient/{patientId}")
    public ResponseEntity<Task> createTask(@PathVariable Long patientId, @RequestBody TaskDto task) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requirePatientAccess(currentUser, patientId);
        Task created = taskService.createTask(patientId, task);
        return ResponseEntity.ok(created);
    }

    @RequirePermission(Permission.UPDATE_TASKS)


    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody TaskDto task) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireAdminOrCaregiver(currentUser);
        Task updated = taskService.updateTask(id, task);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequirePermission(Permission.DELETE_PATIENTS)


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireAdminOrCaregiver(currentUser);
        if (taskService.deleteTask(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}