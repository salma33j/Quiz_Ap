import { createContext, useEffect, useState } from "react";
import { loginApi, getCurrentUserApi } from "../api/authApi";

export const AuthContext = createContext(null);

function normalizeAuth(payload) {
  const root = payload?.data ?? payload ?? {};
  const user = root?.user || root?.customer || root?.enseignant || root?.student || root;
  const nameFromParts = [user?.firstName, user?.lastName].filter(Boolean).join(" ");
  const fullName =
    user?.fullName ||
    nameFromParts ||
    user?.username ||
    "";
  const [derivedFirstName, ...derivedLastNameParts] = `${fullName || ""}`
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  return {
    ...user,
    id: user?.id ?? user?.userId ?? root?.id ?? root?.userId,
    userId: user?.userId ?? user?.id ?? root?.userId ?? root?.id,
    firstName: user?.firstName || derivedFirstName || "",
    lastName: user?.lastName || derivedLastNameParts.join(" "),
    fullName,
    username: user?.username || fullName,
    role: user?.role || root?.role,
    token: user?.token || root?.token || localStorage.getItem("token"),
    mustChangePassword: user?.mustChangePassword ?? root?.mustChangePassword,
  };
}

function getSavedUser() {
  try {
    const savedUser = localStorage.getItem("user");
    return savedUser ? normalizeAuth(JSON.parse(savedUser)) : null;
  } catch {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getSavedUser);
  const [loading, setLoading] = useState(false);

  const login = async (email, password) => {
    const response = await loginApi({ email, password });
    const data = response.data;

    if (!data.success) {
      throw new Error(data.message || "Erreur de connexion");
    }

    const auth = normalizeAuth(data);
    localStorage.setItem("token", auth.token);
    localStorage.setItem("user", JSON.stringify(auth));
    setUser(auth);

    return auth;
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
    window.location.href = "/login";
  };

  const refreshUser = async () => {
    try {
      const response = await getCurrentUserApi();
      const data = response.data;

      const auth = normalizeAuth(data);
      localStorage.setItem("user", JSON.stringify(auth));
      setUser(auth);
      return auth;
    } catch {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      setUser(null);
      return null;
    }
  };

  useEffect(() => {
    const token = localStorage.getItem("token");

    if (token && !user) {
      refreshUser();
    }
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        setLoading,
        login,
        logout,
        refreshUser,
        isAuthenticated: Boolean(user),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
