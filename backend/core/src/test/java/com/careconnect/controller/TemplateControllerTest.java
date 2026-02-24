package com.careconnect.controller;

import com.careconnect.dto.TemplateDto;
import com.careconnect.model.Template;
import com.careconnect.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController controller;

    private static final Long TEMPLATE_ID = 1L;

    private Template template(String name) {
        return Template.builder().id(TEMPLATE_ID).name(name).icon(0).build();
    }

    private TemplateDto templateDto(String name) {
        return TemplateDto.builder().name(name).icon(0).build();
    }

    // ─── getAllTemplates ───────────────────────────────────────────────────────

    @Test
    void getAllTemplates_returnsOkWithList() {
        List<Template> templates = List.of(template("T1"), template("T2"));
        when(templateService.getAllTemplates()).thenReturn(templates);

        ResponseEntity<List<Template>> response = controller.getAllTemplates();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(templates);
        verify(templateService).getAllTemplates();
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsOkWithList() {
        List<Template> templates = List.of(template("T1"));
        when(templateService.getAllTemplates()).thenReturn(templates);

        ResponseEntity<List<Template>> response = controller.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(templates);
    }

    // ─── getTemplateById ──────────────────────────────────────────────────────

    @Test
    void getTemplateById_returnsOkWithTemplate() {
        Template t = template("MyTemplate");
        when(templateService.getTemplateById(TEMPLATE_ID)).thenReturn(t);

        ResponseEntity<Template> response = controller.getTemplateById(TEMPLATE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(t);
        verify(templateService).getTemplateById(TEMPLATE_ID);
    }

    // ─── createTemplate ───────────────────────────────────────────────────────

    @Test
    void createTemplate_returnsOkWithCreatedTemplate() {
        TemplateDto dto = templateDto("New");
        Template created = template("New");
        when(templateService.createTemplate(dto)).thenReturn(created);

        ResponseEntity<Template> response = controller.createTemplate(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(created);
        verify(templateService).createTemplate(dto);
    }

    // ─── updateTemplate ───────────────────────────────────────────────────────

    @Test
    void updateTemplate_returnsOkWithUpdatedTemplate() {
        TemplateDto dto = templateDto("Updated");
        Template updated = template("Updated");
        when(templateService.updateTemplate(TEMPLATE_ID, dto)).thenReturn(updated);

        ResponseEntity<Template> response = controller.updateTemplate(TEMPLATE_ID, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(templateService).updateTemplate(TEMPLATE_ID, dto);
    }

    // ─── deleteTemplate ───────────────────────────────────────────────────────

    @Test
    void deleteTemplate_returnsNoContent() {
        when(templateService.deleteTemplate(TEMPLATE_ID)).thenReturn(true);

        ResponseEntity<Void> response = controller.deleteTemplate(TEMPLATE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(templateService).deleteTemplate(TEMPLATE_ID);
    }
}
