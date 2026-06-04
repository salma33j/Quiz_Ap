import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import {
  BarChart3,
  BookOpen,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  GraduationCap,
  History,
  LayoutDashboard,
  LogOut,
  User,
} from "lucide-react";
import LogoutConfirmDialog from "../common/LogoutConfirmDialog";
import styles from "./StudentSidebar.module.css";

const menu = [
  { label: "Dashboard", path: "/student/dashboard", icon: LayoutDashboard },
  { label: "Quiz disponibles", path: "/student/available-quizzes", icon: BookOpen },
  { label: "Historique", path: "/student/history", icon: History },
  { label: "Performances", path: "/student/performance", icon: BarChart3 },
  { label: "Corrections", path: "/student/corrections", icon: CheckCircle2 },
];

export default function StudentSidebar({ collapsed, setCollapsed }) {
  const navigate = useNavigate();
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const logout = () => {
    localStorage.clear();
    navigate("/login");
  };

  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ""}`}>
      <button
        type="button"
        className={styles.brand}
        onClick={() => navigate("/student/dashboard")}
      >
        <img src="/logo.png" alt="QuizApp" />
        {!collapsed && (
          <div>
            <h2>QuizApp</h2>
            <p>Espace etudiant</p>
          </div>
        )}
      </button>

      <button
        type="button"
        className={styles.toggle}
        onClick={() => setCollapsed(!collapsed)}
      >
        {collapsed ? <ChevronRight size={22} /> : <ChevronLeft size={22} />}
      </button>

      <div className={styles.studentMark}>
        <GraduationCap size={collapsed ? 23 : 18} />
        {!collapsed && <span>Parcours apprenant</span>}
      </div>

      <nav className={styles.nav}>
        {menu.map((item) => {
          const Icon = item.icon;

          return (
            <NavLink
              key={item.path}
              to={item.path}
              title={collapsed ? item.label : ""}
              className={({ isActive }) =>
                isActive ? `${styles.link} ${styles.active}` : styles.link
              }
            >
              <Icon size={22} />
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          );
        })}
      </nav>

      <button
        type="button"
        className={styles.profile}
        onClick={() => navigate("/student/profile")}
      >
        <User size={22} />
        {!collapsed && <span>Mon profil</span>}
      </button>

      <button
        type="button"
        className={styles.logout}
        onClick={() => setShowLogoutConfirm(true)}
      >
        <LogOut size={22} />
        {!collapsed && <span>Deconnexion</span>}
      </button>

      <LogoutConfirmDialog
        open={showLogoutConfirm}
        onCancel={() => setShowLogoutConfirm(false)}
        onConfirm={logout}
      />
    </aside>
  );
}
