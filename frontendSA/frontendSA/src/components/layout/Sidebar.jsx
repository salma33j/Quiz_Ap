import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  BookOpen,
  PlusCircle,
  Brain,
  BarChart3,
  Trophy,
  Users,
  User,
  LogOut,
  ChevronLeft,
  ChevronRight,
  PieChart,
} from "lucide-react";
import LogoutConfirmDialog from "../common/LogoutConfirmDialog";
import styles from "./Sidebar.module.css";

export default function Sidebar({ collapsed, setCollapsed }) {
  const navigate = useNavigate();
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const menu = [
    { label: "Dashboard", path: "/teacher/dashboard", icon: LayoutDashboard },
    { label: "Mes quiz", path: "/teacher/quizzes", icon: BookOpen, end: true },
    { label: "Créer quiz", path: "/teacher/quizzes/create", icon: PlusCircle },
    { label: "Quiz IA", path: "/teacher/ai-generator", icon: Brain },
    { label: "Résultats", path: "/teacher/results", icon: BarChart3 },
    { label: "Classement", path: "/teacher/ranking", icon: Trophy },
    { label: "Statistiques", path: "/teacher/statistics", icon: PieChart },
    { label: "Étudiants", path: "/teacher/students", icon: Users },
  ];

  const logout = () => {
    localStorage.clear();
    navigate("/login");
  };

  const goToProfile = () => {
    navigate("/teacher/profile");
  };

  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ""}`}>
      {/* Top section - Logo et toggle */}
      <div className={styles.topArea}>
        <button
          type="button"
          className={styles.logoBox}
          onClick={() => navigate("/teacher/dashboard")}
        >
          <img src="/logo.png" alt="QuizApp" />
          {!collapsed && (
            <div>
              <h2>QuizApp</h2>
              <p>Espace enseignant</p>
            </div>
          )}
        </button>

        <button
          className={styles.toggleBtn}
          onClick={() => setCollapsed(!collapsed)}
          type="button"
        >
          {collapsed ? <ChevronRight size={20} /> : <ChevronLeft size={20} />}
        </button>
      </div>

      {/* Navigation - Menu principal */}
      <nav className={styles.nav}>
        {menu.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              title={collapsed ? item.label : ""}
              className={({ isActive }) =>
                isActive ? `${styles.link} ${styles.active}` : styles.link
              }
            >
              <Icon size={20} />
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          );
        })}
      </nav>

      {/* Bottom section - Actions utilisateur */}
      <div className={styles.bottomActions}>
        <button className={styles.profileBtn} onClick={goToProfile}>
          <User size={20} />
          {!collapsed && <span>Mon profil</span>}
        </button>

        <button
          type="button"
          className={styles.logout}
          onClick={() => setShowLogoutConfirm(true)}
        >
          <LogOut size={20} />
          {!collapsed && <span>Déconnexion</span>}
        </button>
      </div>

      <LogoutConfirmDialog
        open={showLogoutConfirm}
        onCancel={() => setShowLogoutConfirm(false)}
        onConfirm={logout}
      />
    </aside>
  );
}
