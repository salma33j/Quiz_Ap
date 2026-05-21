import { useState } from "react";

import { Outlet } from "react-router-dom";

import StudentSidebar from "./StudentSidebar";

import styles from "./DashboardLayout.module.css";

export default function StudentLayout() {
  const [collapsed, setCollapsed] =
    useState(false);

  return (
    <div className={styles.layout}>
      <StudentSidebar
        collapsed={collapsed}
        setCollapsed={setCollapsed}
      />

      <main
        className={`${styles.main} ${
          collapsed
            ? styles.mainCollapsed
            : ""
        }`}
      >
        <Outlet />
      </main>
    </div>
  );
}