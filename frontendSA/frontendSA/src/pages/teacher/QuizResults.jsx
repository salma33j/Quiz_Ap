import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BarChart3,
  BookOpen,
  Filter,
  Search,
  TrendingDown,
  Trophy,
  Users,
} from "lucide-react";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./QuizResults.module.css";

const getStudentName = (row) =>
  row.studentName ||
  row.etudiant?.fullName ||
  row.student?.fullName ||
  [row.studentFirstName, row.studentLastName].filter(Boolean).join(" ") ||
  [row.firstName, row.lastName].filter(Boolean).join(" ") ||
  [row.student?.firstName, row.student?.lastName].filter(Boolean).join(" ") ||
  "Étudiant";

const getFirstName = (row) =>
  row.studentFirstName || row.firstName || row.student?.firstName || "";

const getLastName = (row) =>
  row.studentLastName || row.lastName || row.student?.lastName || "";

const getCne = (row) =>
  row.cne || row.studentCne || row.student?.cne || row.etudiant?.cne || "-";

const getCodeApogee = (row) =>
  row.codeApogee ||
  row.codeApoge ||
  row.studentCodeApogee ||
  row.studentCodeApoge ||
  row.student?.codeApogee ||
  row.student?.codeApoge ||
  row.etudiant?.codeApogee ||
  row.etudiant?.codeApoge ||
  "-";

const getClassName = (row, quiz) =>
  row.className ||
  row.classeName ||
  row.classe ||
  row.groupName ||
  quiz?.className ||
  quiz?.classeName ||
  quiz?.classe ||
  "Groupe non défini";

const getSubjectName = (row, quiz) =>
  row.subjectName ||
  row.matiereName ||
  row.matiereNom ||
  quiz?.matiereName ||
  quiz?.matiereNom ||
  quiz?.subjectName ||
  row.quizTheme ||
  quiz?.theme ||
  "Matiere non definie";
const getScorePercent = (row) => {
  if (row.scorePercentage != null) return Number(row.scorePercentage);
  if (row.percentage != null) return Number(row.percentage);
  if (row.noteSur20 != null) return Number(row.noteSur20) * 5;

  const earned = Number(row.earnedPoints ?? row.pointsObtenus ?? row.points);
  const total = Number(row.totalPoints ?? row.total);
  if (Number.isFinite(earned) && Number.isFinite(total) && total > 0) {
    return (earned * 100) / total;
  }

  const note = Number(row.note ?? row.score);
  if (Number.isFinite(note)) {
    return note <= 20 ? note * 5 : note;
  }

  return 0;
};

const getNoteSur20 = (row) => getScorePercent(row) / 5;

const getDate = (row) =>
  row.completedDate || row.createdAt || row.submittedAt || row.dateSoumission || row.startedAt;

const averageOf = (items) => {
  if (items.length === 0) return 0;
  return items.reduce((sum, row) => sum + Number(row.noteSur20 || 0), 0) / items.length;
};

const normalizeResult = (row, quiz) => ({
  ...row,
  quizId: row.quizId || quiz?.id,
  quizTitle: row.quizTitle || quiz?.titre || quiz?.title || "Quiz sans titre",
  subjectName: getSubjectName(row, quiz),
  className: getClassName(row, quiz),
  firstName: getFirstName(row),
  lastName: getLastName(row),
  studentName: getStudentName(row),
  cne: getCne(row),
  codeApogee: getCodeApogee(row),
  email: row.email || row.studentEmail || row.student?.email || row.etudiant?.email || "-",
  noteSur20: getNoteSur20(row),
  scorePercent: getScorePercent(row),
  earnedPoints: row.earnedPoints ?? row.pointsObtenus ?? row.points ?? 0,
  totalPoints: row.totalPoints ?? row.total ?? "-",
  submittedAt: getDate(row),
});

export default function QuizResults() {
  const { quizId, id } = useParams();
  const qid = quizId || id;
  const [rows, setRows] = useState([]);
  const [search, setSearch] = useState("");
  const [selectedClass, setSelectedClass] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;

    async function loadResults() {
      try {
        setLoading(true);
        setError("");

        const quizData = qid
          ? [await teacherQuizApi.getQuizById(qid)]
          : await teacherQuizApi.getMyQuizzes();
        const quizzes = Array.isArray(quizData) ? quizData : [];
        const allRows = [];

        for (const quiz of quizzes) {
          try {
            const data = await teacherQuizApi.getResults(quiz.id);
            const results = Array.isArray(data) ? data : [];
            allRows.push(...results.map((row) => normalizeResult(row, quiz)));
          } catch (e) {
            console.warn("Résultats introuvables pour le quiz :", quiz.id, e);
          }
        }

        if (alive) {
          setRows(allRows);
        }
      } catch (e) {
        if (alive) {
          setError(e?.response?.data?.message || "Impossible de charger les résultats.");
          setRows([]);
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    loadResults();

    return () => {
      alive = false;
    };
  }, [qid]);

  const classes = useMemo(
    () => [...new Set(rows.map((row) => row.className))].sort(),
    [rows]
  );

  const filteredRows = useMemo(() => {
    const value = search.trim().toLowerCase();

    return rows
      .filter((row) => selectedClass === "ALL" || row.className === selectedClass)
      .filter((row) =>
        [
          row.quizTitle,
          row.subjectName,
          row.className,
          row.studentName,
          row.firstName,
          row.lastName,
          row.email,
          row.cne,
          row.codeApogee,
          row.noteSur20,
        ]
          .join(" ")
          .toLowerCase()
          .includes(value)
      );
  }, [rows, search, selectedClass]);

  const groupedRows = useMemo(() => {
    const classMap = new Map();

    filteredRows.forEach((row) => {
      if (!classMap.has(row.className)) {
        classMap.set(row.className, {
          className: row.className,
          rows: [],
          subjects: new Map(),
        });
      }

      const classGroup = classMap.get(row.className);
      classGroup.rows.push(row);

      if (!classGroup.subjects.has(row.subjectName)) {
        classGroup.subjects.set(row.subjectName, {
          subjectName: row.subjectName,
          rows: [],
        });
      }

      classGroup.subjects.get(row.subjectName).rows.push(row);
    });

    return Array.from(classMap.values())
      .sort((a, b) => a.className.localeCompare(b.className, "fr", { sensitivity: "base" }))
      .map((classGroup) => ({
        ...classGroup,
        subjects: Array.from(classGroup.subjects.values())
          .sort((a, b) => a.subjectName.localeCompare(b.subjectName, "fr", { sensitivity: "base" }))
          .map((subjectGroup) => ({
            ...subjectGroup,
            rows: subjectGroup.rows.sort((a, b) => b.noteSur20 - a.noteSur20),
          })),
      }));
  }, [filteredRows]);

  const averageNote = averageOf(filteredRows);
  const bestNote =
    filteredRows.length > 0 ? Math.max(...filteredRows.map((row) => row.noteSur20)) : 0;
  const worstNote =
    filteredRows.length > 0 ? Math.min(...filteredRows.map((row) => row.noteSur20)) : 0;
  const successRate =
    filteredRows.length > 0
      ? (filteredRows.filter((row) => row.noteSur20 >= 10).length / filteredRows.length) * 100
      : 0;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <span className={styles.badge}>
            <BarChart3 size={16} />
            Résultats
          </span>
          <h1>{qid ? "Résultats du quiz" : "Résultats des quiz"}</h1>
          <p>Lecture par groupe, matière et étudiant avec les informations nécessaires.</p>
        </div>

        {qid && (
          <Link className={styles.backBtn} to="/teacher/quizzes">
            <ArrowLeft size={18} />
            Mes quiz
          </Link>
        )}
      </div>

      <div className={styles.filters}>
        <div className={styles.searchBox}>
          <Search size={19} />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher étudiant, CNE, Apogée, groupe, matière, quiz..."
          />
        </div>

        <div className={styles.selectBox}>
          <Filter size={18} />
          <select value={selectedClass} onChange={(e) => setSelectedClass(e.target.value)}>
            <option value="ALL">Tous les groupes</option>
            {classes.map((className) => (
              <option key={className} value={className}>
                {className}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className={styles.summary}>
        <div>
          <span>
            <Users size={18} />
            Soumissions
          </span>
          <strong>{filteredRows.length}</strong>
        </div>
        <div>
          <span>
            <BarChart3 size={18} />
            Moyenne
          </span>
          <strong>{averageNote.toFixed(2)}/20</strong>
        </div>
        <div>
          <span>
            <Trophy size={18} />
            Meilleure note
          </span>
          <strong>{bestNote.toFixed(2)}/20</strong>
        </div>
        <div>
          <span>
            <TrendingDown size={18} />
            Plus faible note
          </span>
          <strong>{worstNote.toFixed(2)}/20</strong>
        </div>
        <div>
          <span>
            <BookOpen size={18} />
            Taux réussite
          </span>
          <strong>{successRate.toFixed(1)}%</strong>
        </div>
      </div>

      {loading ? (
        <div className={styles.empty}>Chargement des résultats...</div>
      ) : error ? (
        <div className={styles.errorBox}>{error}</div>
      ) : filteredRows.length === 0 ? (
        <div className={styles.empty}>Aucun résultat trouvé avec ces filtres.</div>
      ) : (
        <div className={styles.classesWrapper}>
          {groupedRows.map((classGroup) => (
            <section className={styles.classCard} key={classGroup.className}>
              <div className={styles.classHeader}>
                <div className={styles.titleGroup}>
                  <div>
                    <span>Groupe / Classe</span>
                    <h2>{classGroup.className}</h2>
                  </div>
                </div>

                <div className={styles.classMetrics}>
                  <strong>{classGroup.rows.length} soumission(s)</strong>
                  <span>{averageOf(classGroup.rows).toFixed(2)}/20 moyenne</span>
                  <span>{classGroup.subjects.length} matière(s)</span>
                </div>
              </div>

              <div className={styles.subjectsWrapper}>
                {classGroup.subjects.map((subjectGroup) => (
                  <article className={styles.subjectCard} key={subjectGroup.subjectName}>
                    <div className={styles.subjectHeader}>
                      <div>
                        <span>Matière</span>
                        <h3>{subjectGroup.subjectName}</h3>
                      </div>

                      <div className={styles.subjectStats}>
                        <b>{subjectGroup.rows.length} étudiant(s)</b>
                        <span>{averageOf(subjectGroup.rows).toFixed(2)}/20</span>
                      </div>
                    </div>

                    <div className={styles.table}>
                      <table>
                        <thead>
                          <tr>
                            <th>Quiz</th>
                            <th>Étudiant</th>
                            <th>CNE</th>
                            <th>Code Apogée</th>
                            <th>Email</th>
                            <th>Note</th>
                            <th>Score</th>
                            <th>Points</th>
                            <th>Date</th>
                          </tr>
                        </thead>
                        <tbody>
                          {subjectGroup.rows.map((row, index) => (
                            <tr
                              key={
                                row.id ||
                                `${row.quizId}-${row.studentId || row.studentName}-${index}`
                              }
                            >
                              <td>{row.quizTitle}</td>
                              <td>
                                <div className={styles.studentCell}>
                                  <strong>{row.studentName}</strong>
                                  <span>
                                    {[row.lastName, row.firstName].filter(Boolean).join(" ") ||
                                      "Nom non détaillé"}
                                  </span>
                                </div>
                              </td>
                              <td>{row.cne}</td>
                              <td>{row.codeApogee}</td>
                              <td>{row.email}</td>
                              <td>
                                <b>{row.noteSur20.toFixed(2)}/20</b>
                              </td>
                              <td>{row.scorePercent.toFixed(1)}%</td>
                              <td>
                                {row.earnedPoints}/{row.totalPoints}
                              </td>
                              <td>
                                {row.submittedAt
                                  ? new Date(row.submittedAt).toLocaleString("fr-FR")
                                  : "-"}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
