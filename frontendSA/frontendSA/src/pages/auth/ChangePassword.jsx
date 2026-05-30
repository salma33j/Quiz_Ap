import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Lock, ShieldCheck } from "lucide-react";
import { changePasswordApi } from "../../api/authApi";
import LogoutConfirmDialog from "../../components/common/LogoutConfirmDialog";
import useAuth from "../../hooks/useAuth";
import styles from "./ChangePassword.module.css";

export default function ChangePassword() {
  const navigate = useNavigate();
  const { user, refreshUser, logout } = useAuth();

  const [form, setForm] = useState({
    oldPassword: "",
    newPassword: "",
  });

  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const redirectByRole = () => {
    if (user.role === "ADMIN") {
      navigate("/admin/dashboard");
    } else if (user.role === "ENSEIGNANT") {
      navigate("/teacher/dashboard");
    } else {
      navigate("/student/dashboard");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (form.newPassword.length < 8) {
      setError("Le nouveau mot de passe doit contenir au moins 8 caractères.");
      return;
    }

    if (form.newPassword !== confirmPassword) {
      setError("Les deux mots de passe ne correspondent pas.");
      return;
    }

    setLoading(true);

    try {
      await changePasswordApi(user.userId, form);
      setSuccess("Mot de passe modifié avec succès.");

      await refreshUser();

      setTimeout(() => {
        redirectByRole();
      }, 800);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Erreur lors du changement du mot de passe."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.icon}>
          <ShieldCheck size={38} />
        </div>

        <h1>Changer le mot de passe</h1>

        <p>
          Pour sécuriser votre compte, veuillez modifier votre mot de passe
          provisoire.
        </p>

        {error && <div className={styles.error}>{error}</div>}
        {success && <div className={styles.success}>{success}</div>}

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.inputGroup}>
            <Lock size={18} />
            <input
              type="password"
              name="oldPassword"
              placeholder="Ancien mot de passe"
              value={form.oldPassword}
              onChange={handleChange}
              required
            />
          </div>

          <div className={styles.inputGroup}>
            <Lock size={18} />
            <input
              type="password"
              name="newPassword"
              placeholder="Nouveau mot de passe"
              value={form.newPassword}
              onChange={handleChange}
              required
              minLength={8}
            />
          </div>

          <div className={styles.inputGroup}>
            <Lock size={18} />
            <input
              type="password"
              placeholder="Confirmer le nouveau mot de passe"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={8}
            />
          </div>

          <button disabled={loading}>
            {loading ? "Modification..." : "Modifier le mot de passe"}
          </button>
        </form>

        <button
          type="button"
          className={styles.logoutBtn}
          onClick={() => setShowLogoutConfirm(true)}
        >
          Se déconnecter
        </button>
      </div>

      <LogoutConfirmDialog
        open={showLogoutConfirm}
        onCancel={() => setShowLogoutConfirm(false)}
        onConfirm={logout}
      />
    </div>
  );
}
