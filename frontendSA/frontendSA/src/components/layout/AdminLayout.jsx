import { useState } from "react";
import { Outlet } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import styles from "./AdminLayout.module.css";

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className={styles.layout}>
      <AdminSidebar collapsed={collapsed} setCollapsed={setCollapsed} />
      <main className={`${styles.main} ${collapsed ? styles.mainCollapsed : ""}`}>
        <Outlet />
      </main>
    </div>
  );
}
