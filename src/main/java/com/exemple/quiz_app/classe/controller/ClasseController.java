package com.exemple.quiz_app.classe.controller;

import com.exemple.quiz_app.classe.dto.*;
import com.exemple.quiz_app.classe.service.ClasseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
public class ClasseController {

    private final ClasseService classeService;

    public ClasseController(ClasseService classeService) {
        this.classeService = classeService;
    }

    @GetMapping("/classes")
    public ResponseEntity<List<ClasseResponse>> getClasses() {
        return ResponseEntity.ok(classeService.getMyClasses());
    }

    @PostMapping("/classes")
    public ResponseEntity<ClasseResponse> createClasse(
            @RequestBody ClasseRequest request
    ) {
        return ResponseEntity.ok(classeService.createClasse(request));
    }

    @DeleteMapping("/classes/{classId}")
    public ResponseEntity<Map<String, String>> deleteClasse(
            @PathVariable Long classId
    ) {
        classeService.deleteClasse(classId);
        return ResponseEntity.ok(Map.of("message", "Classe supprimée."));
    }

    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<List<StudentResponse>> getStudents(
            @PathVariable Long classId
    ) {
        return ResponseEntity.ok(classeService.getStudents(classId));
    }

    @PostMapping("/classes/{classId}/students")
    public ResponseEntity<StudentResponse> addStudent(
            @PathVariable Long classId,
            @RequestBody StudentRequest request
    ) {
        return ResponseEntity.ok(classeService.addStudent(classId, request));
    }

    @DeleteMapping("/classes/{classId}/students/{studentId}")
    public ResponseEntity<Map<String, String>> deleteStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId
    ) {
        classeService.deleteStudentFromClass(classId, studentId);
        return ResponseEntity.ok(Map.of("message", "Étudiant retiré de la classe."));
    }

    @PostMapping("/classes/{classId}/students/import")
    public ResponseEntity<Map<String, Object>> importStudents(
            @PathVariable Long classId,
            @RequestParam("file") MultipartFile file
    ) {
        int imported = classeService.importStudents(classId, file);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Import terminé.",
                        "imported", imported
                )
        );
    }

    @PostMapping("/quizzes/{quizId}/assign-class")
    public ResponseEntity<Map<String, Object>> assignQuizToClass(
            @PathVariable Long quizId,
            @RequestBody AssignClassRequest request
    ) {
        int added = classeService.assignQuizToClass(quizId, request.getClassId());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Quiz affecté à la classe.",
                        "addedStudents", added
                )
        );
    }
}