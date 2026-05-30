import { createContext, useEffect, useState } from "react";
import { loginApi, getCurrentUserApi } from "../api/authApi";

export const AuthContext = createContext(null);

function getSavedUser() {
  try {
    const savedUser = localStorage.getItem("user");
    return savedUser ? JSON.parse(savedUser) : null;
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

    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify(data));
    setUser(data);

    return data;
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

      localStorage.setItem("user", JSON.stringify(data));
      setUser(data);
    } catch {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      setUser(null);
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