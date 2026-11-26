package ro.mycode.sebischool.users.security;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum UserPermissions {
    STUDENT_VIEW("STUDENT_VIEW"),
    STUDENT_MANAGE("STUDENT_MANAGE"),
    PROFESSOR_VIEW("PROFESSOR_VIEW"),
    PROFESSOR_MANAGE("PROFESSOR_MANAGE"),
    COURSE_VIEW("COURSE_VIEW"),
    COURSE_MANAGE("COURSE_MANAGE"),
    BOOK_VIEW("BOOK_VIEW"),
    BOOK_MANAGE("BOOK_MANAGE"),
    ENROL_VIEW("ENROL_VIEW"),
    ENROL_MANAGE("ENROL_MANAGE");

    private final String permission;
    public String getPermission() {
        return permission;
    }
}
