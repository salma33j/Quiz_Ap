package com.exemple.quiz_app.classe.controller;

import com.exemple.quiz_app.classe.dto.ClassStudentDto;
import com.exemple.quiz_app.classe.dto.ClassStudentRequest;
import com.exemple.quiz_app.classe.dto.ClasseDto;
import com.exemple.quiz_app.classe.dto.ClasseRequest;
import com.exemple.quiz_app.classe.dto.TeacherAssignmentRequest;
import com.exemple.quiz_app.classe.service.ClasseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/classes")
@PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
public class ClasseController {

    private final ClasseService classeService;

    public ClasseController(ClasseService classeService) {
        this.classeService = classeService;
    }

    @GetMapping
    public ResponseEntity<List<ClasseDto>> getVisibleClasses() {
        return ResponseEntity.ok(classeService.getVisibleClasses());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClasseDto>> getAllClasses() {
        return ResponseEntity.ok(classeService.getAllClasses());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClasseDto> createClasse(@RequestBody ClasseRequest request) {
        return ResponseEntity.ok(classeService.createClasse(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClasseDto> updateClasse(@PathVariable Long id, @RequestBody ClasseRequest request) {
        return ResponseEntity.ok(classeService.updateClasse(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteClasse(@PathVariable Long id) {
        classeService.deleteClasse(id);
        return ResponseEntity.ok(Map.of("message", "Classe supprimee"));
    }

    @PostMapping("/{id}/teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClasseDto> assignTeachers(
            @PathVariable Long id,
            @RequestBody TeacherAssignmentRequest request
    ) {
        return ResponseEntity.ok(classeService.assignTeachers(id, request.getTeacherIds()));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<List<ClassStudentDto>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(classeService.getStudents(id));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClassStudentDto> addStudent(
            @PathVariable Long id,
            @RequestBody ClassStudentRequest request
    ) {
        return ResponseEntity.ok(classeService.addStudent(id, request));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeStudent(
            @PathVariable Long id,
            @PathVariable Long studentId
    ) {
        classeService.removeStudent(id, studentId);
        return ResponseEntity.ok(Map.of("message", "Etudiant retire de la classe"));
    }

    @PostMapping("/{id}/students/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importStudents(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(classeService.importStudents(id, file));
    }
}
