import { NavLink, useNavigate } from "react-router-dom";

import {
  LayoutDashboard,
  ClipboardList,
  History,
  BarChart3,
  FileCheck,
  User,
  LogOut,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

import styles from "./Sidebar.module.css";

export default function StudentSidebar({
  collapsed,
  setCollapsed,
}) {
  const navigate = useNavigate();

  const menu = [
    {
      label: "Dashboard",
      path: "/student/dashboard",
      icon: LayoutDashboard,
    },

    {
      label: "Quiz disponibles",
      path: "/student/quizzes",
      icon: ClipboardList,
    },

    {
      label: "Historique",
      path: "/student/history",
      icon: History,
    },

    {
      label: "Performance",
      path: "/student/performance",
      icon: BarChart3,
    },

    {
      label: "Corrections",
      path: "/student/corrections",
      icon: FileCheck,
    },

    {
      label: "Profil",
      path: "/student/profile",
      icon: User,
    },
  ];

  const logout = () => {
    localStorage.clear();
    navigate("/login");
  };

  return (
    <aside
      className={`${styles.sidebar} ${
        collapsed ? styles.collapsed : ""
      }`}
    >
      <div>
        <button
          type="button"
          className={styles.logoBox}
          onClick={() =>
            navigate("/student/dashboard")
          }
        >
          <img src="/logo.png" alt="QuizApp" />

          {!collapsed && (
            <div>
              <h2>QuizApp</h2>
              <p>Espace étudiant</p>
            </div>
          )}
        </button>

        <button
          className={styles.toggleBtn}
          onClick={() =>
            setCollapsed(!collapsed)
          }
          type="button"
        >
          {collapsed ? (
            <ChevronRight size={20} />
          ) : (
            <ChevronLeft size={20} />
          )}
        </button>

        <nav className={styles.nav}>
          {menu.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                title={
                  collapsed ? item.label : ""
                }
                className={({ isActive }) =>
                  isActive
                    ? `${styles.link} ${styles.active}`
                    : styles.link
                }
              >
                <Icon size={20} />

                {!collapsed && (
                  <span>{item.label}</span>
                )}
              </NavLink>
            );
          })}
        </nav>
      </div>

      <button
        className={styles.logout}
        onClick={logout}
      >
        <LogOut size={20} />

        {!collapsed && (
          <span>Déconnexion</span>
        )}
      </button>
    </aside>
  );
}