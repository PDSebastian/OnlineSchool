package ro.mycode.sebischool.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ro.mycode.sebischool.auth.dtos.AuthRegisterRequest;
import ro.mycode.sebischool.course.dtos.CourseRequest;
import ro.mycode.sebischool.system.constants.ErrorConstants;
import ro.mycode.sebischool.users.model.UserType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica contractul de erori prin HTTP: 404/409/400 cu body {message, status}.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithAllPermissions
@Transactional
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthRegisterRequest register(UserType type, String email, Integer age) {
        return new AuthRegisterRequest(type, "Ana", "Pop", email, "parola123", age, null, null, null);
    }

    @Test
    @DisplayName("GET curs inexistent -> 404 cu body")
    void courseNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/courses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.COURSE_NOT_FOUND_ERROR));
    }

    @Test
    @DisplayName("GET student inexistent -> 404 cu body")
    void studentNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/students/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.STUDENT_NOT_FOUND_ERROR));
    }

    @Test
    @DisplayName("GET carte inexistenta -> 404 cu body")
    void bookNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.BOOK_NOT_FOUND_ERROR));
    }

    @Test
    @DisplayName("DELETE profesor inexistent -> 404 cu body")
    void professorNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/profesori/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.PROFESSOR_NOT_FOUND_ERROR));
    }

    @Test
    @DisplayName("DELETE inscriere inexistenta -> 404 cu body")
    void enrolmentNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/enrolments/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.ENROLMENT_NOT_FOUND_ERROR));
    }

    @Test
    @DisplayName("DELETE curs inexistent -> 404 cu body")
    void deleteCourseNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/courses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.COURSE_NOT_FOUND_ERROR));
    }

    @Test
    @DisplayName("POST curs duplicat -> 409 cu body")
    void courseAlreadyExists() throws Exception {
        String body = objectMapper.writeValueAsString(
                CourseRequest.builder().name("Duplicat").departament("IT").build());

        mockMvc.perform(post("/api/v2/courses/add").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v2/courses/add").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(ErrorConstants.COURSE_ALREADY_EXISTS_ERROR));
    }

    @Test
    @DisplayName("Register cu email existent -> 409 cu body")
    void userAlreadyExists() throws Exception {
        String body = objectMapper.writeValueAsString(register(UserType.STUDENT, "dup@test.ro", 20));

        mockMvc.perform(post("/api/v2/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v2/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(ErrorConstants.USER_ALREADY_EXISTS_ERROR));
    }

    @Test
    @DisplayName("Register STUDENT fara varsta -> 400 cu mesaj din ErrorConstants")
    void authValidation() throws Exception {
        String body = objectMapper.writeValueAsString(register(UserType.STUDENT, "noage@test.ro", null));

        mockMvc.perform(post("/api/v2/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(ErrorConstants.AUTH_STUDENT_AGE_REQUIRED_ERROR));
    }

    @Test
    @DisplayName("Body invalid (@Valid) -> 400 in formatul {message, status}")
    void validationFailure() throws Exception {
        String body = objectMapper.writeValueAsString(
                CourseRequest.builder().name("").departament("IT").build());

        mockMvc.perform(post("/api/v2/courses/add").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("name: Numele cursului e obligatoriu"));
    }
}
