package com.help.erpweb.domain.academy.entity;

import lombok.Getter;

@Getter
public enum AcademyRole {
    STUDENT,
    TEACHER,
    ACADEMY_OWNER;

//    public boolean isTeacherOrAbove() {
//        return this == TEACHER
//                || this == ACADEMY_OWNER;
//    }
//
//    public boolean isOwner() {
//        return this == ACADEMY_OWNER;
//    }
}