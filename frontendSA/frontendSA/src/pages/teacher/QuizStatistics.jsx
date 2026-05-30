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

export default function QuizStatistics() {
  const { quizId, id } = useParams();
  const qid = quizId || id;
  const [quizzes, setQuizzes] = useState([]);
  const [results, setResults] = useState([]);
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
        return;
      }

      const allResults = [];

      for (const quiz of validQuizzes) {
        try {
          const res = await axiosInstance.get(`/resultats/quiz/${quiz.id}`);
          const quizResults = Array.isArray(unwrap(res)) ? unwrap(res) : [];

          quizResults.forEach((r) => {
            allResults.push({
              id: r.id,
              quizId: quiz.id,
              quizTitle: r.quizTitle || quiz.titre || quiz.title || "Quiz sans titre",
              subjectName:
                r.subjectName ||
                r.matiereName ||
                r.quizTheme ||
                quiz.theme ||
                "Matière non définie",
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
            });
          });
        } catch (err) {
          console.warn("Résultats introuvables pour quiz :", quiz.id, err);
        }
      }

      setQuizzes(validQuizzes);
      setResults(allResults);
    } catch (err) {
      console.error("Erreur statistiques :", err);
      setError("Impossible de charger les statistiques depuis le backend.");
      setQuizzes([]);
      setResults([]);
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

  const classes = useMemo(() => {
    return [...new Set(results.map((r) => r.className))].sort();
  }, [results]);

  const subjects = useMemo(() => {
    return [...new Set(results.map((r) => r.subjectName))].sort();
  }, [results]);

  const filteredResults = useMemo(() => {
    const value = search.trim().toLowerCase();

    return results.filter((r) => {
      const matchClass = selectedClass === "ALL" || r.className === selectedClass;
      const matchSubject = selectedSubject === "ALL" || r.subjectName === selectedSubject;

      const matchSearch = [
        r.quizTitle,
        r.subjectName,
        r.className,
        r.studentName,
        r.note,
      ]
        .join(" ")
        .toLowerCase()
        .includes(value);

      return matchClass && matchSubject && matchSearch;
    });
  }, [results, selectedClass, selectedSubject, search]);

  const totalStudents = new Set(filteredResults.map((r) => r.studentName)).size;
  const totalParticipations = filteredResults.length;

  const average =
    totalParticipations > 0
      ? filteredResults.reduce((sum, r) => sum + r.note, 0) / totalParticipations
      : 0;

  const bestScore =
    totalParticipations > 0 ? Math.max(...filteredResults.map((r) => r.note)) : 0;

  const successCount = filteredResults.filter((r) => r.note >= 10).length;
  const failCount = filteredResults.filter((r) => r.note < 10).length;

  const successRate =
    totalParticipations > 0 ? (successCount / totalParticipations) * 100 : 0;

  const quizzesWithResults = new Set(filteredResults.map((r) => r.quizId)).size;
  const quizzesWithoutResults = Math.max(quizzes.length - quizzesWithResults, 0);

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
          <span className={styles.badge}>{qid ? "Analyse du quiz" : "Analyse pédagogique"}</span>
          <h1>Statistiques</h1>
          <p>
            {qid
              ? "Analyse des résultats disponibles pour ce quiz."
              : "Analyse uniquement les quiz publiés arrivés à expiration ou déjà expirés."}
          </p>
        </div>

        {qid && (
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
          <select value={selectedClass} onChange={(e) => setSelectedClass(e.target.value)}>
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
            <option value="ALL">Toutes les matières</option>
            {subjects.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className={styles.statsGrid}>
        <StatCard icon={<BookOpen />} title={qid ? "Quiz sélectionné" : "Quiz clôturés publiés"} value={quizzes.length} color="blue" />
        <StatCard icon={<Users />} title="Étudiants participants" value={totalStudents} color="green" />
        <StatCard icon={<BarChart3 />} title="Participations" value={totalParticipations} color="purple" />
        <StatCard icon={<Target />} title="Moyenne générale" value={`${average.toFixed(2)}/20`} color="orange" />
        <StatCard icon={<Trophy />} title="Meilleure note" value={`${bestScore.toFixed(2)}/20`} color="yellow" />
        <StatCard icon={<CheckCircle2 />} title="Taux réussite" value={`${successRate.toFixed(1)}%`} color="green" />
        <StatCard icon={<AlertTriangle />} title="Quiz sans réponse" value={quizzesWithoutResults} color="red" />
        <StatCard icon={<Timer />} title="Quiz analysés" value={quizzesWithResults} color="blue" />
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
          {totalParticipations === 0 ? (
            <p className={styles.emptyText}>Aucune participation disponible.</p>
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
