package com.exemple.quiz_app.classe.dto;

import lombok.Data;

import java.util.List;

@Data
public class TeacherAssignmentRequest {
    private List<Long> teacherIds;
}
