package com.exemple.quiz_app.auth.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import com.exemple.quiz_app.auth.model.Role;
@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private BigInteger id;
    @NotBlank(message="Le nom est obligatoire")
    @Column(nullable = false)
    private String firstName;
    @NotBlank(message="Le prenom est obligatoire")
    @Column(nullable = false)
    private String lastName;
    @NotBlank(message="Email est obligatoire")
    @Email(message="Format email doit etre valide")
    @Column(nullable = false, unique = true)
    private String email;
    @NotBlank(message="Le mot de passe est obligatoire")
    @Size(min=8,message="Le mot de passe doit contenir au moins 8 caracteres")
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role ;
    public User(){}
    public User(BigInteger id,String prenom,String nom,String email,String passwd,Role role){
        this.id=id;
        this.firstName=prenom;
        this.lastName=nom;
        this.password=passwd;
        this.email=email;
        this.role=role;
    }
    public User(String firstName,String lastName,String nom, String email, String password, Role role) {
        this.firstName = firstName;
        this.lastName=lastName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public BigInteger getId() { return id; }
    public void setId(BigInteger id) { this.id = id; }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + firstName + '\'' +
                ",prenom='"+lastName+'\''+
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}//
