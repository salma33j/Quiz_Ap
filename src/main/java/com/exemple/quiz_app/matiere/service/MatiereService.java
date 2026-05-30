package com.exemple.quiz_app.matiere.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import com.exemple.quiz_app.matiere.dto.MatiereRequest;
import com.exemple.quiz_app.matiere.dto.MatiereResponse;
import com.exemple.quiz_app.matiere.entity.Matiere;
import com.exemple.quiz_app.matiere.repository.MatiereRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatiereService {

    private final MatiereRepository matiereRepository;
    private final ClasseRepository classeRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public MatiereService(
            MatiereRepository matiereRepository,
            ClasseRepository classeRepository,
            UserRepository userRepository,
            AuthService authService
    ) {
        this.matiereRepository = matiereRepository;
        this.classeRepository = classeRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public List<MatiereResponse> getMatieres() {
        User requester = authService.getCurrentUser();
        List<Matiere> matieres = requester.getRole() == Role.ADMIN
                ? matiereRepository.findAllByOrderByCreatedAtDesc()
                : matiereRepository.findByEnseignantIdOrderByCreatedAtDesc(requester.getId());

        return matieres.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MatiereResponse> getMatieresByClasse(Long classId) {
        User requester = authService.getCurrentUser();
        Classe classe = getAccessibleClasse(classId, requester);

        return matiereRepository.findByClasseIdOrderByNomAsc(classe.getId())
                .stream()
                .filter(matiere -> requester.getRole() == Role.ADMIN || matiere.getEnseignant().getId().equals(requester.getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MatiereResponse createMatiere(MatiereRequest request) {
        User requester = authService.getCurrentUser();
        Classe classe = getAccessibleClasse(request.getClassId(), requester);
        User teacher = getTeacherForRequest(request.getTeacherId(), requester);

        validateMatiere(request, classe, teacher, null);

        Matiere matiere = new Matiere();
        matiere.setNom(request.getNom().trim());
        matiere.setDescription(cleanText(request.getDescription()));
        matiere.setClasse(classe);
        matiere.setEnseignant(teacher);

        return toResponse(matiereRepository.save(matiere));
    }

    @Transactional
    public MatiereResponse updateMatiere(Long id, MatiereRequest request) {
        User requester = authService.getCurrentUser();
        Matiere matiere = matiereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matiere introuvable."));

        if (requester.getRole() != Role.ADMIN && !matiere.getEnseignant().getId().equals(requester.getId())) {
            throw new RuntimeException("Acces interdit a cette matiere.");
        }

        Classe classe = getAccessibleClasse(request.getClassId(), requester);
        User teacher = getTeacherForRequest(request.getTeacherId(), requester);

        validateMatiere(request, classe, teacher, id);

        matiere.setNom(request.getNom().trim());
        matiere.setDescription(cleanText(request.getDescription()));
        matiere.setClasse(classe);
        matiere.setEnseignant(teacher);

        return toResponse(matiereRepository.save(matiere));
    }

    @Transactional
    public void deleteMatiere(Long id) {
        User requester = authService.getCurrentUser();
        Matiere matiere = matiereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matiere introuvable."));

        if (requester.getRole() != Role.ADMIN && !matiere.getEnseignant().getId().equals(requester.getId())) {
            throw new RuntimeException("Acces interdit a cette matiere.");
        }

        matiereRepository.delete(matiere);
    }

    private void validateMatiere(MatiereRequest request, Classe classe, User teacher, Long currentMatiereId) {
        if (request.getNom() == null || request.getNom().isBlank()) {
            throw new RuntimeException("Le nom de la matiere est obligatoire.");
        }

        boolean duplicate = currentMatiereId == null
                ? matiereRepository.existsByNomIgnoreCaseAndClasseId(
                        request.getNom().trim(),
                        classe.getId()
                )
                : matiereRepository.existsByNomIgnoreCaseAndClasseIdAndIdNot(
                        request.getNom().trim(),
                        classe.getId(),
                        currentMatiereId
                );

        if (duplicate) {
            throw new RuntimeException("Cette matiere existe deja pour cette classe.");
        }
    }

    private Classe getAccessibleClasse(Long classId, User requester) {
        if (classId == null) {
            throw new RuntimeException("Veuillez choisir une classe.");
        }

        Classe classe = classeRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable."));

        if (requester.getRole() == Role.ADMIN || isTeacherAssignedToClass(classe, requester)) {
            return classe;
        }

        throw new RuntimeException("Acces interdit a cette classe.");
    }

    private User getTeacherForRequest(Long teacherId, User requester) {
        Long selectedTeacherId = requester.getRole() == Role.ADMIN ? teacherId : requester.getId();
        if (selectedTeacherId == null) {
            throw new RuntimeException("Veuillez choisir un enseignant.");
        }

        User teacher = userRepository.findById(selectedTeacherId)
                .orElseThrow(() -> new RuntimeException("Enseignant introuvable."));

        if (teacher.getRole() != Role.ENSEIGNANT) {
            throw new RuntimeException("L'utilisateur choisi doit etre un enseignant.");
        }

        return teacher;
    }

    private boolean isTeacherAssignedToClass(Classe classe, User teacher) {
        if (classe == null || teacher == null) return false;
        if (classe.getEnseignant() != null && classe.getEnseignant().getId().equals(teacher.getId())) {
            return true;
        }
        Set<User> enseignants = classe.getEnseignants();
        return enseignants != null && enseignants.stream()
                .anyMatch(item -> item.getId().equals(teacher.getId()));
    }

    private MatiereResponse toResponse(Matiere matiere) {
        MatiereResponse response = new MatiereResponse();
        response.setId(matiere.getId());
        response.setNom(matiere.getNom());
        response.setDescription(matiere.getDescription());
        response.setCreatedAt(matiere.getCreatedAt());

        Classe classe = matiere.getClasse();
        if (classe != null) {
            response.setClassId(classe.getId());
            response.setClassName(classe.getName());
            response.setClassFiliere(classe.getFiliere());
            response.setClassNiveau(classe.getNiveau());
        }

        User teacher = matiere.getEnseignant();
        if (teacher != null) {
            response.setTeacherId(teacher.getId());
            response.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName());
            response.setTeacherEmail(teacher.getEmail());
        }

        return response;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }
}
