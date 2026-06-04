import { Navigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";

export default function HomeRedirect() {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.mustChangePassword) {
    return <Navigate to="/change-password" replace />;
  }

  if (user.role === "ADMIN") {
    return <Navigate to="/admin/dashboard" replace />;
  }

  if (user.role === "ENSEIGNANT") {
    return <Navigate to="/teacher/dashboard" replace />;
  }

  if (user.role === "ETUDIANT") {
    return <Navigate to="/student/dashboard" replace />;
  }

  return <Navigate to="/login" replace />;
}