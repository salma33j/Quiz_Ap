import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  User,
  Mail,
  Calendar,
  Award,
  BookOpen,
  BarChart3,
  Trophy,
  Star,
  Edit,
  ArrowLeft,
  CheckCircle2,
  TrendingUp,
  Users,
  LogOut,
  GraduationCap,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  Cell,
} from "recharts";

import studentApi from "../../api/studentApi";
import styles from "./StudentProfile.module.css";

const StudentProfile = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  
  const [profile, setProfile] = useState({
    id: "",
    firstName: "",
    lastName: "",
    email: "",
    role: "Étudiant",
    memberSince: "",
    avatar: "",
    stats: {
      completedQuizzes: 0,
      averageScore: 0,
      ranking: 0,
      bestScore: 0,
      totalStudents: 0,
    },
    performancesBySubject: [],
  });

  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      
      // Récupérer les informations du profil
      const userData = await studentApi.getProfile();
      
      // Récupérer les statistiques
      const statsData = await studentApi.getStats();
      
      // Récupérer les performances par matière
      const performancesData = await studentApi.getPerformancesBySubject();
      
      setProfile({
        id: userData?.id || "",
        firstName: userData?.firstName || "Karim",
        lastName: userData?.lastName || "Benali",
        email: userData?.email || "karim.benali@etu.quizapp.com",
        role: "Étudiant",
        memberSince: userData?.createdAt || "Septembre 2024",
        avatar: userData?.avatar || "",
        stats: {
          completedQuizzes: statsData?.completedQuizzes || 14,
          averageScore: statsData?.averageScore || 78,
          ranking: statsData?.ranking || 3,
          bestScore: statsData?.bestScore || 96,
          totalStudents: statsData?.totalStudents || 45,
        },
        performancesBySubject: performancesData?.length > 0 ? performancesData : [
          { subject: "Informatique", score: 91, color: "#10B981" },
          { subject: "Mathématiques", score: 85, color: "#4F46E5" },
          { subject: "Physique", score: 72, color: "#F59E0B" },
          { subject: "Anglais", score: 68, color: "#EF4444" },
        ],
      });
      
      setFormData({
        firstName: userData?.firstName || "Karim",
        lastName: userData?.lastName || "Benali",
        email: userData?.email || "karim.benali@etu.quizapp.com",
      });
      
    } catch (error) {
      console.error("Erreur chargement profil:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleEditToggle = () => {
    setIsEditing(!isEditing);
    if (isEditing) {
      setFormData({
        firstName: profile.firstName,
        lastName: profile.lastName,
        email: profile.email,
      });
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSaveProfile = async () => {
    try {
      await studentApi.updateProfile(formData);
      setProfile({
        ...profile,
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
      });
      setIsEditing(false);
    } catch (error) {
      console.error("Erreur mise à jour profil:", error);
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  // Récupérer l'initiale pour l'avatar
  const getInitial = () => {
    return profile.firstName?.charAt(0) || "K";
  };

  const cards = [
    {
      label: "Quiz terminés",
      value: profile.stats.completedQuizzes,
      icon: CheckCircle2,
      hint: "quiz complétés",
      color: "green",
    },
    {
      label: "Moyenne générale",
      value: `${profile.stats.averageScore}%`,
      icon: BarChart3,
      hint: "score général",
      color: "purple",
    },
    {
      label: "Rang classement",
      value: `#${profile.stats.ranking}`,
      icon: Trophy,
      hint: `sur ${profile.stats.totalStudents} étudiants`,
      color: "orange",
    },
    {
      label: "Meilleur score",
      value: `${profile.stats.bestScore}%`,
      icon: Star,
      hint: "meilleure tentative",
      color: "pink",
    },
  ];

  if (loading) {
    return <div className={styles.loading}>Chargement du profil...</div>;
  }

  return (
    <div className={styles.page}>
      {/* Bouton retour */}
      <button className={styles.backBtn} onClick={() => navigate("/student/dashboard")}>
        <ArrowLeft size={18} />
        Retour au tableau de bord
      </button>

      <div className={styles.profileContainer}>
        {/* Section gauche - Informations personnelles */}
        <div className={styles.leftPanel}>
          <div className={styles.profileCard}>
            <div className={styles.avatarContainer}>
              <div className={styles.avatar}>
                {profile.avatar ? (
                  <img src={profile.avatar} alt="Avatar" />
                ) : (
                  <span>{getInitial()}</span>
                )}
              </div>
            </div>

            {!isEditing ? (
              <>
                <h2 className={styles.fullName}>
                  {profile.firstName} {profile.lastName}
                </h2>
                <p className={styles.role}>{profile.role}</p>
                
                <div className={styles.infoList}>
                  <div className={styles.infoItem}>
                    <Mail size={18} />
                    <div>
                      <span>Email</span>
                      <p>{profile.email}</p>
                    </div>
                  </div>
                  <div className={styles.infoItem}>
                    <GraduationCap size={18} />
                    <div>
                      <span>Rôle</span>
                      <p>{profile.role}</p>
                    </div>
                  </div>
                  <div className={styles.infoItem}>
                    <Calendar size={18} />
                    <div>
                      <span>Membre depuis</span>
                      <p>{profile.memberSince}</p>
                    </div>
                  </div>
                </div>

                <button className={styles.editBtn} onClick={handleEditToggle}>
                  <Edit size={18} />
                  Modifier mon profil
                </button>

                <button className={styles.logoutBtn} onClick={handleLogout}>
                  <LogOut size={18} />
                  Déconnexion
                </button>
              </>
            ) : (
              <div className={styles.editForm}>
                <h2>Modifier le profil</h2>
                <div className={styles.formGroup}>
                  <label>Prénom</label>
                  <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleInputChange}
                    placeholder="Votre prénom"
                  />
                </div>
                <div className={styles.formGroup}>
                  <label>Nom</label>
                  <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleInputChange}
                    placeholder="Votre nom"
                  />
                </div>
                <div className={styles.formGroup}>
                  <label>Email</label>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    placeholder="votre@email.com"
                  />
                </div>
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={handleEditToggle}>
                    Annuler
                  </button>
                  <button className={styles.saveBtn} onClick={handleSaveProfile}>
                    Enregistrer
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Section droite - Statistiques et performances */}
        <div className={styles.rightPanel}>
          {/* Statistiques */}
          <div className={styles.statsCard}>
            <h2>Statistiques</h2>
            <div className={styles.statsGrid}>
              {cards.map((card) => {
                const Icon = card.icon;
                return (
                  <div className={styles.statItem} key={card.label}>
                    <div className={`${styles.statIcon} ${styles[card.color]}`}>
                      <Icon size={20} />
                    </div>
                    <div>
                      <strong>{card.value}</strong>
                      <span>{card.label}</span>
                      <p>{card.hint}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Performances par matière */}
          <div className={styles.performancesCard}>
            <h2>Performances par matière</h2>
            <div className={styles.chartContainer}>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart
                  data={profile.performancesBySubject}
                  layout="vertical"
                  margin={{ top: 20, right: 30, left: 80, bottom: 20 }}
                >
                  <CartesianGrid strokeDasharray="3 3" horizontal={true} />
                  <XAxis type="number" domain={[0, 100]} />
                  <YAxis type="category" dataKey="subject" />
                  <Tooltip
                    formatter={(value) => [`${value}%`, 'Score']}
                    contentStyle={{
                      borderRadius: '12px',
                      border: '1px solid #e5e7eb',
                      backgroundColor: 'white',
                    }}
                  />
                  <Bar dataKey="score" radius={[0, 8, 8, 0]}>
                    {profile.performancesBySubject.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color || "#4F46E5"} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudentProfile;