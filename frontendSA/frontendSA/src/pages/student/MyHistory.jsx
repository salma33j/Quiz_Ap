import { useEffect, useMemo, useState } from "react";
import { BookOpen, CalendarDays, GraduationCap, Search, Trophy } from "lucide-react";
import axiosInstance from "../../api/axiosInstance";
import studentQuizApi from "../../api/studentQuizApi";
import styles from "./MyHistory.module.css";

const unwrap = (res) => res?.data?.data ?? res?.data ?? [];

const getSubjectName = (item) =>
  item.matiereNom ||
  item.matiereName ||
  item.subjectName ||
  item.nom ||
  item.name ||
  item.quizTheme ||
  item.theme ||
  item.matiere?.nom ||
  "Matière générale";

const getSubjectId = (item) =>
  item.matiereId ||
  item.subjectId ||
  item.matiere?.id ||
  item.subject?.id ||
  item.id ||
  getSubjectName(item);

const getQuizSubjectId = (item) =>
  item.matiereId ||
  item.subjectId ||
  item.matiere?.id ||
  item.subject?.id ||
  getSubjectName(item);

const getQuizTitle = (item, index) =>
  item.quizTitle || item.titre || item.title || item.quiz?.titre || `Quiz ${index + 1}`;

const getQuizId = (item) => item.quizId || item.idQuiz || item.quiz?.id || item.id;

const getScore = (item) => {
  const raw =
    item.scorePercentage ??
    item.percentage ??
    item.score ??
    (item.noteSur20 != null ? Number(item.noteSur20) * 5 : 0);

  const value = Number(raw);
  return Number.isFinite(value) ? Math.round(value) : 0;
};

const getNoteLabel = (item) => {
  if (item.noteSur20 != null) return `${Number(item.noteSur20).toFixed(2)} / 20`;
  if (item.earnedPoints != null && item.totalPoints != null) {
    return `${item.earnedPoints} / ${item.totalPoints}`;
  }
  return `${getScore(item)}%`;
};

const getRanking = (item) =>
  item.rang || item.rank || item.classement || item.position || "—";

const getQuizEndDate = (item) =>
  item.availableUntil ||
  item.dateFin ||
  item.endDate ||
  item.deadline ||
  item.expiresAt ||
  item.quiz?.availableUntil ||
  item.quiz?.dateFin ||
  null;

const isQuizExpired = (item) => {
  const status = String(item.status || item.quiz?.status || "").toUpperCase();
  if (status === "EXPIRED") return true;

  const endDate = getQuizEndDate(item);
  if (!endDate) return false;

  const parsedEndDate = new Date(endDate);
  return !Number.isNaN(parsedEndDate.getTime()) && parsedEndDate <= new Date();
};

const formatDateTime = (value) => {
  if (!value) return "Date inconnue";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Date inconnue";

  return date.toLocaleString("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function MyHistory() {
  const [history, setHistory] = useState([]);
  const [matieres, setMatieres] = useState([]);
  const [search, setSearch] = useState("");
  const [selectedSubject, setSelectedSubject] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      setLoading(true);
      setError("");

      const [matieresData, historyRes] = await Promise.all([
        studentQuizApi.getMySubjects().catch(() => []),
        axiosInstance.get("/resultats/my-history"),
      ]);

      const data = unwrap(historyRes);

      const completed = Array.isArray(data)
        ? data.filter((item) => item.isCompleted !== false)
        : [];

      setMatieres(Array.isArray(matieresData) ? matieresData : []);
      setHistory(completed);
    } catch (err) {
      console.error("Erreur historique:", err);
      setError("Impossible de charger votre historique.");
    } finally {
      setLoading(false);
    }
  };

  const subjects = useMemo(() => {
    const map = new Map();

    matieres.forEach((matiere) => {
      const id = String(getSubjectId(matiere));
      map.set(id, {
        id,
        name: getSubjectName(matiere),
        count: 0,
      });
    });

    history.forEach((item) => {
      const subjectId = String(getQuizSubjectId(item));
      const subjectName = getSubjectName(item);

      if (!map.has(subjectId)) {
        map.set(subjectId, {
          id: subjectId,
          name: subjectName,
          count: 0,
        });
      }

      map.get(subjectId).count += 1;
    });

    return Array.from(map.values()).sort((a, b) => a.name.localeCompare(b.name));
  }, [matieres, history]);

  const groupedHistory = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const map = new Map();

    subjects.forEach((subject) => {
      if (selectedSubject === "ALL" || String(selectedSubject) === String(subject.id)) {
        map.set(subject.id, {
          subjectId: subject.id,
          subject: subject.name,
          quizzes: [],
        });
      }
    });

    history.forEach((item, index) => {
      const subjectId = String(getQuizSubjectId(item));
      const subjectName =
        subjects.find((s) => String(s.id) === String(subjectId))?.name ||
        getSubjectName(item);

      const title = getQuizTitle(item, index);

      const matchSubject =
        selectedSubject === "ALL" || String(selectedSubject) === String(subjectId);

      const matchSearch =
        !keyword ||
        title.toLowerCase().includes(keyword) ||
        subjectName.toLowerCase().includes(keyword);

      if (!matchSubject || !matchSearch) return;

      if (!map.has(subjectId)) {
        map.set(subjectId, {
          subjectId,
          subject: subjectName,
          quizzes: [],
        });
      }

      map.get(subjectId).quizzes.push({
        ...item,
        _title: title,
        _quizId: getQuizId(item),
        _score: getScore(item),
        _note: getNoteLabel(item),
        _ranking: isQuizExpired(item) ? getRanking(item) : "Apres expiration",
        _submittedAt:
          item.submittedAt ||
          item.completedDate ||
          item.dateSoumission ||
          item.date ||
          item.createdAt,
        _date:
          item.completedDate ||
          item.date ||
          item.createdAt ||
          item.submittedAt,
      });
    });

    return Array.from(map.values()).filter((group) => {
      if (search.trim()) return group.quizzes.length > 0;
      return true;
    });
  }, [history, subjects, search, selectedSubject]);

  if (loading) {
    return <div className={styles.loading}>Chargement de votre historique...</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <span className={styles.badge}>Étudiant</span>
        <h1>Mon historique</h1>
        <p>
          Consultez vos quiz déjà passés, classés selon les vraies matières
          assignées à votre classe.
        </p>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <div className={styles.toolbar}>
        <div className={styles.searchBox}>
          <Search size={18} />
          <input
            type="text"
            placeholder="Rechercher par quiz ou matière..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <select
          value={selectedSubject}
          onChange={(e) => setSelectedSubject(e.target.value)}
          className={styles.select}
        >
          <option value="ALL">Toutes les matières</option>
          {subjects.map((subject) => (
            <option key={subject.id} value={subject.id}>
              {subject.name}
            </option>
          ))}
        </select>
      </div>

      {groupedHistory.length === 0 ? (
        <div className={styles.empty}>
          <BookOpen size={42} />
          <p>Aucun résultat trouvé.</p>
        </div>
      ) : (
        <div className={styles.subjects}>
          {groupedHistory.map((group) => (
            <section className={styles.subjectBlock} key={group.subjectId}>
              <div className={styles.subjectHeader}>
                <div className={styles.subjectIcon}>
                  <GraduationCap size={22} />
                </div>
                <div>
                  <h2>{group.subject}</h2>
                  <p>{group.quizzes.length} quiz répondu(s)</p>
                </div>
              </div>

              {group.quizzes.length === 0 ? (
                <div className={styles.emptySubject}>
                  Aucun quiz passé dans cette matière pour le moment.
                </div>
              ) : (
                <div className={styles.tableWrapper}>
                  <table className={styles.historyTable}>
                    <thead>
                      <tr>
                        <th>Nom du quiz</th>
                        <th>Date de soumission</th>
                        <th>Score</th>
                        <th>Classement</th>
                        <th>Date</th>
                      </tr>
                    </thead>

                    <tbody>
                      {group.quizzes.map((quiz, index) => (
                        <tr key={`${quiz._quizId}-${index}`}>
                          <td>
                            <div className={styles.quizName}>
                              <BookOpen size={18} />
                              <span>{quiz._title}</span>
                            </div>
                          </td>

                          <td>
                            <CalendarDays size={15} />
                            {formatDateTime(quiz._submittedAt)}
                          </td>

                          <td>
                            <span className={styles.scoreBadge}>
                              <Trophy size={14} />
                              {quiz._note}
                            </span>
                          </td>

                          <td>{quiz._ranking}</td>

                          <td>{formatDateTime(quiz._date)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
