package ro.mycode.sebischool.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.mycode.sebischool.auth.exceptions.AuthValidationException;
import ro.mycode.sebischool.books.exceptions.BookAlreadyExistsException;
import ro.mycode.sebischool.books.exceptions.BookNotFoundException;
import ro.mycode.sebischool.course.exceptions.CourseAlreadyExistsException;
import ro.mycode.sebischool.course.exceptions.CourseFullException;
import ro.mycode.sebischool.course.exceptions.CourseNotFoundException;
import ro.mycode.sebischool.enrolment.exceptions.EnrolmentAlreadyExistsException;
import ro.mycode.sebischool.enrolment.exceptions.EnrolmentNotFoundException;
import ro.mycode.sebischool.professor.exceptions.ProfessorAlreadyExistsException;
import ro.mycode.sebischool.professor.exceptions.ProfessorNotFoundException;
import ro.mycode.sebischool.student.exceptions.EmailAlreadyExistsException;
import ro.mycode.sebischool.student.exceptions.InvalidStudentAgeException;
import ro.mycode.sebischool.student.exceptions.StudentAlreadyExistsException;
import ro.mycode.sebischool.student.exceptions.StudentNotFoundException;
import ro.mycode.sebischool.system.constants.ErrorConstants;
import ro.mycode.sebischool.system.exceptions.ApiErrorResponse;
import ro.mycode.sebischool.system.exceptions.GlobalExceptionsHandler;
import ro.mycode.sebischool.users.exceptions.UserAlreadyExistsException;
import ro.mycode.sebischool.users.exceptions.UserNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionsHandlerTest {

    private final GlobalExceptionsHandler handler = new GlobalExceptionsHandler();

    private void assertResponse(ResponseEntity<ApiErrorResponse> res, HttpStatus expected, String message) {
        assertThat(res.getStatusCode()).isEqualTo(expected);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().status()).isEqualTo(res.getStatusCode().value());
        assertThat(res.getBody().message()).isEqualTo(message);
    }

    @Test
    void notFoundExceptions_return404_andBodyStatusMatches() {
        List<RuntimeException> exceptions = List.of(
                new StudentNotFoundException(), new BookNotFoundException(), new EnrolmentNotFoundException(),
                new CourseNotFoundException(), new UserNotFoundException(), new ProfessorNotFoundException());

        for (RuntimeException e : exceptions) {
            assertResponse(handler.handleNotFound(e), HttpStatus.NOT_FOUND, e.getMessage());
        }
        assertResponse(handler.handleNotFound(new ProfessorNotFoundException()),
                HttpStatus.NOT_FOUND, ErrorConstants.PROFESSOR_NOT_FOUND_ERROR);
    }

    @Test
    void alreadyExistsExceptions_return409_andBodyStatusMatches() {
        List<RuntimeException> exceptions = List.of(
                new StudentAlreadyExistsException(), new CourseAlreadyExistsException(),
                new EnrolmentAlreadyExistsException(), new BookAlreadyExistsException(),
                new EmailAlreadyExistsException(), new UserAlreadyExistsException(),
                new ProfessorAlreadyExistsException());

        for (RuntimeException e : exceptions) {
            assertResponse(handler.handleConflict(e), HttpStatus.CONFLICT, e.getMessage());
        }
        assertResponse(handler.handleConflict(new ProfessorAlreadyExistsException()),
                HttpStatus.CONFLICT, ErrorConstants.PROFESSOR_ALREADY_EXISTS_ERROR);
    }

    @Test
    void invalidRequestExceptions_return400_andBodyStatusMatches() {
        assertResponse(handler.handleBadRequest(new InvalidStudentAgeException()),
                HttpStatus.BAD_REQUEST, ErrorConstants.INVALID_STUDENT_AGE_ERROR);
        assertResponse(handler.handleBadRequest(new CourseFullException()),
                HttpStatus.BAD_REQUEST, ErrorConstants.COURSE_ALREADY_FULL_ERROR);
    }

    @Test
    void authValidation_returns400_withMessageFromErrorConstants() {
        assertResponse(handler.handleAuthValidation(
                        new AuthValidationException(ErrorConstants.AUTH_STUDENT_AGE_REQUIRED_ERROR)),
                HttpStatus.BAD_REQUEST, ErrorConstants.AUTH_STUDENT_AGE_REQUIRED_ERROR);
    }
}
