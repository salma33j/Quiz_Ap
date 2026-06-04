package com.exemple.quiz_app.auth.dto;

import java.time.LocalDateTime;

public class UserDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    private String cne;
    private String codeApoge;

    private Long classId;
    private String className;
    private String classFiliere;
    private String classNiveau;

    private boolean blocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCne() { return cne; }
    public void setCne(String cne) { this.cne = cne; }

    public String getCodeApoge() { return codeApoge; }
    public void setCodeApoge(String codeApoge) { this.codeApoge = codeApoge; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getClassFiliere() { return classFiliere; }
    public void setClassFiliere(String classFiliere) { this.classFiliere = classFiliere; }

    public String getClassNiveau() { return classNiveau; }
    public void setClassNiveau(String classNiveau) { this.classNiveau = classNiveau; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}