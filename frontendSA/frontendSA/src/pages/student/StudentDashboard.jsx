import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BookOpen,
  CheckCircle2,
  Activity,
  BarChart3,
  Trophy,
  Award,
  TrendingUp,
  Clock,
  XCircle,
  GraduationCap,
  IdCard,
  Mail,
  UserRound,
} from "lucide-react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  Cell,
} from "recharts";

import studentQuizApi from "../../api/studentQuizApi";
import useAuth from "../../hooks/useAuth";
import styles from "./StudentDashboard.module.css";

const COLORS = ["#4F46E5", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899"];

const unwrap = (res) => res?.data?.data ?? res?.data ?? [];

const getSubjectName = (item) =>
  item?.matiereNom ||
  item?.matiereName ||
  item?.subjectName ||
  item?.quizTheme ||
  item?.theme ||
  item?.matiere?.nom ||
  "Matière générale";

const getScore = (item) => {
  const raw =
    item?.scorePercentage ??
    item?.percentage ??
    item?.score ??
    (item?.noteSur20 != null ? Number(item.noteSur20) * 5 : 0);

  const value = Number(raw);
  return Number.isFinite(value) ? Math.round(value) : 0;
};

const PASSING_SCORE = 50;

const isFailedResult = (item) => {
  const grade = `${item?.grade ?? ""}`.trim().toUpperCase();
  return grade === "F" || getScore(item) < PASSING_SCORE;
};

const getQuizEndDate = (quiz) =>
  quiz?.availableUntil ||
  quiz?.endDate ||
  quiz?.dateFin ||
  quiz?.expirationDate ||
  quiz?.deadline ||
  quiz?.dateExpiration ||
  quiz?.expiresAt ||
  quiz?.endAt ||
  null;

const getQuizStartDate = (quiz) =>
  quiz?.availableFrom ||
  quiz?.startDate ||
  quiz?.dateDebut ||
  quiz?.debut ||
  quiz?.startsAt ||
  null;

const normalizeStatus = (status) =>
  `${status || ""}`
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const isQuizCurrentlyAvailable = (quiz) => {
  const now = new Date();
  const startDate = getQuizStartDate(quiz);
  const endDate = getQuizEndDate(quiz);
  const parsedStart = startDate ? new Date(startDate) : null;
  const parsedEnd = endDate ? new Date(endDate) : null;
  const status = normalizeStatus(quiz?.status);

  if (quiz?.sessionExists || quiz?.started || quiz?.hasStarted || quiz?.inProgress) {
    return false;
  }
  if (status.includes("expir") || status.includes("termin") || status.includes("complete")) {
    return false;
  }
  if (parsedStart && !Number.isNaN(parsedStart.getTime()) && now < parsedStart) {
    return false;
  }
  if (parsedEnd && !Number.isNaN(parsedEnd.getTime()) && now > parsedEnd) {
    return false;
  }
  if (Number(quiz?.timeRemainingSeconds) === 0 && parsedEnd) {
    return false;
  }

  return true;
};

const isQuizExpired = (quiz) => {
  const status = String(quiz?.status || "").toUpperCase();
  if (status === "EXPIRED") return true;

  const endDate = getQuizEndDate(quiz);
  if (!endDate) return false;

  const parsedEndDate = new Date(endDate);
  return !Number.isNaN(parsedEndDate.getTime()) && parsedEndDate <= new Date();
};

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem("user") || "{}");
  } catch {
    return {};
  }
};

const normalizeStudentInfo = (source = {}) => {
  const nameFromParts = [source.firstName, source.lastName].filter(Boolean).join(" ");
  const fullName =
    source.fullName ||
    nameFromParts ||
    source.username ||
    "";
  const [derivedFirstName, ...derivedLastNameParts] = `${fullName || ""}`
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  const firstName = source.firstName || derivedFirstName || "";
  const lastName = source.lastName || derivedLastNameParts.join(" ");
  const classLabel = [
    source.classeName || source.className || source.classe?.name,
    source.classFiliere || source.classe?.filiere,
    source.classNiveau || source.classe?.niveau,
  ]
    .filter(Boolean)
    .join(" - ");

  return {
    id: source.id || source.userId || "",
    fullName: [firstName, lastName].filter(Boolean).join(" ") || fullName || "Étudiant",
    email: source.email || "-",
    cne: source.cne || "",
    codeApoge: source.codeApoge || source.codeApogee || "",
    classLabel,
  };
};

const formatDate = (value) => {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleDateString("fr-FR");
};

const formatDateTime = (value) => {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("fr-FR");
};

export default function StudentDashboard() {
  const navigate = useNavigate();
  const auth = useAuth() || {};

  const studentInfo = normalizeStudentInfo({
    ...readStoredUser(),
    ...(auth.user || {}),
  });

  const [stats, setStats] = useState({
    totalQuizzes: 0,
    completedQuizzes: 0,
    failedQuizzes: 0,
    averageScore: 0,
    bestScore: 0,
    totalAttempts: 0,
    ranking: 0,
  });

  const [recentQuizzes, setRecentQuizzes] = useState([]);
  const [failedQuizList, setFailedQuizList] = useState([]);
  const [performanceBySubject, setPerformanceBySubject] = useState([]);
  const [weeklyActivity, setWeeklyActivity] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    try {
      setLoading(true);

      const [availableQuizzes, historyRes, perfRes] = await Promise.all([
        studentQuizApi.getAvailableQuizzes().catch(() => []),
        studentQuizApi.getMyResultsHistory(),
        studentQuizApi.getMyPerformance().catch(() => ({})),
      ]);

      const history = unwrap(historyRes);
      const perf = unwrap(perfRes);
      const safeAvailableQuizzes = Array.isArray(availableQuizzes)
        ? availableQuizzes.filter(isQuizCurrentlyAvailable)
        : [];

      const completedHistory = Array.isArray(history)
        ? history.filter((item) => item.isCompleted !== false)
        : [];

      const nowDate = new Date();

      const failedByResultData = completedHistory
        .filter(isFailedResult)
        .map((item) => ({
          id: item.id || item.quizId,
          subject: getSubjectName(item),
          title: item.quizTitle || item.titre || item.title || "Quiz",
          date: item.completedDate || item.submittedAt || item.createdAt || null,
          startDate: item.startedAt || null,
          endDate: item.completedDate || item.submittedAt || item.createdAt || null,
          score: getScore(item),
          statusLabel: "Score insuffisant",
        }));

      const missedQuizListData = Array.isArray(availableQuizzes)
        ? availableQuizzes
            .filter((quiz) => {
              const endDate = getQuizEndDate(quiz);
              if (!endDate) return false;

              const parsedEndDate = new Date(endDate);
              if (Number.isNaN(parsedEndDate.getTime())) return false;

              const expired = parsedEndDate < nowDate;

              const alreadyAnswered = completedHistory.some(
                (h) =>
                  String(h.quizId) === String(quiz.id) ||
                  String(h.quizId) === String(quiz.quizId) ||
                  String(h.idQuiz) === String(quiz.id)
              );

              return expired && !alreadyAnswered;
            })
            .map((quiz) => ({
              id: quiz.id || quiz.quizId,
              subject:
                quiz.matiereNom ||
                quiz.matiereName ||
                quiz.subjectName ||
                quiz.matiere?.nom ||
                "Matière générale",
              title: quiz.titre || quiz.title || quiz.quizTitle || "Quiz",
              date:
                quiz.dateQuiz ||
                quiz.quizDate ||
                quiz.date ||
                quiz.createdAt ||
                null,
              startDate:
                quiz.startDate ||
                quiz.dateDebut ||
                quiz.debut ||
                quiz.startsAt ||
                null,
              endDate: getQuizEndDate(quiz),
              score: null,
              statusLabel: "Non repondu",
            }))
        : [];

      const failedQuizListData = [...failedByResultData, ...missedQuizListData];
      const failedQuizzes = failedQuizListData.length;

      let bestRanking = 0;

      if (completedHistory.length > 0) {
        const rankingEligibleHistory = await Promise.all(
          completedHistory
            .filter((h) => h.quizId)
            .map(async (h) => {
              if (isQuizExpired(h)) return h;

              const quizDetails = await studentQuizApi
                .getQuizDetails(h.quizId)
                .catch(() => null);

              return isQuizExpired(quizDetails) ? h : null;
            })
        );

        const rankings = await Promise.all(
          rankingEligibleHistory
            .filter(Boolean)
            .map((h) =>
              studentQuizApi
                .getRanking(h.quizId)
                .catch(() => null)
            )
        );

        const validRankings = rankings.filter((r) => r?.rang || r?.rank);
        if (validRankings.length > 0) {
          bestRanking = Math.min(
            ...validRankings.map((r) => Number(r.rang || r.rank))
          );
        }
      }

      const subjectMap = {};
      completedHistory.forEach((item) => {
        const subject = getSubjectName(item);

        if (!subjectMap[subject]) {
          subjectMap[subject] = { total: 0, count: 0 };
        }

        subjectMap[subject].total += getScore(item);
        subjectMap[subject].count += 1;
      });

      const subjectPerformance = Object.entries(subjectMap)
        .slice(0, 6)
        .map(([subject, data], index) => ({
          subject: subject.length > 14 ? `${subject.slice(0, 14)}…` : subject,
          score: data.count > 0 ? Math.round(data.total / data.count) : 0,
          color: COLORS[index % COLORS.length],
        }));

      const weekDays = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"];
      const activityCount = [0, 0, 0, 0, 0, 0, 0];
      const now = new Date();

      completedHistory.forEach((item) => {
        const dateValue = item.completedDate || item.submittedAt || item.createdAt;
        if (!dateValue) return;

        const date = new Date(dateValue);
        if (Number.isNaN(date.getTime())) return;

        const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));
        if (diffDays < 7) {
          const dayIndex = (date.getDay() + 6) % 7;
          activityCount[dayIndex] += 1;
        }
      });

      const recent = completedHistory.slice(0, 4).map((item) => ({
        title: item.quizTitle || item.titre || item.title || "Quiz",
        score: getScore(item),
        date: item.completedDate
          ? new Date(item.completedDate).toLocaleDateString("fr-FR")
          : "—",
      }));

      setStats({
        totalQuizzes: safeAvailableQuizzes.length,
        completedQuizzes: completedHistory.length,
        failedQuizzes,
        averageScore: Math.round(perf?.moyenneScore || 0),
        bestScore: Math.round(perf?.meilleurScore || 0),
        totalAttempts: completedHistory.length,
        ranking: bestRanking,
      });

      setPerformanceBySubject(subjectPerformance);
      setWeeklyActivity(weekDays.map((day, i) => ({ day, attempts: activityCount[i] })));
      setRecentQuizzes(recent);
      setFailedQuizList(failedQuizListData);
    } catch (error) {
      console.error("Erreur dashboard étudiant :", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDashboard();
  }, [loadDashboard]);

  const cards = useMemo(
    () => [
      {
        label: "Quiz disponibles",
        value: stats.totalQuizzes,
        icon: BookOpen,
        hint: "à compléter",
        color: "blue",
      },
      {
        label: "Quiz terminés",
        value: stats.completedQuizzes,
        icon: CheckCircle2,
        hint: "complétés",
        color: "green",
      },
      {
        label: "Quiz ratés",
        value: stats.failedQuizzes,
        icon: XCircle,
        hint: "score ou date depassee",
        color: "red",
      },
      {
        label: "Moyenne générale",
        value: `${stats.averageScore}%`,
        icon: BarChart3,
        hint: "score général",
        color: "purple",
      },
      {
        label: "Total tentatives",
        value: stats.totalAttempts,
        icon: Activity,
        hint: "quiz passés",
        color: "cyan",
      },
      {
        label: "Classement",
        value: stats.ranking > 0 ? `#${stats.ranking}` : "—",
        icon: Trophy,
        hint: "meilleur rang",
        color: "orange",
      },
      {
        label: "Meilleur score",
        value: `${stats.bestScore}%`,
        icon: Award,
        hint: "meilleure tentative",
        color: "pink",
      },
    ],
    [stats]
  );

  if (loading) {
    return <div className={styles.loading}>Chargement...</div>;
  }

  return (
    <div className={styles.page}>
      <section className={styles.topSection}>
        <div className={styles.studentIntro}>
          <p className={styles.hello}>Bonjour {studentInfo.fullName} 👋</p>
          <p className={styles.subtitle}>
            Résumé de vos quiz, résultats et activités.
          </p>
          <div className={styles.studentDetails}>
            <span>
              <UserRound size={16} />
              ID: {studentInfo.id || "-"}
            </span>
            <span>
              <Mail size={16} />
              {studentInfo.email}
            </span>
            <span>
              <IdCard size={16} />
              CNE: {studentInfo.cne || "-"}
            </span>
            <span>
              <IdCard size={16} />
              Apogée: {studentInfo.codeApoge || "-"}
            </span>
            <span>
              <GraduationCap size={16} />
              {studentInfo.classLabel || "Classe non renseignée"}
            </span>
          </div>
        </div>

        <div className={styles.todayBox}>
          <span>Aujourd'hui</span>
          <strong>{new Date().toLocaleDateString("fr-FR")}</strong>
        </div>
      </section>

      <section className={styles.statsGrid}>
        {cards.map((card) => {
          const Icon = card.icon;

          return (
            <div className={styles.statCard} key={card.label}>
              <div className={`${styles.statIcon} ${styles[card.color]}`}>
                <Icon size={22} />
              </div>

              <div>
                <span>{card.label}</span>
                <strong>{card.value}</strong>
                <p>{card.hint}</p>
              </div>
            </div>
          );
        })}
      </section>

      <section className={styles.analyticsGrid}>
        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Progression par matière</h2>
              <p>Score moyen par matière.</p>
            </div>
          </div>

          <div className={styles.chartBox}>
            {performanceBySubject.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={performanceBySubject}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="subject" />
                  <YAxis domain={[0, 100]} />
                  <Tooltip formatter={(value) => `${value}%`} />
                  <Bar dataKey="score" radius={[14, 14, 0, 0]}>
                    {performanceBySubject.map((entry, index) => (
                      <Cell key={index} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className={styles.emptyBox}>
                Aucune donnée de performance disponible.
              </div>
            )}
          </div>
        </div>

        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Activité hebdomadaire</h2>
              <p>Nombre de quiz complétés cette semaine.</p>
            </div>
          </div>

          <div className={styles.chartBox}>
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={weeklyActivity}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="day" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="attempts"
                  stroke="#4F46E5"
                  strokeWidth={3}
                  dot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      <section className={styles.failedSection}>
        <div className={styles.cardHeader}>
          <div>
            <h2>Quiz ratés</h2>
            <p>Quiz echoues par score ou non repondus apres la date de fin.</p>
          </div>
        </div>

        {failedQuizList.length > 0 ? (
          <div className={styles.tableWrapper}>
            <table className={styles.failedTable}>
              <thead>
                <tr>
                  <th>Matière</th>
                  <th>Titre</th>
                  <th>Statut</th>
                  <th>Score</th>
                  <th>Date du quiz</th>
                  <th>Début</th>
                  <th>Fin</th>
                </tr>
              </thead>

              <tbody>
                {failedQuizList.map((quiz) => (
                  <tr key={quiz.id}>
                    <td>{quiz.subject}</td>
                    <td>{quiz.title}</td>
                    <td>{quiz.statusLabel}</td>
                    <td>{quiz.score == null ? "-" : `${quiz.score}%`}</td>
                    <td>{formatDate(quiz.date)}</td>
                    <td>{formatDateTime(quiz.startDate)}</td>
                    <td>{formatDateTime(quiz.endDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className={styles.emptyBox}>
            Aucun quiz raté pour le moment.
          </div>
        )}
      </section>

      <section className={styles.bottomGrid}>
        <div className={styles.panel}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Derniers quiz passés</h2>
              <p>Activité récente de vos tentatives.</p>
            </div>

            <button onClick={() => navigate("/student/history")}>
              Voir tout
            </button>
          </div>

          <div className={styles.quizList}>
            {recentQuizzes.length > 0 ? (
              recentQuizzes.map((quiz, index) => (
                <div className={styles.quizItem} key={index}>
                  <div className={styles.quizIcon}>
                    <BookOpen size={20} />
                  </div>

                  <div>
                    <h3>{quiz.title}</h3>
                    <p>
                      {quiz.date} • {quiz.score}%
                    </p>
                  </div>

                  <span className={styles.scorePill}>{quiz.score}%</span>
                </div>
              ))
            ) : (
              <div className={styles.emptyBox}>
                Aucun quiz passé pour le moment.
              </div>
            )}
          </div>
        </div>

        <div className={styles.activityCard}>
          <h2>Indicateurs utiles</h2>

          <div className={styles.activityItem}>
            <CheckCircle2 size={18} />
            <span>{stats.completedQuizzes} quiz complétés</span>
          </div>

          <div className={styles.activityItem}>
            <XCircle size={18} />
            <span>{stats.failedQuizzes} quiz ratés</span>
          </div>

          <div className={styles.activityItem}>
            <Clock size={18} />
            <span>{stats.totalQuizzes} quiz disponibles</span>
          </div>

          <div className={styles.activityItem}>
            <TrendingUp size={18} />
            <span>Moyenne : {stats.averageScore}%</span>
          </div>

          <div className={styles.activityItem}>
            <Trophy size={18} />
            <span>
              Meilleur rang : {stats.ranking > 0 ? `#${stats.ranking}` : "—"}
            </span>
          </div>
        </div>
      </section>
    </div>
  );
}
