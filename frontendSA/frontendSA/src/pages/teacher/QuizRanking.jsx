import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  Filter,
  GraduationCap,
  Medal,
  Search,
  Target,
  TrendingDown,
  Trophy,
  Users,
} from "lucide-react";
import axiosInstance from "../../api/axiosInstance";
import styles from "./QuizRanking.module.css";

const unwrap = (res) => res.data?.data ?? res.data;

const getMention = (note) => {
  const n = Number(note || 0);

  if (n >= 16) return "Très bien";
  if (n >= 14) return "Bien";
  if (n >= 12) return "Assez bien";
  if (n >= 10) return "Passable";
  return "Insuffisant";
};

const getNoteSur20 = (item) => {
  if (item.noteSur20 != null) return Number(item.noteSur20);
  if (item.note != null) return Number(item.note);
  if (item.grade != null && !Number.isNaN(Number(item.grade))) return Number(item.grade);
  if (item.scorePercentage != null) return (Number(item.scorePercentage) * 20) / 100;

  const earned = Number(item.earnedPoints ?? item.pointsObtenus);
  const total = Number(item.totalPoints ?? item.total);
  if (Number.isFinite(earned) && Number.isFinite(total) && total > 0) {
    return (earned * 20) / total;
  }

  return 0;
};

const getStudentParts = (item) => {
  const fullName = item.studentName || "";
  const splitName = fullName.trim().split(/\s+/).filter(Boolean);

  return {
    firstName: item.studentFirstName || item.firstName || splitName[0] || "",
    lastName:
      item.studentLastName ||
      item.lastName ||
      splitName.slice(1).join(" ") ||
      "",
  };
};

const getClassName = (item, quiz) =>
  item.className ||
  item.classeName ||
  item.classe ||
  item.groupName ||
  quiz?.className ||
  quiz?.classeName ||
  quiz?.classe ||
  "Groupe non défini";

const getSubjectName = (item, quiz) =>
  item.subjectName ||
  item.matiereName ||
  item.matiereNom ||
  quiz?.matiereName ||
  quiz?.matiereNom ||
  quiz?.subjectName ||
  item.quizTheme ||
  quiz?.theme ||
  "Matiere non definie";
const isAnalyticsAvailable = (quiz) => {
  const status = String(quiz.status || "").toUpperCase();
  if (status === "EXPIRED") return true;
  if (status !== "PUBLISHED" || !quiz.availableUntil) return false;

  const expirationDate = new Date(quiz.availableUntil);
  return !Number.isNaN(expirationDate.getTime()) && expirationDate <= new Date();
};

const averageOf = (items, key = "note") => {
  if (items.length === 0) return 0;
  return items.reduce((sum, item) => sum + Number(item[key] || 0), 0) / items.length;
};

export default function QuizRanking() {
  const { quizId, id } = useParams();
  const qid = quizId || id;
  const [results, setResults] = useState([]);
  const [search, setSearch] = useState("");
  const [selectedClass, setSelectedClass] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchRanking = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const quizRes = qid
        ? await axiosInstance.get(`/teacher/quizzes/${qid}`)
        : await axiosInstance.get("/teacher/quizzes");
      const quizData = unwrap(quizRes);
      const quizzes = qid
        ? [quizData || { id: qid }]
        : Array.isArray(quizData)
          ? quizData
          : [];
      const visibleQuizzes = quizzes.filter(isAnalyticsAvailable);

      if (qid && visibleQuizzes.length === 0) {
        setError("Le classement sera disponible après la date d'expiration du quiz.");
        setResults([]);
        return;
      }

      const allResults = [];

      for (const quiz of visibleQuizzes) {
        try {
          const res = await axiosInstance.get(`/resultats/quiz/${quiz.id}`);
          const quizResults = Array.isArray(unwrap(res)) ? unwrap(res) : [];

          quizResults.forEach((item) => {
            const student = getStudentParts(item);

            allResults.push({
              id: item.id,
              quizId: quiz.id,
              quizTitle: item.quizTitle || quiz.titre || quiz.title || "Quiz sans titre",
              studentId: item.studentId,
              className: getClassName(item, quiz),
              subjectName: getSubjectName(item, quiz),
              cne: item.cne || item.studentCne || "",
              codeApogee:
                item.codeApogee ||
                item.codeApoge ||
                item.studentCodeApogee ||
                item.studentCodeApoge ||
                "",
              lastName: student.lastName,
              firstName: student.firstName,
              studentName: item.studentName || "",
              note: getNoteSur20(item),
            });
          });
        } catch (err) {
          console.warn("Erreur résultats quiz :", quiz.id, err);
        }
      }

      setResults(allResults);
    } catch (err) {
      console.error("Erreur classement :", err);
      setError("Impossible de charger le classement depuis le backend.");
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, [qid]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      fetchRanking();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [fetchRanking]);

  const classes = useMemo(() => {
    return [...new Set(results.map((s) => s.className))].sort();
  }, [results]);

  const scopedResults = useMemo(() => {
    return results.filter((r) => selectedClass === "ALL" || r.className === selectedClass);
  }, [results, selectedClass]);

  const studentRanking = useMemo(() => {
    const map = {};

    scopedResults.forEach((r) => {
      const key = r.studentId || r.cne || `${r.className}-${r.firstName}-${r.lastName}`;

      if (!map[key]) {
        map[key] = {
          key,
          studentId: r.studentId,
          cne: r.cne,
          codeApogee: r.codeApogee,
          firstName: r.firstName || r.studentName?.split(" ")[0] || "",
          lastName:
            r.lastName ||
            r.studentName?.split(" ").slice(1).join(" ") ||
            "",
          className: r.className,
          subjects: new Set(),
          total: 0,
          count: 0,
          bestNote: r.note,
          worstNote: r.note,
        };
      }

      map[key].subjects.add(r.subjectName);
      map[key].total += Number.isFinite(r.note) ? r.note : 0;
      map[key].count += 1;
      map[key].bestNote = Math.max(map[key].bestNote, r.note);
      map[key].worstNote = Math.min(map[key].worstNote, r.note);
    });

    return Object.values(map).map((s) => ({
      ...s,
      subjectName: Array.from(s.subjects).join(", "),
      average: s.count > 0 ? s.total / s.count : 0,
    }));
  }, [scopedResults]);

  const ranking = useMemo(() => {
    const value = search.trim().toLowerCase();

    return studentRanking.filter((s) =>
      [
        s.className,
        s.subjectName,
        s.cne,
        s.codeApogee,
        s.lastName,
        s.firstName,
        s.average,
        getMention(s.average),
      ]
        .join(" ")
        .toLowerCase()
        .includes(value)
    );
  }, [studentRanking, search]);

  const rankingGroups = useMemo(() => {
    const map = new Map();

    ranking.forEach((student) => {
      if (!map.has(student.className)) {
        map.set(student.className, []);
      }
      map.get(student.className).push(student);
    });

    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b, "fr", { sensitivity: "base" }))
      .map(([className, students]) => ({
        className,
        students: students
          .sort((a, b) => b.average - a.average)
          .map((student, index) => ({
            ...student,
            rank: index + 1,
          })),
      }));
  }, [ranking]);

  const subjectAverages = useMemo(() => {
    const map = {};

    scopedResults.forEach((r) => {
      if (!map[r.subjectName]) map[r.subjectName] = [];
      map[r.subjectName].push(r);
    });

    return Object.entries(map)
      .map(([name, rows]) => ({
        name,
        count: rows.length,
        average: averageOf(rows),
      }))
      .sort((a, b) => b.average - a.average);
  }, [scopedResults]);

  const classAverages = useMemo(() => {
    const map = {};

    results.forEach((r) => {
      if (!map[r.className]) map[r.className] = [];
      map[r.className].push(r);
    });

    return Object.entries(map)
      .map(([name, rows]) => ({
        name,
        count: rows.length,
        average: averageOf(rows),
      }))
      .sort((a, b) => b.average - a.average);
  }, [results]);

  const bestNote =
    scopedResults.length > 0 ? Math.max(...scopedResults.map((result) => result.note)) : 0;
  const worstNote =
    scopedResults.length > 0 ? Math.min(...scopedResults.map((result) => result.note)) : 0;
  const overallAverage = averageOf(scopedResults);

  const getMentionClass = (note) => {
    const n = Number(note || 0);

    if (n >= 16) return styles.excellent;
    if (n >= 14) return styles.good;
    if (n >= 12) return styles.medium;
    if (n >= 10) return styles.passable;
    return styles.bad;
  };

  const getRankIcon = (rank) => {
    if (rank === 1) return <Medal className={styles.gold} size={22} />;
    if (rank === 2) return <Medal className={styles.silver} size={22} />;
    if (rank === 3) return <Medal className={styles.bronze} size={22} />;
    return <span className={styles.rankNumber}>{rank}</span>;
  };

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.loadingBox}>
          <div className={styles.spinner}></div>
          <p>Chargement du classement...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <span className={styles.badge}>Classement</span>
          <h1>{qid ? "Classement du quiz" : "Classement des étudiants"}</h1>
          <p>Classement calculé dans chaque classe, avec moyennes par matière et par groupe.</p>
        </div>

        {qid && (
          <Link className={styles.backBtn} to="/teacher/quizzes">
            <ArrowLeft size={18} />
            Mes quiz
          </Link>
        )}
      </div>

      <div className={styles.statsGrid}>
        <div className={styles.statCard}>
          <div className={styles.statIconBlue}>
            <Users size={24} />
          </div>
          <div>
            <h3>{ranking.length}</h3>
            <p>Étudiants affichés</p>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIconPurple}>
            <GraduationCap size={24} />
          </div>
          <div>
            <h3>{selectedClass === "ALL" ? classes.length : 1}</h3>
            <p>Classe(s)</p>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIconGreen}>
            <Target size={24} />
          </div>
          <div>
            <h3>{overallAverage.toFixed(2)}</h3>
            <p>Moyenne générale</p>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIconOrange}>
            <Trophy size={24} />
          </div>
          <div>
            <h3>{bestNote.toFixed(2)}</h3>
            <p>Meilleure note</p>
          </div>
        </div>

        <div className={styles.statCard}>
          <div className={styles.statIconRed}>
            <TrendingDown size={24} />
          </div>
          <div>
            <h3>{worstNote.toFixed(2)}</h3>
            <p>Plus faible note</p>
          </div>
        </div>
      </div>

      <div className={styles.toolsBar}>
        <div className={styles.searchBox}>
          <Search size={20} />
          <input
            type="text"
            placeholder="Rechercher par nom, prénom, CNE, Apogée, matière, classe..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className={styles.selectBox}>
          <Filter size={18} />
          <select
            value={selectedClass}
            onChange={(e) => setSelectedClass(e.target.value)}
          >
            <option value="ALL">Toutes les classes</option>
            {classes.map((className) => (
              <option key={className} value={className}>
                {className}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <div className={styles.errorBox}>{error}</div>}

      <div className={styles.averageGrid}>
        <section className={styles.averageCard}>
          <div className={styles.averageHeader}>
            <BookOpen size={20} />
            <h2>Moyenne par matière</h2>
          </div>

          {subjectAverages.length === 0 ? (
            <p className={styles.muted}>Aucune matière disponible.</p>
          ) : (
            <div className={styles.averageList}>
              {subjectAverages.map((item) => (
                <div className={styles.averageItem} key={item.name}>
                  <div>
                    <strong>{item.name}</strong>
                    <span>{item.count} résultat(s)</span>
                  </div>
                  <b>{item.average.toFixed(2)}/20</b>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className={styles.averageCard}>
          <div className={styles.averageHeader}>
            <GraduationCap size={20} />
            <h2>Moyenne par classe</h2>
          </div>

          {classAverages.length === 0 ? (
            <p className={styles.muted}>Aucune classe disponible.</p>
          ) : (
            <div className={styles.averageList}>
              {classAverages.map((item) => (
                <div className={styles.averageItem} key={item.name}>
                  <div>
                    <strong>{item.name}</strong>
                    <span>{item.count} résultat(s)</span>
                  </div>
                  <b>{item.average.toFixed(2)}/20</b>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      {rankingGroups.length === 0 ? (
        <div className={styles.emptyBox}>
          <BookOpen size={46} />
          <h3>Aucun classement trouvé</h3>
          <p>Aucun étudiant ne correspond à votre recherche.</p>
        </div>
      ) : (
        <div className={styles.classRankingList}>
          {rankingGroups.map((group) => (
            <section className={styles.card} key={group.className}>
              <div className={styles.cardHeader}>
                <div>
                  <span>Classement dans la classe</span>
                  <h2>{group.className}</h2>
                </div>
                <span>{group.students.length} étudiant(s)</span>
              </div>

              <div className={styles.tableWrapper}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>Rang</th>
                      <th>CNE</th>
                      <th>Code Apogée</th>
                      <th>Nom</th>
                      <th>Prénom</th>
                      <th>Matières</th>
                      <th>Moyenne</th>
                      <th>Meilleure</th>
                      <th>Faible</th>
                      <th>Mention</th>
                    </tr>
                  </thead>

                  <tbody>
                    {group.students.map((student) => (
                      <tr key={student.key}>
                        <td>
                          <div className={styles.rankCell}>
                            {getRankIcon(student.rank)}
                          </div>
                        </td>

                        <td>{student.cne || "-"}</td>
                        <td>{student.codeApogee || "-"}</td>
                        <td>{student.lastName || "-"}</td>
                        <td>{student.firstName || "-"}</td>
                        <td>{student.subjectName}</td>

                        <td>
                          <span className={styles.noteBadge}>
                            {student.average.toFixed(2)}/20
                          </span>
                        </td>
                        <td>{student.bestNote.toFixed(2)}/20</td>
                        <td>{student.worstNote.toFixed(2)}/20</td>

                        <td>
                          <span
                            className={`${styles.mentionBadge} ${getMentionClass(
                              student.average
                            )}`}
                          >
                            {getMention(student.average)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
