package ro.mycode.sebischool.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ro.mycode.sebischool.auth.dtos.AuthLoginRequest;
import ro.mycode.sebischool.auth.dtos.AuthRegisterRequest;
import ro.mycode.sebischool.users.model.UserType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Permisiunile verificate cu LOGIN REAL: /register + /login, apoi Bearer token.
 * Fara @WithMockUser si fara addFilters=false, ca sa treaca tot lantul (parola, JWT, claim authorities).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PermissionsIntegrationTest {

    private static final String COURSE = "{\"name\":\"Java\",\"departament\":\"IT\"}";
    private static final Pattern AUTHORITY = Pattern.compile("hasAuthority\\('([A-Z_]+)'\\)");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    private String tokenFor(UserType type, String email) throws Exception {
        AuthRegisterRequest reg = type == UserType.PROFESSOR
                ? new AuthRegisterRequest(type, "Ana", "Pop", email, "parola123", null, "Java", "IT", 5)
                : new AuthRegisterRequest(type, "Ana", "Pop", email, "parola123", 20, null, null, null);

        mockMvc.perform(post("/api/v2/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        String json = mockMvc.perform(post("/api/v2/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequest(email, "parola123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(json).get("token").asText();
    }

    // ---------- regula: fiecare endpoint are @PreAuthorize, GET=VIEW, rest=MANAGE ----------

    @Test
    @DisplayName("Fiecare endpoint (in afara de /auth) are @PreAuthorize: GET cere *_VIEW, restul *_MANAGE")
    void everyEndpointFollowsViewManageRule() {
        List<String> violations = new ArrayList<>();
        int checked = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod hm = e.getValue();
            if (!hm.getBeanType().isAnnotationPresent(RestController.class)
                    || !hm.getBeanType().getPackageName().startsWith("ro.mycode.sebischool")) {
                continue;
            }
            var patterns = e.getKey().getPathPatternsCondition().getPatternValues();
            if (patterns.stream().allMatch(p -> p.startsWith("/api/v2/auth"))) {
                continue;
            }

            checked++;
            String where = hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
            PreAuthorize pre = hm.getMethod().getAnnotation(PreAuthorize.class);
            if (pre == null) {
                violations.add(where + ": lipseste @PreAuthorize");
                continue;
            }
            Matcher m = AUTHORITY.matcher(pre.value());
            if (!m.find()) {
                violations.add(where + ": @PreAuthorize nu foloseste hasAuthority(X_VIEW|X_MANAGE): " + pre.value());
                continue;
            }
            boolean readOnly = e.getKey().getMethodsCondition().getMethods().stream()
                    .allMatch(method -> method.name().equals("GET"));
            String expectedSuffix = readOnly ? "_VIEW" : "_MANAGE";
            if (!m.group(1).endsWith(expectedSuffix)) {
                violations.add(where + ": cere " + m.group(1) + ", trebuia sa se termine in " + expectedSuffix);
            }
        }

        assertThat(checked).as("endpoint-uri verificate").isGreaterThan(30);
        assertThat(violations).isEmpty();
    }

    // ---------- 401 / 403 / 2xx cu token real ----------

    @Test
    @DisplayName("Fara token -> 401")
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v2/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("STUDENT (doar *_VIEW pe cursuri/studenti/profesori) primeste 403 pe scriere")
    void student_writeWithoutManage_returns403() throws Exception {
        String token = tokenFor(UserType.STUDENT, "stud@test.ro");

        mockMvc.perform(post("/api/v2/courses/add").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(COURSE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/v2/courses/1").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v2/students/1").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v2/profesori/1").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT cu *_VIEW poate citi")
    void student_readWithView_returns200() throws Exception {
        String token = tokenFor(UserType.STUDENT, "stud2@test.ro");

        for (String url : List.of("/api/v2/courses", "/api/v2/students", "/api/v2/profesori",
                "/api/v2/books", "/api/v2/enrolments")) {
            mockMvc.perform(get(url).header("Authorization", token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PROFESSOR cu COURSE_MANAGE poate crea curs")
    void professor_writeWithManage_returns201() throws Exception {
        String token = tokenFor(UserType.PROFESSOR, "prof@test.ro");

        mockMvc.perform(post("/api/v2/courses/add").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(COURSE))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PROFESSOR cu STUDENT_MANAGE ajunge la logica: student inexistent -> 404, nu 403")
    void professor_deleteStudent_passesAuthorization() throws Exception {
        String token = tokenFor(UserType.PROFESSOR, "prof2@test.ro");

        mockMvc.perform(delete("/api/v2/students/999999").header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}
