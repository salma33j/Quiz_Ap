import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BarChart3,
  BookOpen,
  Users,
  Trophy,
  Filter,
  Search,
  Target,
  AlertTriangle,
  CheckCircle2,
  Timer,
} from "lucide-react";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
} from "recharts";

import axiosInstance from "../../api/axiosInstance";
import styles from "./QuizStatistics.module.css";

const COLORS = ["#22c55e", "#ef4444"];
const unwrap = (res) => res.data?.data ?? res.data;

const getNoteSur20 = (row) => {
  if (row.noteSur20 != null) return Number(row.noteSur20);
  if (row.note != null) return Number(row.note);
  if (row.scorePercentage != null) return (Number(row.scorePercentage) * 20) / 100;

  const earned = Number(row.earnedPoints ?? row.pointsObtenus);
  const total = Number(row.totalPoints ?? row.total);
  if (Number.isFinite(earned) && Number.isFinite(total) && total > 0) {
    return (earned * 20) / total;
  }

  return 0;
};

const isAnalyzableQuiz = (quiz) => {
  const status = String(quiz.status || "").toUpperCase();
  if (status === "EXPIRED") return true;
  if (status !== "PUBLISHED" || !quiz.availableUntil) return false;

  const expirationDate = new Date(quiz.availableUntil);
  return !Number.isNaN(expirationDate.getTime()) && expirationDate <= new Date();
};

const getStudentName = (row) =>
  row.studentName ||
  `${row.studentFirstName || ""} ${row.studentLastName || ""}`.trim() ||
  row.studentEmail ||
  "Etudiant";

const mapFailedStudent = (row, quiz) => {
  const note = getNoteSur20(row);

  return {
    id:
      row.id ||
      `${row.quizId || quiz.id}-${row.studentId || row.studentEmail || getStudentName(row)}-${row.failureReason || "failed"}`,
    quizId: row.quizId || quiz.id,
    quizTitle: row.quizTitle || quiz.titre || quiz.title || "Quiz sans titre",
    subjectName:
      row.subjectName ||
      row.matiereName ||
      row.matiereNom ||
      quiz.matiereName ||
      quiz.matiereNom ||
      quiz.subjectName ||
      quiz.theme ||
      "Matiere non definie",
    className:
      row.className ||
      row.classeName ||
      quiz.className ||
      quiz.classeName ||
      "Classe non definie",
    studentName: getStudentName(row),
    studentEmail: row.studentEmail || "",
    note: Number.isFinite(note) ? note : 0,
    scorePercentage:
      row.scorePercentage != null
        ? Number(row.scorePercentage)
        : Number.isFinite(note)
          ? note * 5
          : 0,
    completedDate: row.completedDate || null,
    availableUntil: row.availableUntil || quiz.availableUntil || null,
    questionCount: quiz.questionCount || 0,
    totalStudentsAllowed: quiz.totalStudentsAllowed || 0,
    status: quiz.status,
    statusLabel: row.statusLabel || "Note insuffisante",
    failureReason: row.failureReason || "NOTE_INSUFFISANTE",
  };
};

export default function QuizStatistics() {
  const { quizId, id } = useParams();
  const qid = quizId || id;
  const isSingleQuizView = Boolean(qid);
  const [quizzes, setQuizzes] = useState([]);
  const [results, setResults] = useState([]);
  const [failedRows, setFailedRows] = useState([]);
  const [selectedClass, setSelectedClass] = useState("ALL");
  const [selectedSubject, setSelectedSubject] = useState("ALL");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadStatistics = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const quizRes = qid
        ? await axiosInstance.get(`/teacher/quizzes/${qid}`)
        : await axiosInstance.get("/teacher/quizzes");
      const quizData = unwrap(quizRes);
      const quizList = qid
        ? [quizData || { id: qid }]
        : Array.isArray(quizData)
          ? quizData
          : [];

      const validQuizzes = quizList.filter(isAnalyzableQuiz);

      if (qid && validQuizzes.length === 0) {
        setError("Les statistiques seront disponibles après la date d'expiration du quiz.");
        setQuizzes([]);
        setResults([]);
        setFailedRows([]);
        return;
      }

      const allResults = [];
      const allFailedRows = [];

      for (const quiz of validQuizzes) {
        try {
          const [res, statsRes] = await Promise.all([
            axiosInstance.get(`/resultats/quiz/${quiz.id}`),
            axiosInstance.get(`/resultats/quiz/${quiz.id}/statistics`).catch((err) => {
              console.warn("Statistiques introuvables pour quiz :", quiz.id, err);
              return null;
            }),
          ]);
          const quizResults = Array.isArray(unwrap(res)) ? unwrap(res) : [];
          const statsData = statsRes ? unwrap(statsRes) : null;
          const backendFailedRows = Array.isArray(statsData?.failedStudents)
            ? statsData.failedStudents
            : null;

          const mappedResults = quizResults.map((r) => ({
              id: r.id,
              quizId: quiz.id,
              quizTitle: r.quizTitle || quiz.titre || quiz.title || "Quiz sans titre",
              subjectName:
                r.subjectName ||
                r.matiereName ||
                r.matiereNom ||
                quiz.matiereName ||
                quiz.matiereNom ||
                quiz.subjectName ||
                r.quizTheme ||
                quiz.theme ||
                "Matiere non definie",
              className:
                r.className ||
                r.classeName ||
                quiz.className ||
                quiz.classeName ||
                "Classe non définie",
              studentName:
                r.studentName ||
                `${r.studentFirstName || ""} ${r.studentLastName || ""}`.trim() ||
                "Étudiant",
              note: getNoteSur20(r),
              scorePercentage:
                r.scorePercentage != null ? Number(r.scorePercentage) : getNoteSur20(r) * 5,
              completedDate: r.completedDate || r.startedAt || null,
              questionCount: quiz.questionCount || 0,
              totalStudentsAllowed: quiz.totalStudentsAllowed || 0,
              availableUntil: quiz.availableUntil,
              status: quiz.status,
            }));

          mappedResults.forEach((row) => allResults.push(row));

          if (backendFailedRows) {
            backendFailedRows.forEach((row) => {
              allFailedRows.push(mapFailedStudent(row, quiz));
            });
          } else {
            mappedResults
              .filter((row) => row.note < 10)
              .forEach((row) => {
                allFailedRows.push({
                  ...row,
                  statusLabel: "Note insuffisante",
                  failureReason: "NOTE_INSUFFISANTE",
                });
              });
          }
        } catch (err) {
          console.warn("Résultats introuvables pour quiz :", quiz.id, err);
        }
      }

      setQuizzes(validQuizzes);
      setResults(allResults);
      setFailedRows(allFailedRows);
    } catch (err) {
      console.error("Erreur statistiques :", err);
      setError("Impossible de charger les statistiques depuis le backend.");
      setQuizzes([]);
      setResults([]);
      setFailedRows([]);
    } finally {
      setLoading(false);
    }
  }, [qid]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadStatistics();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadStatistics]);

  const analysisRows = useMemo(() => [...results, ...failedRows], [results, failedRows]);

  const classes = useMemo(() => {
    return [...new Set(analysisRows.map((r) => r.className))].filter(Boolean).sort();
  }, [analysisRows]);

  const rowsForSubjectFilter = useMemo(() => {
    if (selectedClass === "ALL") return analysisRows;
    return analysisRows.filter((r) => r.className === selectedClass);
  }, [analysisRows, selectedClass]);

  const subjects = useMemo(() => {
    return [...new Set(rowsForSubjectFilter.map((r) => r.subjectName))].filter(Boolean).sort();
  }, [rowsForSubjectFilter]);

  useEffect(() => {
    if (selectedSubject !== "ALL" && !subjects.includes(selectedSubject)) {
      setSelectedSubject("ALL");
    }
  }, [selectedSubject, subjects]);

  const matchesFilters = useCallback((r) => {
    const value = search.trim().toLowerCase();

    const matchClass = selectedClass === "ALL" || r.className === selectedClass;
    const matchSubject = selectedSubject === "ALL" || r.subjectName === selectedSubject;

    const matchSearch = [
      r.quizTitle,
      r.subjectName,
      r.className,
      r.studentName,
      r.studentEmail,
      r.statusLabel,
      r.note,
    ]
      .join(" ")
      .toLowerCase()
      .includes(value);

    return matchClass && matchSubject && matchSearch;
  }, [selectedClass, selectedSubject, search]);

  const filteredResults = useMemo(() => {
    return results.filter(matchesFilters);
  }, [results, matchesFilters]);

  const totalStudents = new Set(filteredResults.map((r) => r.studentName)).size;
  const totalParticipations = filteredResults.length;

  const average =
    totalParticipations > 0
      ? filteredResults.reduce((sum, r) => sum + r.note, 0) / totalParticipations
      : 0;

  const bestScore =
    totalParticipations > 0 ? Math.max(...filteredResults.map((r) => r.note)) : 0;

  const failedStudents = useMemo(() => {
    return failedRows
      .filter(matchesFilters)
      .sort((a, b) => {
        const reasonCompare = String(a.failureReason || "").localeCompare(String(b.failureReason || ""));
        if (reasonCompare !== 0) return reasonCompare;
        return a.note - b.note;
      });
  }, [failedRows, matchesFilters]);

  const successCount = filteredResults.filter((r) => r.note >= 10).length;
  const failCount = failedStudents.length;
  const missedCount = failedStudents.filter((r) =>
    ["NON_REPONDU", "NON_TERMINE"].includes(String(r.failureReason || "").toUpperCase())
  ).length;

  const successRate =
    successCount + failCount > 0 ? (successCount / (successCount + failCount)) * 100 : 0;

  const filteredQuizCount = new Set(
    [...filteredResults, ...failedStudents].map((r) => r.quizId)
  ).size;

  const subjectAverageData = useMemo(() => {
    const map = {};

    filteredResults.forEach((r) => {
      if (!map[r.subjectName]) map[r.subjectName] = { total: 0, count: 0 };
      map[r.subjectName].total += r.note;
      map[r.subjectName].count += 1;
    });

    return Object.entries(map).map(([name, data]) => ({
      name,
      moyenne: Number((data.total / data.count).toFixed(2)),
    }));
  }, [filteredResults]);

  const subjectParticipantsData = useMemo(() => {
    const map = {};

    filteredResults.forEach((r) => {
      map[r.subjectName] = (map[r.subjectName] || 0) + 1;
    });

    return Object.entries(map).map(([name, participants]) => ({
      name,
      participants,
    }));
  }, [filteredResults]);

  const classAverageData = useMemo(() => {
    const map = {};

    filteredResults.forEach((r) => {
      if (!map[r.className]) map[r.className] = { total: 0, count: 0 };
      map[r.className].total += r.note;
      map[r.className].count += 1;
    });

    return Object.entries(map).map(([name, data]) => ({
      name,
      moyenne: Number((data.total / data.count).toFixed(2)),
    }));
  }, [filteredResults]);

  const successPieData = [
    { name: "Réussite", value: successCount },
    { name: "Échec", value: failCount },
  ];

  const quizTableData = useMemo(() => {
    const map = {};

    filteredResults.forEach((r) => {
      const key = `${r.quizId}-${r.className}-${r.subjectName}`;

      if (!map[key]) {
        map[key] = {
          key,
          quizId: r.quizId,
          quizTitle: r.quizTitle,
          subjectName: r.subjectName,
          className: r.className,
          participants: 0,
          total: 0,
          success: 0,
        };
      }

      map[key].participants += 1;
      map[key].total += r.note;
      if (r.note >= 10) map[key].success += 1;
    });

    return Object.values(map)
      .map((q) => ({
        ...q,
        moyenne: Number((q.total / q.participants).toFixed(2)),
        successRate: Number(((q.success / q.participants) * 100).toFixed(1)),
      }))
      .sort((a, b) => a.moyenne - b.moyenne);
  }, [filteredResults]);

  const hardestQuiz = quizTableData[0];
  const easiestQuiz = quizTableData[quizTableData.length - 1];

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.loadingBox}>
          <div>
            <div className={styles.spinner}></div>
            <p>Chargement des statistiques...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <span className={styles.badge}>{isSingleQuizView ? "Analyse du quiz" : "Vue globale"}</span>
          <h1>Statistiques</h1>
          <p>
            {isSingleQuizView
              ? "Analyse des résultats disponibles pour ce quiz."
              : "Vue globale de tous les quiz publies arrives a expiration, avec filtres par classe puis par matiere."}
          </p>
        </div>

        {isSingleQuizView && (
          <Link className={styles.backBtn} to="/teacher/quizzes">
            <ArrowLeft size={18} />
            Mes quiz
          </Link>
        )}
      </div>

      {error && <div className={styles.errorBox}>{error}</div>}

      <div className={styles.filters}>
        <div className={styles.searchBox}>
          <Search size={19} />
          <input
            placeholder="Rechercher par quiz, classe, matière ou étudiant..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className={styles.selectBox}>
          <Filter size={18} />
          <select
            value={selectedClass}
            onChange={(e) => {
              setSelectedClass(e.target.value);
              setSelectedSubject("ALL");
            }}
          >
            <option value="ALL">Toutes les classes</option>
            {classes.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        <div className={styles.selectBox}>
          <BookOpen size={18} />
          <select value={selectedSubject} onChange={(e) => setSelectedSubject(e.target.value)}>
            <option value="ALL">
              {selectedClass === "ALL" ? "Toutes les matieres" : "Matieres de cette classe"}
            </option>
            {subjects.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className={styles.statsGrid}>
        <StatCard icon={<BookOpen />} title={isSingleQuizView ? "Quiz selectionne" : "Quiz analyses"} value={quizzes.length} color="blue" />
        <StatCard icon={<Users />} title="Étudiants participants" value={totalStudents} color="green" />
        <StatCard icon={<BarChart3 />} title="Participations" value={totalParticipations} color="purple" />
        <StatCard icon={<Target />} title="Moyenne générale" value={`${average.toFixed(2)}/20`} color="orange" />
        <StatCard icon={<Trophy />} title="Meilleure note" value={`${bestScore.toFixed(2)}/20`} color="yellow" />
        <StatCard icon={<CheckCircle2 />} title="Taux réussite" value={`${successRate.toFixed(1)}%`} color="green" />
        <StatCard icon={<AlertTriangle />} title="Etudiants rates" value={failCount} color="red" />
        <StatCard icon={<AlertTriangle />} title="Sans reponse" value={missedCount} color="red" />
        <StatCard icon={<Timer />} title="Quiz filtres" value={filteredQuizCount} color="blue" />
      </div>

      <div className={styles.insightsGrid}>
        <div className={styles.insightCard}>
          <h3>Quiz le plus difficile</h3>
          <strong>{hardestQuiz?.quizTitle || "Aucun"}</strong>
          <p>{hardestQuiz ? `${hardestQuiz.moyenne}/20 moyenne` : "Pas assez de données"}</p>
        </div>

        <div className={styles.insightCard}>
          <h3>Quiz le plus réussi</h3>
          <strong>{easiestQuiz?.quizTitle || "Aucun"}</strong>
          <p>{easiestQuiz ? `${easiestQuiz.moyenne}/20 moyenne` : "Pas assez de données"}</p>
        </div>

        <div className={styles.insightCard}>
          <h3>Réussite</h3>
          <strong>{successCount}</strong>
          <p>participation(s) réussie(s)</p>
        </div>

        <div className={styles.insightCard}>
          <h3>Échec</h3>
          <strong>{failCount}</strong>
          <p>participation(s) en échec</p>
        </div>
      </div>

      <div className={`${styles.tableCard} ${styles.failedStudentsCard}`}>
        <div className={styles.sectionHeader}>
          <div>
            <h2>{isSingleQuizView ? "Etudiants qui ont rate ce quiz" : "Etudiants qui ont rate un quiz"}</h2>
            <p>
              {isSingleQuizView
                ? `${failCount} etudiant(s) en echec ou sans reponse pour ce quiz.`
                : `${failCount} etudiant(s) en echec ou sans reponse dans les quiz filtres.`}
            </p>
          </div>
        </div>

        <div className={styles.tableWrapper}>
          <table>
            <thead>
              <tr>
                <th>Etudiant</th>
                <th>Quiz rate</th>
                <th>Classe</th>
                <th>Matiere</th>
                <th>Statut</th>
                <th>Note</th>
                <th>Date</th>
              </tr>
            </thead>

            <tbody>
              {failedStudents.length === 0 ? (
                <tr>
                  <td colSpan="7">Aucun etudiant en echec pour ce filtre.</td>
                </tr>
              ) : (
                failedStudents.map((row) => (
                  <tr key={`${row.quizId}-${row.studentName}-${row.failureReason}-${row.completedDate || row.id}`}>
                    <td>{row.studentName}</td>
                    <td>{row.quizTitle}</td>
                    <td>{row.className}</td>
                    <td>{row.subjectName}</td>
                    <td>
                      <span className={styles.reasonPill}>{row.statusLabel}</span>
                    </td>
                    <td>
                      <span className={styles.failPill}>{row.note.toFixed(2)}/20</span>
                    </td>
                    <td>
                      {row.completedDate
                        ? new Date(row.completedDate).toLocaleDateString("fr-FR")
                        : row.availableUntil
                          ? `Fin ${new Date(row.availableUntil).toLocaleDateString("fr-FR")}`
                        : "-"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className={styles.chartsGrid}>
        <div className={styles.chartCard}>
          <h2>Moyenne par matière</h2>
          {subjectAverageData.length === 0 ? (
            <p className={styles.emptyText}>Aucune donnée disponible.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={subjectAverageData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis domain={[0, 20]} />
                <Tooltip />
                <Bar dataKey="moyenne" fill="#4f46e5" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className={styles.chartCard}>
          <h2>Réussite / Échec</h2>
          {successCount + failCount === 0 ? (
            <p className={styles.emptyText}>Aucune donnee disponible.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={successPieData} dataKey="value" nameKey="name" outerRadius={105} label>
                  {successPieData.map((_, index) => (
                    <Cell key={index} fill={COLORS[index]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className={styles.chartCard}>
          <h2>Moyenne par classe</h2>
          {classAverageData.length === 0 ? (
            <p className={styles.emptyText}>Aucune donnée disponible.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={classAverageData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis domain={[0, 20]} />
                <Tooltip />
                <Bar dataKey="moyenne" fill="#22c55e" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className={styles.chartCard}>
          <h2>Participants par matière</h2>
          {subjectParticipantsData.length === 0 ? (
            <p className={styles.emptyText}>Aucune donnée disponible.</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={subjectParticipantsData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="participants" fill="#f97316" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      <div className={styles.tableCard}>
        <h2>Statistiques détaillées des quiz analysés</h2>

        <div className={styles.tableWrapper}>
          <table>
            <thead>
              <tr>
                <th>Quiz</th>
                <th>Matière</th>
                <th>Classe</th>
                <th>Participants</th>
                <th>Moyenne</th>
                <th>Taux réussite</th>
              </tr>
            </thead>

            <tbody>
              {quizTableData.length === 0 ? (
                <tr>
                  <td colSpan="6">Aucune statistique disponible.</td>
                </tr>
              ) : (
                quizTableData.map((q) => (
                  <tr key={q.key}>
                    <td>{q.quizTitle}</td>
                    <td>{q.subjectName}</td>
                    <td>{q.className}</td>
                    <td>{q.participants}</td>
                    <td>{q.moyenne}/20</td>
                    <td>{q.successRate}%</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, title, value, color }) {
  return (
    <div className={styles.statCard}>
      <div className={`${styles.statIcon} ${styles[color]}`}>{icon}</div>
      <div>
        <h3>{value}</h3>
        <p>{title}</p>
      </div>
    </div>
  );
}
