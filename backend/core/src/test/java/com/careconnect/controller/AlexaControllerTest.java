package com.careconnect.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.careconnect.dto.v2.TaskDtoV2;
import com.careconnect.model.Patient;
import com.careconnect.model.User;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import com.careconnect.security.JwtTokenProvider;
import com.careconnect.security.Role;
import com.careconnect.service.v2.TaskServiceV2;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link AlexaController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation
 * with mocked dependencies.</p>
 */
@WebMvcTest(AlexaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlexaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PatientRepository patientRepository;

    @MockBean
    private TaskServiceV2 taskService;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
    private static final Long PATIENT_ID = 42L;

    private User patientUser;
    private Patient patient;
    private TaskDtoV2 sampleTask;

    @BeforeEach
    void setup() {
        patientUser = new User();
        patientUser.setId(10L);
        patientUser.setEmail("patient@test.com");
        patientUser.setRole(Role.PATIENT);

        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setUser(patientUser);

        sampleTask = TaskDtoV2.builder()
                .id(1L)
                .name("Take Medication")
                .description("Take blood pressure pill")
                .date(LocalDate.now() + "T00:00:00")
                .isCompleted(false)
                .taskType("Medication")
                .build();

        // Default: token is valid
        Mockito.when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        Mockito.when(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).thenReturn("patient@test.com");
        Mockito.when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));
        Mockito.when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patient));
    }

    // =========================================================================
    // GET /v1/api/alexa/calendarTasks/get
    // =========================================================================

    @Test
    @DisplayName("GET /calendarTasks/get returns all tasks when filter=all")
    void testGetCalendarTasks_allFilter() throws Exception {
        Mockito.when(taskService.getTasksByPatient(PATIENT_ID)).thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get")
                        .header("Authorization", BEARER_TOKEN)
                        .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Take Medication")))
                .andExpect(jsonPath("$[0].id", is(1)));

        Mockito.verify(taskService).getTasksByPatient(PATIENT_ID);
    }

    @Test
    @DisplayName("GET /calendarTasks/get returns only this-week tasks when filter=week")
    void testGetCalendarTasks_weekFilter_includesTaskForToday() throws Exception {
        TaskDtoV2 todayTask = TaskDtoV2.builder()
                .id(2L)
                .name("Today Task")
                .date(LocalDate.now() + "T00:00:00")
                .isCompleted(false)
                .build();

        TaskDtoV2 oldTask = TaskDtoV2.builder()
                .id(3L)
                .name("Old Task")
                .date(LocalDate.now().minusDays(10) + "T00:00:00")
                .isCompleted(false)
                .build();

        Mockito.when(taskService.getTasksByPatient(PATIENT_ID)).thenReturn(List.of(todayTask, oldTask));

        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get")
                        .header("Authorization", BEARER_TOKEN)
                        .param("filter", "week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("Today Task")));
    }

    @Test
    @DisplayName("GET /calendarTasks/get returns 401 when no Authorization header")
    void testGetCalendarTasks_missingToken() throws Exception {
        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Missing or invalid access token")));
    }

    @Test
    @DisplayName("GET /calendarTasks/get returns 401 when token is invalid")
    void testGetCalendarTasks_invalidToken() throws Exception {
        Mockito.when(jwtTokenProvider.validateToken("bad.token")).thenReturn(false);

        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Missing or invalid access token")));
    }

    @Test
    @DisplayName("GET /calendarTasks/get returns 400 when patient cannot be resolved")
    void testGetCalendarTasks_patientNotFound() throws Exception {
        Mockito.when(patientRepository.findByUser(patientUser)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Unable to resolve patient ID")));
    }

    @Test
    @DisplayName("GET /calendarTasks/get resolves patient via caregiver role")
    void testGetCalendarTasks_caregiverRole() throws Exception {
        User caregiverUser = new User();
        caregiverUser.setId(20L);
        caregiverUser.setEmail("caregiver@test.com");
        caregiverUser.setRole(Role.CAREGIVER);

        Mockito.when(jwtTokenProvider.getEmailFromToken(VALID_TOKEN)).thenReturn("caregiver@test.com");
        Mockito.when(userRepository.findByEmail("caregiver@test.com")).thenReturn(Optional.of(caregiverUser));
        Mockito.when(patientRepository.findAll()).thenReturn(List.of(patient));
        Mockito.when(patientRepository.hasAccessByCaregiverId(PATIENT_ID, 20L)).thenReturn(true);
        Mockito.when(taskService.getTasksByPatient(PATIENT_ID)).thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get")
                        .header("Authorization", BEARER_TOKEN)
                        .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Take Medication")));

        Mockito.verify(taskService).getTasksByPatient(PATIENT_ID);
    }

    @Test
    @DisplayName("GET /calendarTasks/get uses default week filter when no filter param provided")
    void testGetCalendarTasks_defaultFilterIsWeek() throws Exception {
        TaskDtoV2 todayTask = TaskDtoV2.builder()
                .id(4L)
                .name("Now Task")
                .date(LocalDate.now() + "T00:00:00")
                .isCompleted(false)
                .build();

        Mockito.when(taskService.getTasksByPatient(PATIENT_ID)).thenReturn(List.of(todayTask));

        mockMvc.perform(get("/v1/api/alexa/calendarTasks/get")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Now Task")));
    }

    // =========================================================================
    // POST /v1/api/alexa/calendarTasks/add
    // =========================================================================

    @Test
    @DisplayName("POST /calendarTasks/add creates a task with Authorization header")
    void testAddCalendarTask_success() throws Exception {
        Mockito.when(taskService.createTask(eq(PATIENT_ID), any(TaskDtoV2.class))).thenReturn(sampleTask);

        Map<String, Object> body = Map.of(
                "name", "Take Medication",
                "date", LocalDate.now().toString(),
                "taskType", "Medication"
        );

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Take Medication")));

        Mockito.verify(taskService).createTask(eq(PATIENT_ID), any(TaskDtoV2.class));
    }

    @Test
    @DisplayName("POST /calendarTasks/add creates a task using accessToken in body as fallback")
    void testAddCalendarTask_tokenFromBody() throws Exception {
        Mockito.when(taskService.createTask(eq(PATIENT_ID), any(TaskDtoV2.class))).thenReturn(sampleTask);

        Map<String, Object> body = Map.of(
                "name", "Walk Outside",
                "date", LocalDate.now().toString(),
                "accessToken", VALID_TOKEN
        );

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Take Medication")));
    }

    @Test
    @DisplayName("POST /calendarTasks/add returns 401 when token is missing or invalid")
    void testAddCalendarTask_invalidToken() throws Exception {
        Map<String, Object> body = Map.of("name", "Walk", "date", LocalDate.now().toString());

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Missing or invalid access token")));
    }

    @Test
    @DisplayName("POST /calendarTasks/add returns 400 when patient cannot be resolved")
    void testAddCalendarTask_patientNotFound() throws Exception {
        Mockito.when(patientRepository.findByUser(patientUser)).thenReturn(Optional.empty());

        Map<String, Object> body = Map.of("name", "Walk", "date", LocalDate.now().toString());

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Unable to resolve patient ID")));
    }

    @Test
    @DisplayName("POST /calendarTasks/add returns 400 when task name is missing")
    void testAddCalendarTask_missingName() throws Exception {
        Map<String, Object> body = Map.of("date", LocalDate.now().toString());

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Task name is required")));
    }

    @Test
    @DisplayName("POST /calendarTasks/add returns 403 when service rejects with Unauthorized")
    void testAddCalendarTask_serviceThrowsUnauthorized() throws Exception {
        Mockito.when(taskService.createTask(eq(PATIENT_ID), any(TaskDtoV2.class)))
                .thenThrow(new RuntimeException("Unauthorized access to patient data"));

        Map<String, Object> body = Map.of(
                "name", "Take Medication",
                "date", LocalDate.now().toString()
        );

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Access denied to patient data")));
    }

    @Test
    @DisplayName("POST /calendarTasks/add returns 500 when service throws unexpected error")
    void testAddCalendarTask_serviceThrowsGenericError() throws Exception {
        Mockito.when(taskService.createTask(eq(PATIENT_ID), any(TaskDtoV2.class)))
                .thenThrow(new RuntimeException("Database connection lost"));

        Map<String, Object> body = Map.of(
                "name", "Take Medication",
                "date", LocalDate.now().toString()
        );

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Error adding task")));
    }

    @Test
    @DisplayName("POST /calendarTasks/add normalizes an invalid date to today")
    void testAddCalendarTask_invalidDateDefaultsToToday() throws Exception {
        TaskDtoV2 created = TaskDtoV2.builder()
                .id(5L)
                .name("Walk")
                .date(LocalDate.now() + "T00:00:00")
                .isCompleted(false)
                .build();

        Mockito.when(taskService.createTask(eq(PATIENT_ID), any(TaskDtoV2.class))).thenReturn(created);

        Map<String, Object> body = Map.of(
                "name", "Walk",
                "date", "not-a-date"
        );

        mockMvc.perform(post("/v1/api/alexa/calendarTasks/add")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Walk")));
    }
}
