import { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import styles from "./DashboardLayout.module.css";

export default function DashboardLayout() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className={styles.layout}>
      <Sidebar collapsed={collapsed} setCollapsed={setCollapsed} />

      <main
        className={`${styles.main} ${
          collapsed ? styles.mainCollapsed : ""
        }`}
      >
        <Outlet />
      </main>
    </div>
  );
}