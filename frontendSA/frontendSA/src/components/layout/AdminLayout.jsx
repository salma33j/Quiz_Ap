import { useEffect, useRef, useState } from "react";
import { Outlet, useLocation } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import styles from "./AdminLayout.module.css";

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const mainRef = useRef(null);

  useEffect(() => {
    mainRef.current?.scrollTo({ top: 0, left: 0, behavior: "smooth" });
  }, [location.pathname, location.search]);

  return (
    <div className={styles.layout}>
      <AdminSidebar collapsed={collapsed} setCollapsed={setCollapsed} />
      <main ref={mainRef} className={`${styles.main} ${collapsed ? styles.mainCollapsed : ""}`}>
        <Outlet />
      </main>
    </div>
  );
}
