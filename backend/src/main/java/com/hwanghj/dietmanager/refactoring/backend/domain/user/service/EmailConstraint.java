package com.hwanghj.dietmanager.refactoring.backend.domain.user.service;

import java.util.regex.Pattern;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;

import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;

final class EmailConstraint {
    private static final Pattern H2_EMAIL_INDEX = Pattern.compile(User.EMAIL_UNIQUE_INDEX + "_INDEX_[0-9A-F]+", Pattern.CASE_INSENSITIVE);

    private EmailConstraint() {
    }

    static boolean isViolation(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getKind() == ConstraintKind.UNIQUE) {
                String name = violation.getConstraintName();
                if (name == null)
                    return false;
                String index = name.substring(name.lastIndexOf('.') + 1);
                // H2는 고유 제약을 구현하는 인덱스에 _INDEX_와 16진수 접미사를 붙인다.
                return User.EMAIL_UNIQUE_INDEX.equalsIgnoreCase(index)
                        || ("23505".equals(violation.getSQLState()) && H2_EMAIL_INDEX.matcher(index).matches());
            }
        }
        return false;
    }
}
