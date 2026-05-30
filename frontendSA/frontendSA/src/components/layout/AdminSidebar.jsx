import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import {
  BookMarked,
  BookOpen,
  BarChart3,
  ChevronLeft,
  ChevronRight,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Mail,
  ShieldCheck,
  User,
  Users,
} from "lucide-react";
import LogoutConfirmDialog from "../common/LogoutConfirmDialog";
import styles from "./AdminSidebar.module.css";

const menu = [
  { label: "Vue globale", path: "/admin/dashboard", icon: LayoutDashboard },
  { label: "Classes", path: "/admin/classes", icon: GraduationCap },
  { label: "Utilisateurs", path: "/admin/users", icon: Users },
  { label: "Matières", path: "/admin/subjects", icon: BookMarked },
  { label: "Quiz", path: "/admin/quizzes", icon: BookOpen },
  { label: "Résultats", path: "/admin/results", icon: BarChart3 },
  { label: "Emails", path: "/admin/emails", icon: Mail },
  { label: "Profil", path: "/admin/profile", icon: User },
];

export default function AdminSidebar({ collapsed, setCollapsed }) {
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
        onClick={() => navigate("/admin/dashboard")}
      >
        <img src="/logo.png" alt="QuizApp" />
        {!collapsed && (
          <div>
            <h2>QuizApp</h2>
            <p>Console admin</p>
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

      <div className={styles.adminMark}>
        <ShieldCheck size={collapsed ? 23 : 18} />
        {!collapsed && <span>Contrôle plateforme</span>}
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
        className={styles.logout}
        onClick={() => setShowLogoutConfirm(true)}
      >
        <LogOut size={22} />
        {!collapsed && <span>Déconnexion</span>}
      </button>

      <LogoutConfirmDialog
        open={showLogoutConfirm}
        onCancel={() => setShowLogoutConfirm(false)}
        onConfirm={logout}
      />
    </aside>
  );
}
