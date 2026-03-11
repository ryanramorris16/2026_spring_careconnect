package com.careconnect.controller;

import com.careconnect.security.Permission;
import com.careconnect.security.RequirePermission;

import com.careconnect.model.Template;
import com.careconnect.dto.TemplateDto;
import com.careconnect.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/templates")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)


    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)


    @GetMapping("/all")
    public ResponseEntity<List<Template>> getAll() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)


    @GetMapping("/{id}")
    public ResponseEntity<Template> getTemplateById(@PathVariable Long id) {
        Template template = templateService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @RequirePermission(Permission.CREATE_TASKS)


    @PostMapping
    public ResponseEntity<Template> createTemplate(@RequestBody TemplateDto templateDto) {
        Template created = templateService.createTemplate(templateDto);
        return ResponseEntity.ok(created);
    }

    @RequirePermission(Permission.UPDATE_TASKS)


    @PutMapping("/{id}")
    public ResponseEntity<Template> updateTemplate(@PathVariable Long id, @RequestBody TemplateDto templateDto) {
        Template updated = templateService.updateTemplate(id, templateDto);
        return ResponseEntity.ok(updated);
    }

    @RequirePermission(Permission.DELETE_PATIENTS)


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}