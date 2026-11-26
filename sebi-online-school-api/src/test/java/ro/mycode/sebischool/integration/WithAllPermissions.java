package ro.mycode.sebischool.integration;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pentru testele de LOGICA (date, erori), unde securitatea nu e subiectul testului.
 * Testele de permisiuni folosesc login real, vezi PermissionsIntegrationTest.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithMockUser(authorities = {
        "STUDENT_VIEW", "STUDENT_MANAGE",
        "PROFESSOR_VIEW", "PROFESSOR_MANAGE",
        "COURSE_VIEW", "COURSE_MANAGE",
        "BOOK_VIEW", "BOOK_MANAGE",
        "ENROL_VIEW", "ENROL_MANAGE"
})
public @interface WithAllPermissions {
}
