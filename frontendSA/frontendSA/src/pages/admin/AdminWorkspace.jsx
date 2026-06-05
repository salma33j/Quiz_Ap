import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  AlertTriangle,
  BarChart3,
  BookMarked,
  BookOpen,
  GraduationCap,
  Mail,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  Trophy,
  UserPlus,
  Users,
} from "lucide-react";
import adminApi from "../../api/adminApi";
import ConfirmDialog from "../../components/common/ConfirmDialog";
import styles from "./AdminWorkspace.module.css";

const emptyUser = {
  firstName: "",
  lastName: "",
  email: "",
  role: "ETUDIANT",
  classId: "",
  cne: "",
  codeApoge: "",
};

const emptyClass = { name: "", filiere: "", niveau: "", teacherIds: [] };
const emptySubject = { nom: "", classId: "", teacherId: "" };
const emptyEmail = { target: "ETUDIANTS", subject: "", message: "" };
const adminGeneratedPasswordPlaceholder = "AdminTemp123!";

const getId = (item) => item?.id ?? item?._id;

// ✅ Important : pour les matières, on n'utilise PAS classId/teacherId
// sinon le bouton supprimer peut envoyer l'id de la classe ou du prof
// et le backend répond : "Matière introuvable".
const getSubjectId = (subject) =>
  subject?.id ?? subject?.matiereId ?? subject?.subjectId ?? subject?._id;
const asArray = (value) => {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.content)) return value.content;
  if (Array.isArray(value?.items)) return value.items;
  if (Array.isArray(value?.classes)) return value.classes;
  if (Array.isArray(value?.data)) return value.data;
  return [];
};
const fullName = (user) =>
  [user?.firstName || user?.prenom, user?.lastName || user?.nom].filter(Boolean).join(" ") ||
  user?.name ||
  user?.email ||
  "Utilisateur";
const roleOf = (user) => String(user?.role || "ETUDIANT").toUpperCase();
const isBlockedUser = (user) =>
  Boolean(user?.blocked || user?.isBlocked || user?.active === false || user?.enabled === false);
const quizTitle = (quiz) => quiz?.titre || quiz?.title || "Quiz sans titre";
const isPublished = (quiz) => String(quiz?.status || "").toUpperCase() === "PUBLISHED";
const percent = (value) => `${Math.round(Number(value || 0))}%`;
const apiMessage = (error, fallback) => {
  const data = error?.response?.data;
  if (data?.code === "VALIDATION_ERROR" && data?.details) {
    return `${data.message || "Erreur de validation"} : ${data.details}`;
  }
  const raw = data?.message || data?.error || data?.details || error?.message || "";
  const text = String(raw).toLowerCase();

  if (
    text.includes("foreign key") ||
    text.includes("constraint") ||
    text.includes("could not execute statement") ||
    text.includes("sql")
  ) {
    return "Action impossible : cet élément est encore lié à des données existantes.";
  }

  return raw || fallback;
};
const buildUserPayload = (form) => {
  const payload = {
    ...form,
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: form.email.trim(),
    role: form.role,
    password: form.password || adminGeneratedPasswordPlaceholder,
  };

  if (form.role === "ETUDIANT") {
    payload.classId = form.classId ? Number(form.classId) : null;
    payload.classeId = payload.classId;
    payload.cne = form.cne.trim();
    payload.codeApoge = form.codeApoge.trim();
  } else {
    delete payload.classId;
    delete payload.classeId;
    delete payload.cne;
    delete payload.codeApoge;
  }

  return payload;
};
const normalizeSearch = (value) =>
  String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
const userActionMessage = (feedback) => {
  if (!feedback) return "";
  if (feedback.type !== "error") return feedback.message;

  const raw = String(feedback.message || "");
  const text = raw.toLowerCase();

  if (text.includes("classe_enseignants")) return "Affecté à une classe";
  if (text.includes("classes") && text.includes("enseignant_id")) return "Responsable d'une classe";
  if (text.includes("responsable de la classe")) return "Responsable d'une classe";
  if (text.includes("quiz_students")) return "Étudiant lié à un quiz";
  if (text.includes("id_enseignant") || text.includes("possede encore des quiz") || text.includes("possède encore des quiz")) {
    return "Possède encore des quiz";
  }
  if (text.includes("propre compte")) return "Compte connecté";
  if (text.includes("introuvable")) return "Utilisateur introuvable";
  if (raw.length <= 34) return raw;
  return "Suppression impossible";
};
const teacherNamesOf = (classe) => {
  if (Array.isArray(classe?.enseignantNames)) return classe.enseignantNames;
  if (Array.isArray(classe?.enseignants)) return classe.enseignants.map(fullName);
  return [classe?.enseignantName || classe?.enseignant?.name || fullName(classe?.enseignant)].filter(
    (name) => name && name !== "Utilisateur"
  );
};

const getQuizAvailableUntil = (quiz) =>
  quiz?.availableUntil || quiz?.dateFin || quiz?.endDate || quiz?.expiresAt || quiz?.expirationDate;

const getQuizCreatedAt = (quiz) =>
  quiz?.createdAt || quiz?.dateCreation || quiz?.createdDate || quiz?.publishedAt || quiz?.availableFrom;

const getQuizStatus = (quiz) => String(quiz?.status || quiz?.statut || "DRAFT").toUpperCase();

const isExpiredQuiz = (quiz) => {
  if (getQuizStatus(quiz) === "EXPIRED") return true;
  const availableUntil = getQuizAvailableUntil(quiz);
  if (!availableUntil) return false;
  const endDate = new Date(availableUntil);
  return Number.isFinite(endDate.getTime()) && endDate <= new Date();
};

const isDraftQuiz = (quiz) => getQuizStatus(quiz) === "DRAFT";
const isActivePublishedQuiz = (quiz) => getQuizStatus(quiz) === "PUBLISHED" && !isExpiredQuiz(quiz);

const addMonths = (date, months) => {
  const next = new Date(date);
  next.setMonth(next.getMonth() + months);
  return next;
};

const canDeleteQuiz = (quiz) => {
  if (getQuizStatus(quiz) !== "PUBLISHED") return true;

  const reference = quiz?.availableFrom || getQuizCreatedAt(quiz);
  if (!reference) return false;

  const referenceDate = new Date(reference);
  return Number.isFinite(referenceDate.getTime()) && addMonths(referenceDate, 5) <= new Date();
};

const formatDate = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return "-";
  return date.toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const getScorePercent = (row) => {
  if (row?.scorePercentage != null) return Number(row.scorePercentage);
  if (row?.percentage != null) return Number(row.percentage);
  if (row?.noteSur20 != null) return Number(row.noteSur20) * 5;

  const earned = Number(row?.earnedPoints ?? row?.pointsObtenus ?? row?.points);
  const total = Number(row?.totalPoints ?? row?.total);
  if (Number.isFinite(earned) && Number.isFinite(total) && total > 0) {
    return (earned * 100) / total;
  }

  const note = Number(row?.note ?? row?.score);
  if (Number.isFinite(note)) return note <= 20 ? note * 5 : note;
  return 0;
};

const getNoteSur20 = (row) => getScorePercent(row) / 5;

const getStudentFirstName = (row, student) =>
  row?.studentFirstName || row?.firstName || row?.student?.firstName || row?.etudiant?.firstName || student?.firstName || student?.prenom || "";

const getStudentLastName = (row, student) =>
  row?.studentLastName || row?.lastName || row?.student?.lastName || row?.etudiant?.lastName || student?.lastName || student?.nom || "";

const getStudentEmail = (row, student) =>
  row?.email || row?.studentEmail || row?.student?.email || row?.etudiant?.email || student?.email || "-";

const getStudentCne = (row, student) =>
  row?.cne || row?.studentCne || row?.student?.cne || row?.etudiant?.cne || student?.cne || "-";

const getStudentCodeApogee = (row, student) =>
  row?.codeApogee ||
  row?.codeApoge ||
  row?.studentCodeApogee ||
  row?.studentCodeApoge ||
  row?.student?.codeApogee ||
  row?.student?.codeApoge ||
  row?.etudiant?.codeApogee ||
  row?.etudiant?.codeApoge ||
  student?.codeApogee ||
  student?.codeApoge ||
  "-";

const getSubmissionDate = (row) =>
  row?.submittedAt || row?.dateSoumission || row?.completedDate || row?.createdAt || row?.updatedAt || row?.startedAt;

export default function AdminWorkspace({ section = "dashboard" }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const [globalStats, setGlobalStats] = useState(null);
  const [quizStats, setQuizStats] = useState({});
  const [rankings, setRankings] = useState({});
  const [quizResults, setQuizResults] = useState({});
  const [userForm, setUserForm] = useState(emptyUser);
  const [accountMode, setAccountMode] = useState("single");
  const [selectedImportFile, setSelectedImportFile] = useState(null);
  const [editingUserId, setEditingUserId] = useState(null);
  const [classForm, setClassForm] = useState(emptyClass);
  const [editingClassId, setEditingClassId] = useState(null);
  const [subjectForm, setSubjectForm] = useState(emptySubject);
  const [editingSubjectId, setEditingSubjectId] = useState(null);
  const [emailForm, setEmailForm] = useState(emptyEmail);
  const [query, setQuery] = useState("");
  const [subjectQuery, setSubjectQuery] = useState("");
  const [quizQuery, setQuizQuery] = useState("");
  const [quizStatusFilter, setQuizStatusFilter] = useState("ALL");
  const [resultsQuery, setResultsQuery] = useState("");
  const [working, setWorking] = useState("");
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [userFormNotice, setUserFormNotice] = useState("");
  const [userFormError, setUserFormError] = useState("");
  const [userListError, setUserListError] = useState("");
  const [userActionFeedback, setUserActionFeedback] = useState(null);
  const [pendingDeleteUserId, setPendingDeleteUserId] = useState("");
  const [confirmDialog, setConfirmDialog] = useState(null);
  const userActionFeedbackTimerRef = useRef(null);
  const userFormCardRef = useRef(null);
  const classFormCardRef = useRef(null);
  const subjectFormCardRef = useRef(null);

  const scrollToPanel = useCallback((ref) => {
    window.setTimeout(() => {
      ref.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }, 0);
  }, []);

  const askConfirmation = useCallback((options) => (
    new Promise((resolve) => {
      setConfirmDialog({ ...options, resolve });
    })
  ), []);

  const closeConfirmDialog = useCallback((confirmed) => {
    setConfirmDialog((current) => {
      current?.resolve(Boolean(confirmed));
      return null;
    });
  }, []);

  useEffect(() => {
    loadAdminData();
  }, []);

  useEffect(() => {
    let cancelled = false;

    Promise.resolve().then(() => {
      if (cancelled) return;
      setNotice("");
      setError("");
      setUserFormNotice("");
      setUserFormError("");
      setUserListError("");
      setUserActionFeedback(null);
    });

    return () => {
      cancelled = true;
    };
  }, [section]);

  useEffect(() => () => {
    if (userActionFeedbackTimerRef.current) {
      window.clearTimeout(userActionFeedbackTimerRef.current);
    }
  }, []);

  useEffect(() => {
    const selectedClassId = location.state?.classId;
    let cancelled = false;

    Promise.resolve().then(() => {
    if (!cancelled && section === "users" && selectedClassId) {
      setEditingUserId(null);
      setUserForm((prev) => ({
        ...prev,
        role: "ETUDIANT",
        classId: String(selectedClassId),
      }));
      scrollToPanel(userFormCardRef);
    }
    });

    return () => {
      cancelled = true;
    };
  }, [section, location.state, scrollToPanel]);

  async function loadAdminData({ silent = false } = {}) {
    try {
      if (!silent) {
        setLoading(true);
        setError("");
      }

      const [usersData, classesData, subjectsData, quizzesData, globalData] = await Promise.all([
        adminApi.getUsers(),
        adminApi.getClasses().catch(() => []),
        adminApi.getSubjects().catch(() => []),
        adminApi.getQuizzes().catch(() => []),
        adminApi.getGlobalStats().catch(() => null),
      ]);

      const nextQuizzes = asArray(quizzesData);
      setUsers(asArray(usersData));
      setClasses(asArray(classesData));
      setSubjects(asArray(subjectsData));
      setQuizzes(nextQuizzes);
      setGlobalStats(globalData);

      const published = nextQuizzes.filter(isPublished).slice(0, 8);
      const statsEntries = await Promise.all(
        published.map(async (quiz) => [
          String(getId(quiz)),
          await adminApi.getQuizStatistics(getId(quiz)).catch(() => null),
        ])
      );
      const rankingEntries = await Promise.all(
        published.map(async (quiz) => [
          String(getId(quiz)),
          await adminApi.getQuizRanking(getId(quiz)).catch(() => []),
        ])
      );
      const resultEntries = await Promise.all(
        nextQuizzes.map(async (quiz) => [
          String(getId(quiz)),
          await adminApi.getQuizResults(getId(quiz)).catch(() => []),
        ])
      );
      setQuizStats(Object.fromEntries(statsEntries));
      setRankings(Object.fromEntries(rankingEntries));
      setQuizResults(Object.fromEntries(resultEntries));
    } catch (e) {
      setError(apiMessage(e, "Impossible de charger l'espace administrateur."));
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }

  function clearNotice() {
    setNotice("");
    setError("");
    setUserFormNotice("");
    setUserFormError("");
    setUserListError("");
    setUserActionFeedback(null);
    if (userActionFeedbackTimerRef.current) {
      window.clearTimeout(userActionFeedbackTimerRef.current);
    }
  }

  function showUserActionFeedback(userId, message, type = "success", duration = 1800) {
    if (userActionFeedbackTimerRef.current) {
      window.clearTimeout(userActionFeedbackTimerRef.current);
    }
    setUserActionFeedback({ userId: String(userId), message, type });
    userActionFeedbackTimerRef.current = window.setTimeout(() => {
      setUserActionFeedback((current) =>
        current?.userId === String(userId) ? null : current
      );
    }, duration);
  }

  async function createUser(event) {
    event.preventDefault();
    if (!userForm.firstName.trim() || !userForm.lastName.trim() || !userForm.email.trim()) {
      setUserFormError("Veuillez remplir le nom, prénom et email.");
      return;
    }
    if (!editingUserId && userForm.role === "ETUDIANT" && !userForm.classId) {
      setUserFormError("Veuillez choisir une classe pour cet étudiant.");
      return;
    }
    if (userForm.role === "ETUDIANT" && (!userForm.cne.trim() || !userForm.codeApoge.trim())) {
      setUserFormError("Veuillez saisir le CNE et le Code Apogée de l'étudiant.");
      return;
    }

    try {
      clearNotice();
      setWorking("create-user");
      const payload = buildUserPayload(userForm);
      if (editingUserId) {
        await adminApi.updateUser(editingUserId, payload);
      } else if (payload.role === "ENSEIGNANT") {
        await adminApi.createTeacher(payload);
      } else if (payload.role === "ADMIN") {
        await adminApi.createAdmin(payload);
      } else {
        await adminApi.createStudent(payload);
      }
      setUserForm(emptyUser);
      setEditingUserId(null);
      setUserFormNotice(editingUserId ? "Utilisateur modifié avec succès." : "Compte créé avec succès.");
      await loadAdminData({ silent: true });
    } catch (e) {
      setUserFormError(apiMessage(e, "Enregistrement impossible."));
    } finally {
      setWorking("");
    }
  }

  function editUser(user) {
    clearNotice();
    setAccountMode("single");
    setEditingUserId(getId(user));
    setUserForm({
      firstName: user?.firstName || user?.prenom || "",
      lastName: user?.lastName || user?.nom || "",
      email: user?.email || "",
      role: roleOf(user),
      classId: user?.classeId || user?.classId || "",
      cne: user?.cne || "",
      codeApoge: user?.codeApoge || user?.codeApogee || "",
    });
    scrollToPanel(userFormCardRef);
  }

  async function toggleBlockUser(user) {
    const id = getId(user);
    const blocked = isBlockedUser(user);
    try {
      clearNotice();
      setWorking(`block-user-${id}`);
      if (blocked) {
        await adminApi.unblockUser(id);
      } else {
        await adminApi.blockUser(id);
      }
      setUsers((prev) =>
        prev.map((item) =>
          String(getId(item)) === String(id)
            ? { ...item, blocked: !blocked, active: blocked, enabled: blocked }
            : item
        )
      );
      showUserActionFeedback(id, blocked ? "Utilisateur débloqué." : "Utilisateur bloqué.");
    } catch (e) {
      showUserActionFeedback(
        id,
        e?.response?.data?.message || e?.response?.data?.error || "Action de blocage impossible.",
        "error",
        3600
      );
    } finally {
      setWorking("");
    }
  }

  async function resetUserPassword(user) {
    const id = getId(user);
    try {
      clearNotice();
      setWorking(`reset-user-${id}`);
      await adminApi.resetPassword(id);
      showUserActionFeedback(id, "Mot de passe réinitialisé.");
    } catch (e) {
      showUserActionFeedback(
        id,
        e?.response?.data?.message || e?.response?.data?.error || "Réinitialisation impossible.",
        "error",
        3600
      );
    } finally {
      setWorking("");
    }
  }

  async function deleteUser(id) {
    try {
      clearNotice();
      setWorking(`delete-user-${id}`);
      const response = await adminApi.deleteUser(id);
      if (response?.success === false) {
        throw { response: { data: response } };
      }
      setPendingDeleteUserId("");
      showUserActionFeedback(id, "Utilisateur supprimé.", "success", 1400);
      window.setTimeout(() => {
        setUsers((prev) => prev.filter((user) => String(getId(user)) !== String(id)));
      }, 900);
    } catch (e) {
      setPendingDeleteUserId("");
      showUserActionFeedback(
        id,
        e?.response?.data?.message || e?.response?.data?.error || "Suppression impossible.",
        "error",
        4400
      );
    } finally {
      setWorking("");
    }
  }

  async function createClass(event) {
    event.preventDefault();
    if (!classForm.name.trim()) {
      setError("Veuillez saisir le nom de la classe.");
      return;
    }
    if (classForm.teacherIds.length === 0) {
      setError("Veuillez sélectionner au moins un enseignant pour cette classe.");
      return;
    }

    try {
      clearNotice();
      setWorking("create-class");
      const savedClass = editingClassId
        ? await adminApi.updateClass(editingClassId, classForm)
        : await adminApi.createClass(classForm);
      setClassForm(emptyClass);
      setEditingClassId(null);
      setNotice(editingClassId ? "Classe modifiée avec succès." : "Classe créée avec succès.");
      setClasses((prev) => {
        const nextClass = savedClass && getId(savedClass) ? savedClass : null;
        if (!nextClass) {
          return prev;
        }
        if (prev.some((classe) => String(getId(classe)) === String(getId(nextClass)))) {
          return prev.map((classe) => (String(getId(classe)) === String(getId(nextClass)) ? nextClass : classe));
        }
        return [nextClass, ...prev];
      });
      const refreshedClasses = asArray(await adminApi.getClasses().catch(() => []));
      if (refreshedClasses.length > 0) {
        setClasses(refreshedClasses);
      }
    } catch (e) {
      const message = e?.response?.data?.message || e?.response?.data?.error || "";
      setError(
        message.includes("No static resource") || message.includes("not supported")
          ? "Modification indisponible : redémarrez le backend pour charger les nouvelles routes des classes."
          : message || "Enregistrement de classe impossible."
      );
    } finally {
      setWorking("");
    }
  }

  function editClass(classe) {
    clearNotice();
    setEditingClassId(getId(classe));
    setClassForm({
      name: classe.name || "",
      filiere: classe.filiere || "",
      niveau: classe.niveau || "",
      teacherIds: (classe.enseignantIds || (classe.enseignantId ? [classe.enseignantId] : [])).map(String),
    });
    scrollToPanel(classFormCardRef);
  }

  function goToStudentsForClass(classe) {
    navigate("/admin/users", {
      state: {
        classId: getId(classe),
        className: classe.name,
      },
    });
  }

  async function importStudentsToClass(classId, file) {
    if (!file) return;

    try {
      clearNotice();
      setWorking(`import-students-${classId}`);
      const result = await adminApi.importStudentsToClass(classId, file);
      setUserFormNotice(`${result?.imported ?? 0} étudiant(s) importé(s) dans la classe.`);
      setSelectedImportFile(null);
      await loadAdminData({ silent: true });
    } catch (e) {
      setUserFormError(e?.response?.data?.message || "Import Excel impossible pour cette classe.");
    } finally {
      setWorking("");
    }
  }

  async function importAccountsExcel(file) {
    if (!file) {
      setUserFormError("Veuillez choisir un fichier Excel avant de créer les comptes.");
      return;
    }

    if (userForm.role === "ETUDIANT") {
      if (!userForm.classId) {
        setUserFormError("Veuillez choisir une classe avant d'importer les étudiants.");
        return;
      }
      await importStudentsToClass(userForm.classId, file);
      return;
    }

    try {
      clearNotice();
      setWorking(`import-users-${userForm.role}`);
      const result = await adminApi.importUsersExcel(userForm.role, file);
      setUserFormNotice(`${result?.imported ?? 0} compte(s) ${userForm.role.toLowerCase()} importé(s).`);
      setSelectedImportFile(null);
      await loadAdminData({ silent: true });
    } catch (e) {
      const message = e?.response?.data?.message || e?.response?.data?.error || "";
      setUserFormError(
        message.includes("No static resource")
          ? "Import indisponible : redémarrez le backend pour charger les nouvelles routes d'import."
          : message || "Import Excel impossible."
      );
    } finally {
      setWorking("");
    }
  }

  async function deleteClass(id) {
    const confirmed = await askConfirmation({
      title: "Supprimer cette classe",
      message: "Cette action va supprimer la classe sélectionnée si elle n'est pas encore liée à des données bloquées.",
      confirmLabel: "Supprimer",
      cancelLabel: "Annuler",
    });
    if (!confirmed) return;

    try {
      clearNotice();
      setWorking(`delete-class-${id}`);
      await adminApi.deleteClass(id);
      setClasses((prev) => prev.filter((classe) => String(getId(classe)) !== String(id)));
      setNotice("Classe supprimée.");
    } catch (e) {
      setError(apiMessage(e, "Suppression de classe impossible."));
    } finally {
      setWorking("");
    }
  }

  async function createSubject(event) {
    event.preventDefault();

    if (!subjectForm.nom.trim()) {
      setError("Veuillez saisir le nom de la matière.");
      return;
    }
    if (!subjectForm.teacherId) {
      setError("Veuillez choisir un enseignant pour cette matière.");
      return;
    }
    if (!subjectForm.classId) {
      setError("Veuillez choisir une classe pour cette matière.");
      return;
    }

    try {
      clearNotice();
      setWorking("create-subject");

      const payload = {
        nom: subjectForm.nom.trim(),
        teacherId: subjectForm.teacherId,
        classId: subjectForm.classId,
      };

      const savedSubject = editingSubjectId
        ? await adminApi.updateSubject(editingSubjectId, payload)
        : await adminApi.createSubject(payload);

      setSubjectForm(emptySubject);
      setEditingSubjectId(null);
      setNotice(editingSubjectId ? "Matière modifiée avec succès." : "Matière créée avec succès.");

      setSubjects((prev) => {
        const nextSubject = savedSubject && getId(savedSubject) ? savedSubject : null;
        if (!nextSubject) return prev;
        if (prev.some((subject) => String(getId(subject)) === String(getId(nextSubject)))) {
          return prev.map((subject) =>
            String(getId(subject)) === String(getId(nextSubject)) ? nextSubject : subject
          );
        }
        return [nextSubject, ...prev];
      });

      const refreshedSubjects = asArray(await adminApi.getSubjects().catch(() => []));
      if (refreshedSubjects.length > 0) setSubjects(refreshedSubjects);
    } catch (e) {
      const message = e?.response?.data?.message || e?.response?.data?.error || "";
      setError(
        message.includes("No static resource") || message.includes("not supported")
          ? "Gestion des matières indisponible : redémarrez le backend pour charger les routes /api/admin/subjects."
          : message || "Enregistrement de matière impossible."
      );
    } finally {
      setWorking("");
    }
  }

  function editSubject(subject) {
    clearNotice();
    setEditingSubjectId(getSubjectId(subject));
    setSubjectForm({
      nom: subject?.nom || subject?.name || subject?.titre || "",
      teacherId: String(
        subject?.teacherId ||
          subject?.enseignantId ||
          getId(subject?.teacher) ||
          getId(subject?.enseignant) ||
          ""
      ),
      classId: String(
        subject?.classId ||
          subject?.classeId ||
          getId(subject?.classe) ||
          getId(subject?.classEntity) ||
          ""
      ),
    });
    scrollToPanel(subjectFormCardRef);
  }

  async function deleteSubject(subject) {
    const id = getSubjectId(subject);

    if (!id) {
      setNotice("");
      setError("Impossible de supprimer : identifiant de matière introuvable.");
      return;
    }

    try {
      clearNotice();
      setWorking(`delete-subject-${id}`);

      await adminApi.deleteSubject(id);

      setSubjects((prev) =>
        prev.filter((item) => String(getSubjectId(item)) !== String(id))
      );

      setError("");
      setNotice("Matière supprimée avec succès.");

      window.setTimeout(() => {
        setNotice("");
      }, 3000);
    } catch (e) {
      setNotice("");
      setError(
        e?.response?.data?.message ||
          e?.response?.data?.error ||
          "Suppression de matière impossible."
      );
    } finally {
      setWorking("");
    }
  }

  async function deleteQuiz(id) {
    const confirmed = await askConfirmation({
      title: "Supprimer ce quiz",
      message: "Le quiz sera retiré de la liste. Confirmez uniquement si vous voulez vraiment le supprimer.",
      confirmLabel: "Supprimer",
      cancelLabel: "Annuler",
    });
    if (!confirmed) return;

    try {
      clearNotice();
      setWorking(`delete-quiz-${id}`);
      await adminApi.deleteQuiz(id);
      setQuizzes((prev) => prev.filter((quiz) => String(getId(quiz)) !== String(id)));
      setNotice("Quiz supprimé.");
    } catch (e) {
      setError(apiMessage(e, "Suppression du quiz impossible."));
    } finally {
      setWorking("");
    }
  }

  async function sendEmail(event) {
    event.preventDefault();
    try {
      clearNotice();
      setWorking("send-email");
      const result = await adminApi.sendEmail(emailForm);
      setEmailForm(emptyEmail);
      setNotice(
        result?.sentCount
          ? `Email envoyé à ${result.sentCount} destinataire(s).`
          : result?.message || "Email envoyé avec succès."
      );
    } catch (e) {
      setError(
        e?.response?.data?.message ||
          e?.response?.data?.error ||
          "Envoi de l'email impossible."
      );
    } finally {
      setWorking("");
    }
  }

  const classById = useMemo(
    () => new Map(classes.map((classe) => [String(getId(classe)), classe])),
    [classes]
  );

  const getUserClassInfo = useCallback((user) => {
    const classId =
      user?.classId ||
      user?.classeId ||
      getId(user?.classe) ||
      getId(user?.classEntity) ||
      getId(user?.classeDto);
    const classe = classId ? classById.get(String(classId)) : null;
    const className =
      classe?.name ||
      user?.className ||
      user?.classeName ||
      user?.classe?.name ||
      user?.classEntity?.name ||
      user?.classeDto?.name ||
      "Classe non définie";
    const classFiliere =
      classe?.filiere ||
      user?.classFiliere ||
      user?.classeFiliere ||
      user?.classe?.filiere ||
      user?.classEntity?.filiere ||
      user?.classeDto?.filiere ||
      "";
    const classNiveau =
      classe?.niveau ||
      user?.classNiveau ||
      user?.classeNiveau ||
      user?.classe?.niveau ||
      user?.classEntity?.niveau ||
      user?.classeDto?.niveau ||
      "";

    return {
      id: classId || className || "unassigned",
      name: className,
      filiere: classFiliere,
      niveau: classNiveau,
    };
  }, [classById]);

  const filteredUsers = useMemo(() => {
    const terms = normalizeSearch(query).split(/\s+/).filter(Boolean);
    if (terms.length === 0) return users;

    return users.filter((user) => {
      const classInfo = getUserClassInfo(user);
      const searchText = normalizeSearch([
        fullName(user),
        user?.firstName,
        user?.lastName,
        user?.prenom,
        user?.nom,
        user?.email,
        roleOf(user),
        isBlockedUser(user) ? "bloqué bloque blocked" : "",
        user?.cne,
        user?.codeApoge,
        user?.codeApogee,
        classInfo.name,
        classInfo.filiere,
        classInfo.niveau,
      ].filter(Boolean).join(" "));

      return terms.every((term) => searchText.includes(term));
    });
  }, [users, query, getUserClassInfo]);

  const teachers = useMemo(
    () => users.filter((user) => roleOf(user).includes("ENSEIGNANT")),
    [users]
  );

  const students = useMemo(
    () => users.filter((user) => roleOf(user).includes("ETUDIANT")),
    [users]
  );

  const userLayers = useMemo(() => {
    const admins = filteredUsers
      .filter((user) => roleOf(user).includes("ADMIN"))
      .sort((a, b) => fullName(a).localeCompare(fullName(b), "fr", { sensitivity: "base" }));

    const teachers = filteredUsers
      .filter((user) => roleOf(user).includes("ENSEIGNANT"))
      .sort((a, b) => fullName(a).localeCompare(fullName(b), "fr", { sensitivity: "base" }));

    const studentGroupsMap = new Map();
    filteredUsers
      .filter((user) => roleOf(user).includes("ETUDIANT"))
      .forEach((student) => {
        const classInfo = getUserClassInfo(student);
        const key = String(classInfo.id || classInfo.name || "unassigned");

        if (!studentGroupsMap.has(key)) {
          studentGroupsMap.set(key, {
            key,
            classInfo,
            students: [],
          });
        }

        studentGroupsMap.get(key).students.push(student);
      });

    const studentGroups = Array.from(studentGroupsMap.values())
      .map((group) => ({
        ...group,
        students: group.students.sort((a, b) =>
          fullName(a).localeCompare(fullName(b), "fr", { sensitivity: "base" })
        ),
      }))
      .sort((a, b) =>
        a.classInfo.name.localeCompare(b.classInfo.name, "fr", { sensitivity: "base" })
      );

    return { admins, teachers, studentGroups };
  }, [filteredUsers, getUserClassInfo]);

  const userById = useMemo(
    () => new Map(users.map((user) => [String(getId(user)), user])),
    [users]
  );

  const subjectById = useMemo(
    () => new Map(subjects.map((subject) => [String(getId(subject)), subject])),
    [subjects]
  );

  const quizById = useMemo(
    () => new Map(quizzes.map((quiz) => [String(getId(quiz)), quiz])),
    [quizzes]
  );

  const findUserByEmail = useCallback((email) => {
    const normalized = String(email || "").trim().toLowerCase();
    if (!normalized) return null;
    return users.find((user) => String(user?.email || "").trim().toLowerCase() === normalized) || null;
  }, [users]);

  const resolveSubjectInfo = useCallback((source, fallbackQuiz = null) => {
    const subjectId =
      source?.subjectId ||
      source?.matiereId ||
      getId(source?.subject) ||
      getId(source?.matiere) ||
      fallbackQuiz?.subjectId ||
      fallbackQuiz?.matiereId ||
      getId(fallbackQuiz?.subject) ||
      getId(fallbackQuiz?.matiere);
    const subject = subjectById.get(String(subjectId)) || source?.subject || source?.matiere || fallbackQuiz?.subject || fallbackQuiz?.matiere;

    return {
      id: subjectId || getId(subject) || "unknown-subject",
      name:
        source?.subjectName ||
        source?.matiereName ||
        subject?.nom ||
        subject?.name ||
        subject?.titre ||
        fallbackQuiz?.subjectName ||
        fallbackQuiz?.matiereName ||
        fallbackQuiz?.theme ||
        source?.quizTheme ||
        "Matière non définie",
      subject,
    };
  }, [subjectById]);

  const resolveClassInfo = useCallback((source, subjectInfo = null, fallbackQuiz = null, student = null) => {
    const subject = subjectInfo?.subject;
    const classId =
      source?.classId ||
      source?.classeId ||
      getId(source?.classe) ||
      getId(source?.classEntity) ||
      fallbackQuiz?.classId ||
      fallbackQuiz?.classeId ||
      getId(fallbackQuiz?.classe) ||
      getId(fallbackQuiz?.classEntity) ||
      subject?.classId ||
      subject?.classeId ||
      getId(subject?.classe) ||
      getId(subject?.classEntity) ||
      student?.classId ||
      student?.classeId ||
      getId(student?.classe);
    const classe =
      classById.get(String(classId)) ||
      source?.classe ||
      source?.classEntity ||
      fallbackQuiz?.classe ||
      fallbackQuiz?.classEntity ||
      subject?.classe ||
      subject?.classEntity ||
      student?.classe;

    return {
      id: classId || getId(classe) || "unknown-class",
      name:
        source?.className ||
        source?.classeName ||
        source?.groupName ||
        classe?.name ||
        fallbackQuiz?.className ||
        fallbackQuiz?.classeName ||
        subject?.className ||
        subject?.classeName ||
        "Classe non définie",
      filiere: classe?.filiere || source?.classFiliere || subject?.classFiliere || "",
      niveau: classe?.niveau || source?.classNiveau || subject?.classNiveau || "",
      classe,
    };
  }, [classById]);

  const resolveTeacherInfo = useCallback((source, subjectInfo = null, classInfo = null, fallbackQuiz = null) => {
    const subject = subjectInfo?.subject;
    const classe = classInfo?.classe;
    const teacherId =
      source?.teacherId ||
      source?.enseignantId ||
      getId(source?.teacher) ||
      getId(source?.enseignant) ||
      fallbackQuiz?.teacherId ||
      fallbackQuiz?.enseignantId ||
      getId(fallbackQuiz?.teacher) ||
      getId(fallbackQuiz?.enseignant) ||
      subject?.teacherId ||
      subject?.enseignantId ||
      getId(subject?.teacher) ||
      getId(subject?.enseignant) ||
      classe?.enseignantId ||
      (Array.isArray(classe?.enseignantIds) ? classe.enseignantIds[0] : "");
    const teacher =
      userById.get(String(teacherId)) ||
      source?.teacher ||
      source?.enseignant ||
      fallbackQuiz?.teacher ||
      fallbackQuiz?.enseignant ||
      subject?.teacher ||
      subject?.enseignant;
    const classTeacherNames = teacherNamesOf(classe);
    const teacherFullName = fullName(teacher);

    return {
      id: teacherId || getId(teacher) || "unknown-teacher",
      name:
        source?.teacherName ||
        source?.enseignantName ||
        source?.enseignantNom ||
        fallbackQuiz?.teacherName ||
        fallbackQuiz?.enseignantName ||
        fallbackQuiz?.enseignantNom ||
        subject?.teacherName ||
        subject?.enseignantName ||
        subject?.enseignantNom ||
        (teacherFullName !== "Utilisateur" ? teacherFullName : "") ||
        classTeacherNames[0] ||
        "Professeur non défini",
      email:
        source?.teacherEmail ||
        source?.enseignantEmail ||
        fallbackQuiz?.teacherEmail ||
        fallbackQuiz?.enseignantEmail ||
        teacher?.email ||
        "",
    };
  }, [userById]);

  const enrichQuiz = useCallback((quiz) => {
    const subjectInfo = resolveSubjectInfo(quiz);
    const classInfo = resolveClassInfo(quiz, subjectInfo, quiz);
    const teacherInfo = resolveTeacherInfo(quiz, subjectInfo, classInfo, quiz);

    return {
      ...quiz,
      id: getId(quiz),
      title: quizTitle(quiz),
      subjectInfo,
      classInfo,
      teacherInfo,
      expired: isExpiredQuiz(quiz),
      draft: isDraftQuiz(quiz),
      published: getQuizStatus(quiz) === "PUBLISHED",
      activePublished: isActivePublishedQuiz(quiz),
      deletable: canDeleteQuiz(quiz),
    };
  }, [resolveClassInfo, resolveSubjectInfo, resolveTeacherInfo]);

  const getSubjectTeacherName = (subject) =>
    subject?.teacherName ||
    subject?.enseignantName ||
    fullName(subject?.teacher || subject?.enseignant);

  const getSubjectTeacherEmail = (subject) =>
    subject?.teacherEmail ||
    subject?.enseignantEmail ||
    subject?.teacher?.email ||
    subject?.enseignant?.email ||
    "";

  const getSubjectClassInfo = useCallback((subject) => {
    const subjectClassId =
      subject?.classId ||
      subject?.classeId ||
      getId(subject?.classe) ||
      getId(subject?.classEntity);

    const subjectClass = classes.find(
      (classe) => String(getId(classe)) === String(subjectClassId)
    );

    return {
      id: subjectClassId,
      name:
        subject?.className ||
        subject?.classeName ||
        subject?.classe?.name ||
        subject?.classEntity?.name ||
        subjectClass?.name ||
        "Classe non définie",
      filiere:
        subject?.classFiliere ||
        subject?.classeFiliere ||
        subject?.classe?.filiere ||
        subject?.classEntity?.filiere ||
        subjectClass?.filiere ||
        "",
      niveau:
        subject?.classNiveau ||
        subject?.classeNiveau ||
        subject?.classe?.niveau ||
        subject?.classEntity?.niveau ||
        subjectClass?.niveau ||
        "",
    };
  }, [classes]);

  const uniqueSubjects = useMemo(() => {
    const seen = new Set();

    return subjects.filter((subject) => {
      const classInfo = getSubjectClassInfo(subject);
      const classKey =
        subject?.classId ||
        subject?.classeId ||
        getId(subject?.classe) ||
        getId(subject?.classEntity) ||
        normalizeSearch([classInfo.name, classInfo.filiere, classInfo.niveau].join(" "));
      const subjectKey = normalizeSearch(subject?.nom || subject?.name || subject?.titre);
      const key = `${subjectKey}-${classKey}`;

      if (!subjectKey || seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }, [subjects, getSubjectClassInfo]);

  const filteredSubjects = useMemo(() => {
    const terms = normalizeSearch(subjectQuery).split(/\s+/).filter(Boolean);

    if (terms.length === 0) return uniqueSubjects;

    return uniqueSubjects.filter((subject) => {
      const teacherName = getSubjectTeacherName(subject);
      const teacherEmail = getSubjectTeacherEmail(subject);
      const classInfo = getSubjectClassInfo(subject);

      const searchText = normalizeSearch([
        subject?.nom,
        subject?.name,
        subject?.titre,
        subject?.description,
        teacherName,
        teacherEmail,
        classInfo.name,
        classInfo.filiere,
        classInfo.niveau,
      ]
        .filter(Boolean)
        .join(" "));

      return terms.every((term) => searchText.includes(term));
    });
  }, [uniqueSubjects, subjectQuery, getSubjectClassInfo]);

  const subjectsByTeacher = useMemo(() => {
    const groups = new Map();

    filteredSubjects.forEach((subject) => {
      const teacherId =
        subject?.teacherId ||
        subject?.enseignantId ||
        getId(subject?.teacher) ||
        getId(subject?.enseignant) ||
        "unknown";

      const teacherName = getSubjectTeacherName(subject);
      const teacherEmail = getSubjectTeacherEmail(subject);
      const key = `${teacherId}-${teacherName}`;

      if (!groups.has(key)) {
        groups.set(key, {
          key,
          teacherName:
            teacherName && teacherName !== "Utilisateur"
              ? teacherName
              : "Enseignant non affecté",
          teacherEmail,
          subjects: [],
        });
      }

      groups.get(key).subjects.push(subject);
    });

    return Array.from(groups.values()).sort((a, b) =>
      a.teacherName.localeCompare(b.teacherName, "fr", { sensitivity: "base" })
    );
  }, [filteredSubjects]);

  const summary = useMemo(() => {
    const published = Number(globalStats?.totalQuizPublies ?? globalStats?.publishedQuizzes ?? quizzes.filter(isPublished).length);
    const students = Number(globalStats?.totalEtudiants ?? globalStats?.students ?? users.filter((user) => roleOf(user).includes("ETUDIANT")).length);
    const teachers = Number(globalStats?.totalEnseignants ?? globalStats?.teachers ?? users.filter((user) => roleOf(user).includes("ENSEIGNANT")).length);
    const admins = users.filter((user) => roleOf(user).includes("ADMIN")).length;
    const submittedStats = Object.values(quizStats).filter(Boolean);
    const avg =
      submittedStats.length > 0
        ? submittedStats.reduce((total, stat) => total + Number(stat.averageScore || stat.average || stat.moyenneScore || 0), 0) /
          submittedStats.length
        : Number(globalStats?.averageScore || globalStats?.moyenneGlobale || globalStats?.tauxReussiteGlobal || 0);

    return {
      users: Number(globalStats?.totalUtilisateurs ?? globalStats?.totalUsers ?? users.length),
      students,
      teachers,
      admins,
      classes: classes.length,
      quizzes: Number(globalStats?.totalQuiz ?? globalStats?.totalQuizzes ?? quizzes.length),
      published,
      average: avg,
    };
  }, [users, classes, quizzes, quizStats, globalStats]);

  const topStudents = useMemo(() => {
    return Object.entries(rankings)
      .flatMap(([quizId, rows]) => {
        const quiz = quizzes.find((item) => String(getId(item)) === quizId);
        return (Array.isArray(rows) ? rows : []).map((row) => ({
          name: row.studentName || row.name || row.email || "Étudiant",
          quiz: quizTitle(quiz),
          score: Number(row.scorePercentage || row.score || row.percentage || 0),
        }));
      })
      .sort((a, b) => b.score - a.score)
      .slice(0, 5);
  }, [rankings, quizzes]);

  const difficultQuizzes = useMemo(() => {
    return quizzes
      .filter(isPublished)
      .map((quiz) => ({
        quiz,
        average: Number(quizStats[String(getId(quiz))]?.averageScore || 0),
      }))
      .sort((a, b) => a.average - b.average)
      .slice(0, 5);
  }, [quizzes, quizStats]);

  const quizCounts = useMemo(
    () => ({
      ALL: quizzes.length,
      DRAFT: quizzes.filter(isDraftQuiz).length,
      PUBLISHED: quizzes.filter(isActivePublishedQuiz).length,
      EXPIRED: quizzes.filter(isExpiredQuiz).length,
    }),
    [quizzes]
  );

  const enrichedQuizzes = useMemo(
    () => quizzes.map((quiz) => enrichQuiz(quiz)),
    [quizzes, enrichQuiz]
  );

  const filteredAdminQuizzes = useMemo(() => {
    const terms = normalizeSearch(quizQuery).split(/\s+/).filter(Boolean);

    return enrichedQuizzes.filter((quiz) => {
      const matchesStatus =
        quizStatusFilter === "ALL" ||
        (quizStatusFilter === "DRAFT" && quiz.draft) ||
        (quizStatusFilter === "PUBLISHED" && quiz.activePublished) ||
        (quizStatusFilter === "EXPIRED" && quiz.expired);

      if (!matchesStatus) return false;
      if (terms.length === 0) return true;

      const searchText = normalizeSearch([
        quiz.title,
        quiz.theme,
        quiz.status,
        quiz.teacherInfo.name,
        quiz.teacherInfo.email,
        quiz.classInfo.name,
        quiz.classInfo.filiere,
        quiz.classInfo.niveau,
        quiz.subjectInfo.name,
      ].filter(Boolean).join(" "));

      return terms.every((term) => searchText.includes(term));
    });
  }, [enrichedQuizzes, quizQuery, quizStatusFilter]);

  const quizHierarchy = useMemo(() => {
    const teacherMap = new Map();

    filteredAdminQuizzes.forEach((quiz) => {
      const teacherKey = String(quiz.teacherInfo.id || quiz.teacherInfo.name);
      if (!teacherMap.has(teacherKey)) {
        teacherMap.set(teacherKey, {
          key: teacherKey,
          teacher: quiz.teacherInfo,
          count: 0,
          classes: new Map(),
        });
      }

      const teacherGroup = teacherMap.get(teacherKey);
      teacherGroup.count += 1;

      const classKey = String(quiz.classInfo.id || quiz.classInfo.name);
      if (!teacherGroup.classes.has(classKey)) {
        teacherGroup.classes.set(classKey, {
          key: classKey,
          classInfo: quiz.classInfo,
          count: 0,
          subjects: new Map(),
        });
      }

      const classGroup = teacherGroup.classes.get(classKey);
      classGroup.count += 1;

      const subjectKey = String(quiz.subjectInfo.id || quiz.subjectInfo.name);
      if (!classGroup.subjects.has(subjectKey)) {
        classGroup.subjects.set(subjectKey, {
          key: subjectKey,
          subjectInfo: quiz.subjectInfo,
          quizzes: [],
        });
      }

      classGroup.subjects.get(subjectKey).quizzes.push(quiz);
    });

    return Array.from(teacherMap.values())
      .sort((a, b) => a.teacher.name.localeCompare(b.teacher.name, "fr", { sensitivity: "base" }))
      .map((teacherGroup) => ({
        ...teacherGroup,
        classes: Array.from(teacherGroup.classes.values())
          .sort((a, b) => a.classInfo.name.localeCompare(b.classInfo.name, "fr", { sensitivity: "base" }))
          .map((classGroup) => ({
            ...classGroup,
            subjects: Array.from(classGroup.subjects.values())
              .sort((a, b) => a.subjectInfo.name.localeCompare(b.subjectInfo.name, "fr", { sensitivity: "base" }))
              .map((subjectGroup) => ({
                ...subjectGroup,
                quizzes: subjectGroup.quizzes.sort((a, b) => a.title.localeCompare(b.title, "fr", { sensitivity: "base" })),
              })),
          })),
      }));
  }, [filteredAdminQuizzes]);

  const adminResultRows = useMemo(() => {
    const rows = Object.entries(quizResults).flatMap(([quizId, resultRows]) => {
      const quiz = quizById.get(String(quizId));
      return asArray(resultRows).map((row) => {
        const studentId =
          row?.studentId ||
          row?.etudiantId ||
          getId(row?.student) ||
          getId(row?.etudiant);
        const student =
          userById.get(String(studentId)) ||
          findUserByEmail(row?.email || row?.studentEmail || row?.student?.email || row?.etudiant?.email);
        const subjectInfo = resolveSubjectInfo(row, quiz);
        const classInfo = resolveClassInfo(row, subjectInfo, quiz, student);
        const teacherInfo = resolveTeacherInfo(row, subjectInfo, classInfo, quiz);
        const firstName = getStudentFirstName(row, student);
        const lastName = getStudentLastName(row, student);
        const studentName = [firstName, lastName].filter(Boolean).join(" ") || row?.studentName || fullName(student);

        return {
          id: `${quizId}-${studentId || getStudentEmail(row, student)}-${getSubmissionDate(row) || ""}`,
          quizId,
          quizTitle: row?.quizTitle || quizTitle(quiz),
          teacherInfo,
          classInfo,
          subjectInfo,
          firstName,
          lastName,
          studentName,
          email: getStudentEmail(row, student),
          cne: getStudentCne(row, student),
          codeApogee: getStudentCodeApogee(row, student),
          noteSur20: getNoteSur20(row),
          scorePercent: getScorePercent(row),
          submittedAt: getSubmissionDate(row),
        };
      });
    });

    const rankGroups = new Map();
    rows.forEach((row) => {
      const key = `${row.teacherInfo.id}-${row.classInfo.id}-${row.subjectInfo.id}-${row.quizId}`;
      if (!rankGroups.has(key)) rankGroups.set(key, []);
      rankGroups.get(key).push(row);
    });

    rankGroups.forEach((group) => {
      group
        .sort((a, b) => b.noteSur20 - a.noteSur20)
        .forEach((row, index) => {
          row.rank = index + 1;
        });
    });

    return rows;
  }, [quizResults, quizById, userById, findUserByEmail, resolveSubjectInfo, resolveClassInfo, resolveTeacherInfo]);

  const resultHierarchy = useMemo(() => {
    const terms = normalizeSearch(resultsQuery).split(/\s+/).filter(Boolean);
    const filteredRows = adminResultRows.filter((row) => {
      if (terms.length === 0) return true;
      const searchText = normalizeSearch([
        row.teacherInfo.name,
        row.teacherInfo.email,
        row.classInfo.name,
        row.classInfo.filiere,
        row.subjectInfo.name,
        row.quizTitle,
        row.studentName,
        row.firstName,
        row.lastName,
        row.email,
        row.cne,
        row.codeApogee,
      ].filter(Boolean).join(" "));
      return terms.every((term) => searchText.includes(term));
    });

    const teacherMap = new Map();
    filteredRows.forEach((row) => {
      const teacherKey = String(row.teacherInfo.id || row.teacherInfo.name);
      if (!teacherMap.has(teacherKey)) {
        teacherMap.set(teacherKey, {
          key: teacherKey,
          teacher: row.teacherInfo,
          rows: [],
          classes: new Map(),
        });
      }
      const teacherGroup = teacherMap.get(teacherKey);
      teacherGroup.rows.push(row);

      const classKey = String(row.classInfo.id || row.classInfo.name);
      if (!teacherGroup.classes.has(classKey)) {
        teacherGroup.classes.set(classKey, {
          key: classKey,
          classInfo: row.classInfo,
          rows: [],
          subjects: new Map(),
        });
      }
      const classGroup = teacherGroup.classes.get(classKey);
      classGroup.rows.push(row);

      const subjectKey = String(row.subjectInfo.id || row.subjectInfo.name);
      if (!classGroup.subjects.has(subjectKey)) {
        classGroup.subjects.set(subjectKey, {
          key: subjectKey,
          subjectInfo: row.subjectInfo,
          rows: [],
        });
      }
      classGroup.subjects.get(subjectKey).rows.push(row);
    });

    return Array.from(teacherMap.values())
      .sort((a, b) => a.teacher.name.localeCompare(b.teacher.name, "fr", { sensitivity: "base" }))
      .map((teacherGroup) => ({
        ...teacherGroup,
        classes: Array.from(teacherGroup.classes.values())
          .sort((a, b) => a.classInfo.name.localeCompare(b.classInfo.name, "fr", { sensitivity: "base" }))
          .map((classGroup) => ({
            ...classGroup,
            subjects: Array.from(classGroup.subjects.values())
              .sort((a, b) => a.subjectInfo.name.localeCompare(b.subjectInfo.name, "fr", { sensitivity: "base" }))
              .map((subjectGroup) => ({
                ...subjectGroup,
                rows: subjectGroup.rows.sort((a, b) => b.noteSur20 - a.noteSur20),
              })),
          })),
      }));
  }, [adminResultRows, resultsQuery]);

  const renderUserRow = (user) => {
    const userId = getId(user);
    const role = roleOf(user);
    const classInfo = role.includes("ETUDIANT") ? getUserClassInfo(user) : null;
    const actionFeedback = userActionFeedback?.userId === String(userId)
      ? userActionFeedback
      : null;

    return (
      <div className={styles.tableRow} key={userId}>
        <strong>{fullName(user)}</strong>
        <span>{user.email || "-"}</span>
        <div className={styles.roleCell}>
          <em className={isBlockedUser(user) ? styles.blockedPill : ""}>
            {isBlockedUser(user) ? "BLOQUÉ" : role}
          </em>
          {classInfo && (
            <small>
              {classInfo.name}
              {classInfo.filiere ? ` · ${classInfo.filiere}` : ""}
              {classInfo.niveau ? ` · ${classInfo.niveau}` : ""}
            </small>
          )}
          {role.includes("ETUDIANT") && (
            <small>
              CNE: {user?.cne || "-"} · Apogée: {user?.codeApoge || user?.codeApogee || "-"}
            </small>
          )}
        </div>
        <div className={styles.actions}>
          <button type="button" onClick={() => editUser(user)}>Modifier</button>
          <button
            type="button"
            disabled={working === `block-user-${userId}`}
            onClick={() => toggleBlockUser(user)}
          >
            {isBlockedUser(user) ? "Débloquer" : "Bloquer"}
          </button>
          <button
            type="button"
            disabled={working === `reset-user-${userId}`}
            onClick={() => resetUserPassword(user)}
          >
            Reset
          </button>
          {String(pendingDeleteUserId) === String(userId) ? (
            <div className={styles.inlineConfirm}>
              <span>Supprimer ?</span>
              <button
                type="button"
                className={styles.confirmBtn}
                disabled={working === `delete-user-${userId}`}
                onClick={() => deleteUser(userId)}
              >
                Oui
              </button>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() => setPendingDeleteUserId("")}
              >
                Non
              </button>
            </div>
          ) : (
            <button
              type="button"
              className={styles.dangerBtn}
              disabled={working === `delete-user-${userId}`}
              onClick={() => setPendingDeleteUserId(userId)}
            >
              <Trash2 size={16} />
            </button>
          )}
          {actionFeedback && (
            <div
              className={`${styles.rowActionNotice} ${actionFeedback.type === "error" ? styles.rowActionError : styles.rowActionSuccess}`}
              title={actionFeedback.message}
            >
              {userActionMessage(actionFeedback)}
            </div>
          )}
        </div>
      </div>
    );
  };

  if (loading) return <div className={styles.loading}>Chargement de la console admin...</div>;

  return (
    <div className={styles.page}>
      <ConfirmDialog
        open={Boolean(confirmDialog)}
        title={confirmDialog?.title}
        message={confirmDialog?.message}
        confirmLabel={confirmDialog?.confirmLabel}
        cancelLabel={confirmDialog?.cancelLabel}
        tone={confirmDialog?.tone}
        onCancel={() => closeConfirmDialog(false)}
        onConfirm={() => closeConfirmDialog(true)}
      />

      <header className={styles.hero}>
        <span className={styles.badge}>
          <ShieldCheck size={16} />
          Administration plateforme
        </span>
        <h1>{titleFor(section)}</h1>
        <p>{descriptionFor(section)}</p>
      </header>

      {(notice || error) && section !== "users" && (
        <div className={error ? styles.error : styles.success}>{error || notice}</div>
      )}

      {section === "dashboard" && (
        <>
          <StatsGrid summary={summary} />
          <section className={styles.dashboardOverview}>
            <article>
              <span>Organisation pédagogique</span>
              <strong>{classes.length} classes</strong>
              <p>{subjects.length} matières, {teachers.length} professeurs, {students.length} étudiants.</p>
            </article>
            <article>
              <span>État des quiz</span>
              <strong>{quizCounts.PUBLISHED} publiés actifs</strong>
              <p>{quizCounts.DRAFT} brouillons, {quizCounts.EXPIRED} expirés, {quizCounts.ALL} au total.</p>
            </article>
            <article>
              <span>Résultats collectés</span>
              <strong>{adminResultRows.length} soumissions</strong>
              <p>Notes, classements et dates de soumission disponibles pour l'analyse.</p>
            </article>
          </section>
          <section className={styles.dashboardGrid}>
            <Panel title="Activité récente" icon={RefreshCw}>
              <div className={styles.timeline}>
                {[...enrichedQuizzes].slice(0, 5).map((quiz) => (
                  <div key={getId(quiz)}>
                    <strong>{quizTitle(quiz)}</strong>
                    <span>
                      {quiz.teacherInfo.name} · {quiz.classInfo.name} · {quiz.subjectInfo.name}
                    </span>
                  </div>
                ))}
                {quizzes.length === 0 && <Empty text="Aucune activité quiz." />}
              </div>
            </Panel>
            <Panel title="Top étudiants" icon={Trophy}>
              <TopStudents rows={topStudents} />
            </Panel>
          </section>
        </>
      )}

      {section === "users" && (
        <section className={styles.twoColumns}>
          <div ref={userFormCardRef}>
          <Panel title={editingUserId ? "Modifier un compte" : "Créer un compte"} icon={UserPlus}>
            {(userFormNotice || userFormError) && (
              <div className={userFormError ? styles.inlineError : styles.inlineSuccess}>
                {userFormError || userFormNotice}
              </div>
            )}
            <form className={styles.form} onSubmit={createUser}>
              {!editingUserId && (
                <div className={styles.modeSwitch}>
                  <button
                    type="button"
                    className={accountMode === "single" ? styles.modeActive : ""}
                    onClick={() => {
                      setAccountMode("single");
                      setSelectedImportFile(null);
                    }}
                  >
                    Compte individuel
                  </button>
                  <button
                    type="button"
                    className={`${accountMode === "excel" ? styles.modeActive : ""} ${styles.excelModeButton}`}
                    onClick={() => setAccountMode("excel")}
                  >
                    Import Excel
                  </button>
                </div>
              )}
              {(accountMode === "single" || editingUserId) && (
                <>
                  <input placeholder="Prénom" value={userForm.firstName} onChange={(e) => setUserForm({ ...userForm, firstName: e.target.value })} />
                  <input placeholder="Nom" value={userForm.lastName} onChange={(e) => setUserForm({ ...userForm, lastName: e.target.value })} />
                  <input placeholder="Email" type="email" value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} />
                  {userForm.role === "ETUDIANT" && (
                    <>
                      <input required placeholder="CNE" value={userForm.cne} onChange={(e) => setUserForm({ ...userForm, cne: e.target.value })} />
                      <input required placeholder="Code Apogée" value={userForm.codeApoge} onChange={(e) => setUserForm({ ...userForm, codeApoge: e.target.value })} />
                    </>
                  )}
                </>
              )}
              <select value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}>
                <option value="ETUDIANT">Étudiant</option>
                <option value="ENSEIGNANT">Enseignant</option>
                <option value="ADMIN">Admin</option>
              </select>
              {userForm.role === "ETUDIANT" && !editingUserId && (
                <>
                  {userForm.classId && (
                    <p className={styles.selectedClassName}>
                      Classe sélectionnée :{" "}
                      <strong>
                        {location.state?.className ||
                          classes.find((classe) => String(getId(classe)) === String(userForm.classId))?.name ||
                          "Classe"}
                      </strong>
                    </p>
                  )}
                  <select
                    value={userForm.classId}
                    onChange={(e) => setUserForm({ ...userForm, classId: e.target.value })}
                    required
                  >
                    <option value="">Choisir la classe de l'étudiant</option>
                    {classes.map((classe) => (
                      <option key={getId(classe)} value={getId(classe)}>
                        {classe.name} {classe.filiere ? `- ${classe.filiere}` : ""}
                      </option>
                    ))}
                  </select>
                </>
              )}
              {!editingUserId && accountMode === "excel" && (
                <div className={styles.importActions}>
                  <label
                    className={styles.fileImport}
                  >
                    <span>{selectedImportFile ? selectedImportFile.name : "Choisir un fichier Excel"}</span>
                    <input
                      type="file"
                      accept=".xlsx,.xls"
                      onChange={(e) => {
                        setSelectedImportFile(e.target.files?.[0] || null);
                        e.target.value = "";
                      }}
                    />
                  </label>
                  <button
                    type="button"
                    className={styles.importCreateBtn}
                    disabled={working.startsWith("import-")}
                    onClick={() => importAccountsExcel(selectedImportFile)}
                  >
                    {working.startsWith("import-") ? "Création en cours..." : "Créer les comptes importés"}
                  </button>
                </div>
              )}
              <p className={styles.formHint}>
                {!editingUserId && accountMode === "excel"
                  ? "Fichier Excel attendu : étudiants = Prénom, Nom, Email, CNE, Code Apogée. Profs/Admins = Prénom, Nom, Email."
                  : "Le mot de passe temporaire est généré automatiquement et envoyé par email."}
              </p>
              {(accountMode === "single" || editingUserId) && (
                <button disabled={working === "create-user"}>
                  {editingUserId ? "Enregistrer les modifications" : "Créer le compte"}
                </button>
              )}
              {editingUserId && (
                <button
                  type="button"
                  className={styles.secondaryBtn}
                  onClick={() => {
                    setEditingUserId(null);
                    setUserForm(emptyUser);
                  }}
                >
                  Annuler
                </button>
              )}
            </form>
          </Panel>
          </div>
          <Panel title="Gestion des utilisateurs" icon={Users} wide>
            {(userListError || error) && (
              <div className={userListError || error ? styles.inlineError : styles.inlineSuccess}>
                {userListError || error}
              </div>
            )}
            <SearchBar
              value={query}
              onChange={setQuery}
              placeholder="Rechercher nom, email, rôle, classe, filière, CNE ou Code Apogée..."
            />
            <div className={styles.userLayers}>
              <UserGroup title="Admins" count={userLayers.admins.length}>
                {userLayers.admins.map(renderUserRow)}
              </UserGroup>

              <UserGroup title="Enseignants" count={userLayers.teachers.length}>
                {userLayers.teachers.map(renderUserRow)}
              </UserGroup>

              <section className={styles.userLayer}>
                <div className={styles.userLayerHeader}>
                  <div>
                    <span>Étudiants</span>
                    <strong>{userLayers.studentGroups.reduce((total, group) => total + group.students.length, 0)} compte(s)</strong>
                  </div>
                  <em>{userLayers.studentGroups.length} classe(s)</em>
                </div>
                <div className={styles.classStudentGroups}>
                  {userLayers.studentGroups.map((group) => (
                    <div className={styles.classStudentGroup} key={group.key}>
                      <div className={styles.classStudentHeader}>
                        <div>
                          <strong>{group.classInfo.name}</strong>
                          <span>
                            {[group.classInfo.filiere, group.classInfo.niveau].filter(Boolean).join(" · ") || "Informations de classe non définies"}
                          </span>
                        </div>
                        <em>{group.students.length} étudiant(s)</em>
                      </div>
                      <div className={styles.table}>
                        <div className={styles.tableHead}>
                          <span>Utilisateur</span><span>Email</span><span>Rôle / Classe</span><span>Actions</span>
                        </div>
                        {group.students.map(renderUserRow)}
                      </div>
                    </div>
                  ))}
                  {userLayers.studentGroups.length === 0 && (
                    <Empty text="Aucun étudiant ne correspond à la recherche." />
                  )}
                </div>
              </section>
            </div>
          </Panel>
        </section>
      )}

      {section === "classes" && (
        <section className={styles.twoColumns}>
          <div ref={classFormCardRef}>
          <Panel title={editingClassId ? "Modifier une classe" : "Créer une classe"} icon={GraduationCap}>
            <form className={styles.form} onSubmit={createClass}>
              <input placeholder="Nom de classe : GI1" value={classForm.name} onChange={(e) => setClassForm({ ...classForm, name: e.target.value })} />
              <input placeholder="Filière : Génie informatique" value={classForm.filiere} onChange={(e) => setClassForm({ ...classForm, filiere: e.target.value })} />
              <input placeholder="Niveau : S2, Master..." value={classForm.niveau} onChange={(e) => setClassForm({ ...classForm, niveau: e.target.value })} />
              <TeacherPicker
                teachers={teachers}
                selectedIds={classForm.teacherIds}
                onChange={(teacherIds) => setClassForm({ ...classForm, teacherIds })}
              />
              <button disabled={working === "create-class"}>
                {editingClassId ? "Enregistrer la classe" : "Créer la classe"}
              </button>
              {editingClassId && (
                <button
                  type="button"
                  className={styles.secondaryBtn}
                  onClick={() => {
                    setEditingClassId(null);
                    setClassForm(emptyClass);
                  }}
                >
                  Annuler
                </button>
              )}
            </form>
          </Panel>
          </div>
          <Panel title="Classes existantes" icon={GraduationCap} wide>
            <div className={styles.cardGrid}>
              {classes.map((classe) => (
                <article className={styles.miniCard} key={getId(classe)}>
                  <strong>{classe.name}</strong>
                  <span>{classe.filiere || "Filière non définie"} · {classe.niveau || "Niveau non défini"}</span>
                  <TeacherList names={teacherNamesOf(classe)} />
                  <p>{classe.studentCount ?? 0} étudiant(s)</p>
                  <div className={styles.actions}>
                    <button onClick={() => goToStudentsForClass(classe)}>
                      Affecter
                    </button>
                    <button onClick={() => editClass(classe)}>Modifier</button>
                    <button className={styles.dangerBtn} onClick={() => deleteClass(getId(classe))}><Trash2 size={16} /></button>
                  </div>
                </article>
              ))}
            </div>
          </Panel>
        </section>
      )}

      {section === "subjects" && (
        <section className={styles.twoColumns}>
          <div ref={subjectFormCardRef}>
          <Panel title={editingSubjectId ? "Modifier une matière" : "Créer une matière"} icon={BookMarked}>
            <form className={styles.form} onSubmit={createSubject}>
              <input
                placeholder="Nom de la matière : Java, Réseaux..."
                value={subjectForm.nom}
                onChange={(e) => setSubjectForm({ ...subjectForm, nom: e.target.value })}
              />

              <select
                value={subjectForm.teacherId}
                onChange={(e) => setSubjectForm({ ...subjectForm, teacherId: e.target.value })}
                required
              >
                <option value="">Choisir un enseignant</option>
                {teachers.map((teacher) => (
                  <option key={getId(teacher)} value={getId(teacher)}>
                    {fullName(teacher)} {teacher.email ? `- ${teacher.email}` : ""}
                  </option>
                ))}
              </select>

              <select
                value={subjectForm.classId}
                onChange={(e) => setSubjectForm({ ...subjectForm, classId: e.target.value })}
                required
              >
                <option value="">Choisir une classe</option>
                {classes.map((classe) => (
                  <option key={getId(classe)} value={getId(classe)}>
                    {classe.name} {classe.filiere ? `- ${classe.filiere}` : ""} {classe.niveau ? `- ${classe.niveau}` : ""}
                  </option>
                ))}
              </select>

              <p className={styles.formHint}>
                La même matière peut être créée pour un autre enseignant ou une autre classe.
              </p>

              <button disabled={working === "create-subject"}>
                {editingSubjectId ? "Enregistrer la matière" : "Créer la matière"}
              </button>

              {editingSubjectId && (
                <button
                  type="button"
                  className={styles.secondaryBtn}
                  onClick={() => {
                    setEditingSubjectId(null);
                    setSubjectForm(emptySubject);
                  }}
                >
                  Annuler
                </button>
              )}
            </form>
          </Panel>

          </div>
          <Panel title="Matières existantes" icon={BookMarked} wide>
            <SearchBar
              value={subjectQuery}
              onChange={setSubjectQuery}
              placeholder="Rechercher par matière, professeur ou groupe..."
            />

            <p className={styles.formHint}>
              {filteredSubjects.length} matière(s) trouvée(s), classée(s) par enseignant.
            </p>

            {subjectsByTeacher.length === 0 && (
              <Empty
                text={
                  subjectQuery.trim()
                    ? "Aucune matière ne correspond à votre recherche."
                    : "Aucune matière créée pour le moment."
                }
              />
            )}

            {subjectsByTeacher.map((group) => (
              <div className={styles.subjectTeacherGroup} key={group.key}>
                <div className={styles.subjectTeacherHeader}>
                  <div>
                    <strong>{group.teacherName}</strong>
                    {group.teacherEmail && <span>{group.teacherEmail}</span>}
                  </div>
                  <em>{group.subjects.length} matière(s)</em>
                </div>

                <div className={styles.cardGrid}>
                  {group.subjects.map((subject) => {
                    const classInfo = getSubjectClassInfo(subject);

                    return (
                      <article className={styles.miniCard} key={getSubjectId(subject) || `${subject.nom}-${subject.classId}-${subject.teacherId}`}>
                        <strong>{subject.nom || subject.name || subject.titre}</strong>
                        {subject.description?.trim() && <span>{subject.description}</span>}

                        <p>
                          <b>Enseignant :</b> {group.teacherName}
                        </p>

                        <p>
                          <b>Classe :</b> {classInfo.name}
                          {classInfo.filiere ? ` - ${classInfo.filiere}` : ""}
                          {classInfo.niveau ? ` - ${classInfo.niveau}` : ""}
                        </p>

                        <div className={styles.actions}>
                          <button onClick={() => editSubject(subject)}>Modifier</button>

                          <button
                            className={styles.dangerBtn}
                            disabled={working === `delete-subject-${getSubjectId(subject)}`}
                            onClick={() => deleteSubject(subject)}
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              </div>
            ))}
          </Panel>
        </section>
      )}

      {section === "quizzes" && (
        <section className={styles.adminSectionStack}>
          <Panel title="Organisation des quiz" icon={BookOpen} wide>
            <StatusFilterBar
              active={quizStatusFilter}
              onChange={setQuizStatusFilter}
              counts={quizCounts}
            />

            <SearchBar
              value={quizQuery}
              onChange={setQuizQuery}
              placeholder="Rechercher par professeur, classe, matière ou quiz..."
            />

            {quizHierarchy.length === 0 ? (
              <Empty text="Aucun quiz ne correspond aux filtres." />
            ) : (
              <div className={styles.quizHierarchy}>
                {quizHierarchy.map((teacherGroup) => (
                  <section className={styles.teacherBlock} key={teacherGroup.key}>
                    <div className={styles.teacherBlockHeader}>
                      <div>
                        <span>Professeur</span>
                        <h2>{teacherGroup.teacher.name}</h2>
                        {teacherGroup.teacher.email && <p>{teacherGroup.teacher.email}</p>}
                      </div>
                      <strong>{teacherGroup.count} quiz</strong>
                    </div>

                    {teacherGroup.classes.map((classGroup) => (
                      <div className={styles.quizClassBlock} key={classGroup.key}>
                        <div className={styles.quizClassHeader}>
                          <div>
                            <h2>{classGroup.classInfo.name}</h2>
                            <p>
                              {classGroup.classInfo.filiere || "Filière non définie"}
                              {classGroup.classInfo.niveau ? ` · ${classGroup.classInfo.niveau}` : ""}
                            </p>
                          </div>
                          <span>{classGroup.count} quiz</span>
                        </div>

                        {classGroup.subjects.map((subjectGroup) => (
                          <div className={styles.quizSubjectBlock} key={subjectGroup.key}>
                            <div className={styles.quizSubjectHeader}>
                              <div>
                                <h3>{subjectGroup.subjectInfo.name}</h3>
                                <p>{subjectGroup.quizzes.length} quiz dans cette matière</p>
                              </div>
                              <span>{subjectGroup.quizzes.length}</span>
                            </div>

                            <div className={styles.quizCardsGrid}>
                              {subjectGroup.quizzes.map((quiz) => {
                                const statusLabel = quiz.expired
                                  ? "Expiré"
                                  : quiz.activePublished
                                  ? "Publié"
                                  : "Brouillon";
                                const statusClass = quiz.expired
                                  ? styles.quizExpired
                                  : quiz.activePublished
                                  ? styles.quizPublished
                                  : styles.quizDraft;

                                return (
                                  <article key={quiz.id} className={styles.quizCard}>
                                    <div className={styles.quizCardTop}>
                                      <strong>{quiz.title}</strong>
                                      <em className={statusClass}>{statusLabel}</em>
                                    </div>

                                    <p className={styles.quizTheme}>{quiz.theme || "Sans thème"}</p>
                                    <div className={styles.quizMetaGrid}>
                                      <span>Début : {formatDate(quiz.availableFrom)}</span>
                                      <span>Expiration : {formatDate(getQuizAvailableUntil(quiz))}</span>
                                      <span>Créé : {formatDate(getQuizCreatedAt(quiz))}</span>
                                    </div>

                                    {quiz.deletable ? (
                                      <div className={styles.actions}>
                                        <button
                                          type="button"
                                          className={styles.deleteExpiredBtn}
                                          disabled={working === `delete-quiz-${quiz.id}`}
                                          onClick={() => deleteQuiz(quiz.id)}
                                        >
                                          <Trash2 size={16} />
                                          Supprimer
                                        </button>
                                      </div>
                                    ) : quiz.published ? (
                                      <span className={styles.lockedAction}>
                                        Suppression possible après 5 mois de publication.
                                      </span>
                                    ) : null}
                                  </article>
                                );
                              })}
                            </div>
                          </div>
                        ))}
                      </div>
                    ))}
                  </section>
                ))}
              </div>
            )}
          </Panel>
        </section>
      )}

      {section === "results" && (
        <Panel title="Résultats des quiz" icon={BarChart3} wide>
          <SearchBar
            value={resultsQuery}
            onChange={setResultsQuery}
            placeholder="Filtrer par professeur, classe, matière, étudiant, CNE, Code Apogée ou email..."
          />

          {resultHierarchy.length === 0 ? (
            <Empty text="Aucun résultat ne correspond à votre recherche." />
          ) : (
            <div className={styles.resultsHierarchy}>
              {resultHierarchy.map((teacherGroup) => (
                <section className={styles.teacherBlock} key={teacherGroup.key}>
                  <div className={styles.teacherBlockHeader}>
                    <div>
                      <span>Professeur</span>
                      <h2>{teacherGroup.teacher.name}</h2>
                      {teacherGroup.teacher.email && <p>{teacherGroup.teacher.email}</p>}
                    </div>
                    <strong>{teacherGroup.rows.length} soumission(s)</strong>
                  </div>

                  {teacherGroup.classes.map((classGroup) => (
                    <div className={styles.resultsClassBlock} key={classGroup.key}>
                      <div className={styles.quizClassHeader}>
                        <div>
                          <h2>{classGroup.classInfo.name}</h2>
                          <p>
                            {classGroup.classInfo.filiere || "Filière non définie"}
                            {classGroup.classInfo.niveau ? ` · ${classGroup.classInfo.niveau}` : ""}
                          </p>
                        </div>
                        <span>{classGroup.rows.length} étudiant(s)</span>
                      </div>

                      {classGroup.subjects.map((subjectGroup) => (
                        <div className={styles.resultSubjectBlock} key={subjectGroup.key}>
                          <div className={styles.quizSubjectHeader}>
                            <div>
                              <h3>{subjectGroup.subjectInfo.name}</h3>
                              <p>Tableau des étudiants, notes et classement.</p>
                            </div>
                            <span>{subjectGroup.rows.length}</span>
                          </div>

                          <div className={styles.resultTable}>
                            <table>
                              <thead>
                                <tr>
                                  <th>Classement</th>
                                  <th>Nom</th>
                                  <th>Prénom</th>
                                  <th>Code Apogée</th>
                                  <th>CNE</th>
                                  <th>Email</th>
                                  <th>Quiz</th>
                                  <th>Note</th>
                                  <th>Date de soumission</th>
                                </tr>
                              </thead>
                              <tbody>
                                {subjectGroup.rows.map((row) => (
                                  <tr key={row.id}>
                                    <td>#{row.rank || "-"}</td>
                                    <td>{row.lastName || "-"}</td>
                                    <td>{row.firstName || "-"}</td>
                                    <td>{row.codeApogee}</td>
                                    <td>{row.cne}</td>
                                    <td>{row.email}</td>
                                    <td>{row.quizTitle}</td>
                                    <td>
                                      <strong>{row.noteSur20.toFixed(2)}/20</strong>
                                    </td>
                                    <td>{formatDate(row.submittedAt)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      ))}
                    </div>
                  ))}
                </section>
              ))}
            </div>
          )}
        </Panel>
      )}

     {section === "__legacy_quizzes" && (
  <Panel title="Organisation des quiz" icon={BookOpen} wide>

    <SearchBar
      value={query}
      onChange={setQuery}
      placeholder="Rechercher une classe, matière, quiz ou professeur..."
    />

    <div className={styles.quizHierarchy}>

      {classes.map((classe) => {

        const classSubjects = subjects.filter((subject) => {
          const subjectClassId =
            subject.classId ||
            subject.classeId ||
            getId(subject.classe) ||
            getId(subject.classEntity);

          return String(subjectClassId) === String(getId(classe));
        });

        return (
          <div key={getId(classe)} className={styles.quizClassBlock}>

            <div className={styles.quizClassHeader}>
              <div>
                <h2>{classe.name}</h2>

                <p>
                  {classe.filiere || "Filière non définie"}
                  {classe.niveau ? ` • ${classe.niveau}` : ""}
                </p>
              </div>

              <span>
                {classSubjects.length} matière(s)
              </span>
            </div>

            {classSubjects.length === 0 && (
              <Empty text="Aucune matière dans cette classe." />
            )}

            {classSubjects.map((subject) => {

              const teacherName =
                subject.teacherName ||
                subject.enseignantName ||
                fullName(subject.teacher || subject.enseignant);

              const subjectQuizzes = quizzes.filter((quiz) => {

                const quizSubjectId =
                  quiz.subjectId ||
                  quiz.matiereId ||
                  getId(quiz.subject) ||
                  getId(quiz.matiere);

                return String(quizSubjectId) === String(getId(subject));
              });

              return (
                <div
                  key={getId(subject)}
                  className={styles.quizSubjectBlock}
                >

                  <div className={styles.quizSubjectHeader}>
                    <div>
                      <h3>
                        {subject.nom || subject.name || subject.titre}
                      </h3>

                      <p>
                        {teacherName || "Professeur non défini"}
                      </p>
                    </div>

                    <span>
                      {subjectQuizzes.length} quiz
                    </span>
                  </div>

                  {subjectQuizzes.length === 0 && (
                    <Empty text="Aucun quiz dans cette matière." />
                  )}

                  <div className={styles.quizCardsGrid}>

                    {subjectQuizzes.map((quiz) => {

                      const quizTeacher =
                        quiz.teacherName ||
                        quiz.enseignantName ||
                        teacherName;

                      return (
                        <article
                          key={getId(quiz)}
                          className={styles.quizCard}
                        >

                          <div className={styles.quizCardTop}>
                            <strong>
                              {quizTitle(quiz)}
                            </strong>

                            <em
                              className={
                                isPublished(quiz)
                                  ? styles.quizPublished
                                  : styles.quizDraft
                              }
                            >
                              {isPublished(quiz)
                                ? "Publié"
                                : "Brouillon"}
                            </em>
                          </div>

                          <p className={styles.quizTeacher}>
                            Professeur : {quizTeacher}
                          </p>

                          <p className={styles.quizTheme}>
                            {quiz.theme || "Sans thème"}
                          </p>

                          <div className={styles.actions}>

                            <button
                              onClick={() =>
                                adminApi
                                  .blockQuiz(getId(quiz))
                                  .then(() =>
                                    setNotice("Quiz bloqué.")
                                  )
                                  .catch(() =>
                                    setError(
                                      "Blocage indisponible."
                                    )
                                  )
                              }
                            >
                              Bloquer
                            </button>

                            <button
                              className={styles.dangerBtn}
                              onClick={() =>
                                deleteQuiz(getId(quiz))
                              }
                            >
                              <Trash2 size={16} />
                            </button>

                          </div>

                        </article>
                      );
                    })}

                  </div>

                </div>
              );
            })}

          </div>
        );
      })}

    </div>

  </Panel>
)}

      {section === "__legacy_statistics" && (
        <>
          <StatsGrid summary={summary} />
          <section className={styles.dashboardGrid}>
            <Panel title="Quiz les plus difficiles" icon={AlertTriangle}>
              <RankingList rows={difficultQuizzes} />
            </Panel>
            <Panel title="Meilleurs étudiants" icon={Trophy}>
              <TopStudents rows={topStudents} />
            </Panel>
          </section>
        </>
      )}

      {section === "emails" && (
        <Panel title="Envoyer une annonce" icon={Mail}>
          <form className={styles.formWide} onSubmit={sendEmail}>
            <select value={emailForm.target} onChange={(e) => setEmailForm({ ...emailForm, target: e.target.value })}>
              <option value="ETUDIANTS">Tous les étudiants</option>
              <option value="ENSEIGNANTS">Tous les enseignants</option>
              <option value="TOUS">Toute la plateforme</option>
            </select>
            <input required placeholder="Objet" value={emailForm.subject} onChange={(e) => setEmailForm({ ...emailForm, subject: e.target.value })} />
            <textarea required placeholder="Message à envoyer..." value={emailForm.message} onChange={(e) => setEmailForm({ ...emailForm, message: e.target.value })} />
            <button disabled={working === "send-email"}>
              {working === "send-email" ? "Envoi en cours..." : "Envoyer l'email"}
            </button>
          </form>
        </Panel>
      )}

    </div>
  );
}

function StatsGrid({ summary }) {
  const items = [
    ["Utilisateurs", summary.users, Users],
    ["Étudiants", summary.students, GraduationCap],
    ["Enseignants", summary.teachers, ShieldCheck],
    ["Quiz publiés", `${summary.published}/${summary.quizzes}`, BookOpen],
    ["Classes", summary.classes, GraduationCap],
    ["Moyenne globale", percent(summary.average), BarChart3],
  ];

  return (
    <section className={styles.statsGrid}>
      {items.map(([label, value, Icon]) => (
        <div key={label}>
          <Icon size={22} />
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </section>
  );
}

function Panel({ title, icon: Icon, children, wide }) {
  return (
    <section className={`${styles.panel} ${wide ? styles.widePanel : ""}`}>
      <div className={styles.panelHeader}>
        <div>
          <Icon size={22} />
          <h2>{title}</h2>
        </div>
      </div>
      {children}
    </section>
  );
}

function UserGroup({ title, count, children }) {
  return (
    <section className={styles.userLayer}>
      <div className={styles.userLayerHeader}>
        <div>
          <span>{title}</span>
          <strong>{count} compte(s)</strong>
        </div>
      </div>
      {count > 0 ? (
        <div className={styles.table}>
          <div className={styles.tableHead}>
            <span>Utilisateur</span><span>Email</span><span>Rôle / Classe</span><span>Actions</span>
          </div>
          {children}
        </div>
      ) : (
        <Empty text={`Aucun ${title.toLowerCase()} ne correspond à la recherche.`} />
      )}
    </section>
  );
}

function StatusFilterBar({ active, onChange, counts }) {
  const items = [
    ["ALL", "Tous"],
    ["DRAFT", "Brouillons"],
    ["PUBLISHED", "Publiés"],
    ["EXPIRED", "Expirés"],
  ];

  return (
    <div className={styles.statusFilterBar}>
      {items.map(([value, label]) => (
        <button
          key={value}
          type="button"
          className={active === value ? styles.statusFilterActive : ""}
          onClick={() => onChange(value)}
        >
          <span>{label}</span>
          <b>{counts?.[value] ?? 0}</b>
        </button>
      ))}
    </div>
  );
}

function SearchBar({ value, onChange, placeholder }) {
  return (
    <label className={styles.searchBox}>
      <Search size={18} />
      <input value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
    </label>
  );
}

function TeacherPicker({ teachers, selectedIds, onChange, compact = false }) {
  const [teacherSearch, setTeacherSearch] = useState("");
  const selectedSet = new Set((selectedIds || []).map(String));
  const filteredTeachers = teachers.filter((teacher) =>
    [fullName(teacher), teacher.email].join(" ").toLowerCase().includes(teacherSearch.trim().toLowerCase())
  );

  const toggleTeacher = (id) => {
    const next = new Set(selectedSet);
    if (next.has(String(id))) {
      next.delete(String(id));
    } else {
      next.add(String(id));
    }
    onChange(Array.from(next));
  };

  return (
    <div className={`${styles.teacherPicker} ${compact ? styles.teacherPickerCompact : ""}`}>
      <label className={styles.teacherSearch}>
        <Search size={16} />
        <input
          value={teacherSearch}
          onChange={(event) => setTeacherSearch(event.target.value)}
          placeholder="Rechercher un enseignant..."
        />
      </label>
      <div className={styles.teacherList}>
      {teachers.length === 0 && <span className={styles.emptyPicker}>Aucun enseignant disponible.</span>}
      {teachers.length > 0 && filteredTeachers.length === 0 && (
        <span className={styles.emptyPicker}>Aucun enseignant trouvé.</span>
      )}
      {filteredTeachers.map((teacher) => {
        const id = String(getId(teacher));
        const checked = selectedSet.has(id);
        return (
          <label
            key={id}
            className={checked ? `${styles.teacherChip} ${styles.teacherChipActive}` : styles.teacherChip}
          >
            <input type="checkbox" checked={checked} onChange={() => toggleTeacher(id)} />
            <span>{fullName(teacher).slice(0, 2).toUpperCase()}</span>
            <strong>{fullName(teacher)}</strong>
            {!compact && <em>{teacher.email}</em>}
            <i aria-hidden="true">✓</i>
          </label>
        );
      })}
      </div>
    </div>
  );
}

function TeacherList({ names }) {
  const teacherNames = (names || []).filter(Boolean);
  if (teacherNames.length === 0) {
    return <p className={styles.teacherNames}>Enseignants : Non affecté</p>;
  }

  return (
    <div className={styles.teacherNames}>
      <span>Enseignants :</span>
      <ul>
        {teacherNames.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
    </div>
  );
}

function Empty({ text }) {
  return <div className={styles.empty}>{text}</div>;
}

function TopStudents({ rows }) {
  if (!rows.length) return <Empty text="Aucun classement disponible pour le moment." />;
  return (
    <div className={styles.topList}>
      {rows.map((row, index) => (
        <div key={`${row.name}-${row.quiz}-${index}`}>
          <b>{index + 1}</b>
          <span>
            <strong>{row.name}</strong>
            <small>{row.quiz}</small>
          </span>
          <em>{percent(row.score)}</em>
        </div>
      ))}
    </div>
  );
}

function RankingList({ rows }) {
  if (!rows.length) return <Empty text="Aucun quiz publié à analyser." />;
  return (
    <div className={styles.topList}>
      {rows.map(({ quiz, average }, index) => (
        <div key={getId(quiz)}>
          <b>{index + 1}</b>
          <span>
            <strong>{quizTitle(quiz)}</strong>
            <small>{quiz.theme || "Sans thème"}</small>
          </span>
          <em>{percent(average)}</em>
        </div>
      ))}
    </div>
  );
}

function titleFor(section) {
  return {
    dashboard: "Tableau de bord administrateur",
    users: "Gestion des utilisateurs",
    classes: "Gestion des classes",
    subjects: "Gestion des matières",
    quizzes: "Contrôle des quiz",
    results: "Résultats des quiz",
    statistics: "Statistiques globales",
    emails: "Centre d'emails",
  }[section] || "Console administrateur";
}

function descriptionFor(section) {
  return {
    dashboard: "Pilotez la plateforme, surveillez les comptes, les quiz, les classes et l'activité globale.",
    users: "Créez, recherchez, supprimez, bloquez ou préparez la réinitialisation des comptes.",
    classes: "Organisez les groupes pédagogiques et préparez les affectations étudiants / enseignants.",
    subjects: "Créez les matières et affectez chaque matière à un enseignant et une classe.",
    quizzes: "Visualisez les quiz par professeur, classe et matière, avec recherche et actions sur les quiz expirés.",
    results: "Analysez les soumissions par professeur, classe et matière avec les informations complètes des étudiants.",
    statistics: "Analysez la réussite, la participation, les meilleurs étudiants et les quiz difficiles.",
    emails: "Envoyez des annonces, notifications et messages pédagogiques depuis l'administration.",
  }[section] || "";
}
