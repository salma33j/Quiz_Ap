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
    private Long classeId;
    private String className;
    private String classeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDto() {}

    public UserDto(Long id, String firstName, String lastName, String email, String role,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public Long getClasseId() { return classeId; }
    public void setClasseId(Long classeId) { this.classeId = classeId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getClasseName() { return classeName; }
    public void setClasseName(String classeName) { this.classeName = classeName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
