package com.exemple.quiz_app.classe.entity;

import com.exemple.quiz_app.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Classe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String filiere;

    @Column(length = 60)
    private String niveau;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "classe_enseignants",
            joinColumns = @JoinColumn(name = "classe_id"),
            inverseJoinColumns = @JoinColumn(name = "enseignant_id")
    )
    private Set<User> enseignants = new HashSet<>();

    @OneToMany(mappedBy = "classe", fetch = FetchType.LAZY)
    private Set<User> students = new HashSet<>();
}
