import { useEffect, useState } from "react";
import {
  CalendarDays,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Save,
  ShieldCheck,
  X,
} from "lucide-react";
import axiosInstance from "../../api/axiosInstance";
import styles from "./Profile.module.css";

const splitName = (value = "") => {
  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  return {
    firstName: parts[0] || "",
    lastName: parts.slice(1).join(" "),
  };
};

const normalizeProfile = (data = {}) => {
  const fallbackName = splitName(data.fullName || data.username || "");

  return {
    ...data,
    id: data.id ?? data.userId,
    firstName: data.firstName || fallbackName.firstName,
    lastName: data.lastName || fallbackName.lastName,
    email: data.email || "",
    role: data.role || "",
  };
};

const roleLabel = (role) => {
  if (role === "ADMIN") return "administrateur";
  if (role === "ETUDIANT" || role === "STUDENT") return "étudiant";
  return "enseignant";
};

export default function Profile() {
  const [user, setUser] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [passwordMode, setPasswordMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [passwordMessage, setPasswordMessage] = useState("");
  const [passwordMessageType, setPasswordMessageType] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
  });

  const [passwordForm, setPasswordForm] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const showMessage = (text, type = "error") => {
    setMessage(text);
    setMessageType(type);
  };

  const showPasswordMessage = (text, type = "error") => {
    setPasswordMessage(text);
    setPasswordMessageType(type);
  };

  const getErrorMessage = (error, defaultMessage) => {
    if (error.response) {
      return (
        error.response.data?.message ||
        error.response.data?.error ||
        defaultMessage
      );
    }

    if (error.request) {
      return "Serveur inaccessible. Vérifiez que Spring Boot est démarré.";
    }

    return defaultMessage;
  };

  const syncForm = (profile) => {
    setForm({
      firstName: profile.firstName || "",
      lastName: profile.lastName || "",
      email: profile.email || "",
    });
  };

  const loadProfile = async () => {
    try {
      setLoading(true);
      setMessage("");

      const res = await axiosInstance.get("/auth/me");
      const profile = normalizeProfile(res.data);

      setUser(profile);
      syncForm(profile);
    } catch (error) {
      console.error("Erreur profil :", error);
      showMessage(getErrorMessage(error, "Impossible de charger le profil."));
    } finally {
      setLoading(false);
    }
  };

  const updateProfile = async () => {
    try {
      setSaving(true);
      setMessage("");

      if (!user?.id) {
        showMessage("Utilisateur introuvable. Rechargez la page.");
        return;
      }

      const payload = {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
      };

      if (!payload.firstName || !payload.lastName || !payload.email) {
        showMessage("Veuillez remplir le prénom, le nom et l'e-mail.");
        return;
      }

      const res = await axiosInstance.put(`/auth/profile/${user.id}`, payload);
      const data = res.data || {};

      if (data.success === false) {
        showMessage(data.message || "Erreur lors de l'enregistrement du profil.");
        return;
      }

      const nextUser = normalizeProfile({
        ...user,
        ...data,
        ...payload,
      });

      if (data.token) {
        localStorage.setItem("token", data.token);
      }

      const storedUser = JSON.parse(localStorage.getItem("user") || "null");
      if (storedUser) {
        localStorage.setItem("user", JSON.stringify({ ...storedUser, ...nextUser }));
      }

      setUser(nextUser);
      syncForm(nextUser);
      setEditMode(false);
      showMessage(data.message || "Profil enregistré avec succès.", "success");
    } catch (error) {
      console.error("Erreur modification profil :", error);
      showMessage(getErrorMessage(error, "Erreur lors de l'enregistrement du profil."));
    } finally {
      setSaving(false);
    }
  };

  const updatePassword = async () => {
    try {
      setSaving(true);
      setPasswordMessage("");

      if (!user?.id) {
        showPasswordMessage("Utilisateur introuvable. Rechargez la page.");
        return;
      }

      if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
        showPasswordMessage("Veuillez remplir tous les champs du mot de passe.");
        return;
      }

      if (passwordForm.newPassword.length < 8) {
        showPasswordMessage("Le nouveau mot de passe doit contenir au moins 8 caractères.");
        return;
      }

      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        showPasswordMessage("Les deux nouveaux mots de passe ne correspondent pas.");
        return;
      }

      const res = await axiosInstance.put(`/auth/user/${user.id}/password`, {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      });
      const data = res.data || {};

      if (data.success === false) {
        showPasswordMessage(data.message || "Erreur lors du changement du mot de passe.");
        return;
      }

      setPasswordForm({
        oldPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
      setPasswordMode(false);
      setPasswordMessage("");
      showMessage(data.message || "Mot de passe modifié avec succès.", "success");
    } catch (error) {
      console.error("Erreur mot de passe :", error);
      showPasswordMessage(getErrorMessage(error, "Erreur lors du changement du mot de passe."));
    } finally {
      setSaving(false);
    }
  };

  const cancelEdit = () => {
    setEditMode(false);
    setMessage("");
    if (user) syncForm(user);
  };

  const getCreatedDate = () => {
    if (!user?.createdAt) return "Non renseigné";
    return new Date(user.createdAt).toLocaleDateString("fr-FR");
  };

  const avatarLetters = `${user?.firstName?.[0] || ""}${user?.lastName?.[0] || ""}` || "US";

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.profileCard}>
          <div className={styles.loadingBox}>Chargement du profil...</div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.profileCard}>
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <div className={styles.avatar}>
              <span className={styles.avatarLetters}>{avatarLetters.toUpperCase()}</span>
            </div>

            <div>
              <span className={styles.profileBadge}>
                <ShieldCheck size={16} />
                Compte {roleLabel(user?.role)}
              </span>

              <h1>Mon profil</h1>
              <p>Gérez vos informations, votre email et votre mot de passe.</p>
            </div>
          </div>
        </div>

        {message && (
          <div
            className={`${styles.message} ${
              messageType === "success" ? styles.success : styles.error
            }`}
          >
            {message}
          </div>
        )}

        <section className={styles.section}>
          <div className={styles.sectionTitle}>
            <div>
              <h2>Informations du compte</h2>
              <p>Modifiez vos informations visibles dans la plateforme.</p>
            </div>

            {!editMode && (
              <button
                type="button"
                className={styles.editBtn}
                onClick={() => {
                  setEditMode(true);
                  setMessage("");
                }}
              >
                Modifier le profil
              </button>
            )}
          </div>

          <div className={styles.infoGrid}>
            <div className={styles.infoField}>
              <label>Prénom</label>
              {editMode ? (
                <input
                  value={form.firstName}
                  placeholder="Prénom"
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                />
              ) : (
                <p>{user?.firstName || "-"}</p>
              )}
            </div>

            <div className={styles.infoField}>
              <label>Nom</label>
              {editMode ? (
                <input
                  value={form.lastName}
                  placeholder="Nom"
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                />
              ) : (
                <p>{user?.lastName || "-"}</p>
              )}
            </div>

            <div className={`${styles.infoField} ${styles.fullWidth}`}>
              <label>E-mail</label>
              {editMode ? (
                <input
                  type="email"
                  value={form.email}
                  placeholder="Adresse e-mail"
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
              ) : (
                <p>
                  <Mail size={18} />
                  {user?.email || "-"}
                </p>
              )}
            </div>

            <div className={styles.infoField}>
              <label>Membre depuis</label>
              <p>
                <CalendarDays size={18} />
                {getCreatedDate()}
              </p>
            </div>

            <div className={styles.infoField}>
              <label>Rôle</label>
              <p>{user?.role || "-"}</p>
            </div>
          </div>

          <div className={styles.passwordRow}>
            <div>
              <h3>Mot de passe</h3>
              <div className={styles.passwordPreview}>
                <Lock size={20} />
                <span>••••••••</span>
                <div className={styles.security}>
                  <i />
                  <i />
                  <i className={styles.gray} />
                </div>
                <strong>Sécurité : Moyen</strong>
              </div>
            </div>

            <button
              type="button"
              className={styles.smallBtn}
              onClick={() => {
                setPasswordMode(true);
                setMessage("");
                setPasswordMessage("");
              }}
            >
              Modifier
            </button>
          </div>

          {editMode && (
            <div className={styles.actions}>
              <button
                type="button"
                className={styles.saveBtn}
                onClick={updateProfile}
                disabled={saving}
              >
                <Save size={18} />
                {saving ? "Enregistrement..." : "Enregistrer les modifications"}
              </button>

              <button
                type="button"
                className={styles.cancelBtn}
                onClick={cancelEdit}
                disabled={saving}
              >
                <X size={18} />
                Annuler
              </button>
            </div>
          )}
        </section>
      </div>

      {passwordMode && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <h2>Modifier le mot de passe</h2>

            {passwordMessage && (
              <div
                className={`${styles.message} ${styles.modalMessage} ${
                  passwordMessageType === "success" ? styles.success : styles.error
                }`}
              >
                {passwordMessage}
              </div>
            )}

            <label>Ancien mot de passe</label>
            <input
              type="password"
              value={passwordForm.oldPassword}
              onChange={(e) =>
                setPasswordForm({ ...passwordForm, oldPassword: e.target.value })
              }
            />

            <label>Nouveau mot de passe</label>
            <div className={styles.passwordInput}>
              <input
                type={showPassword ? "text" : "password"}
                value={passwordForm.newPassword}
                onChange={(e) =>
                  setPasswordForm({ ...passwordForm, newPassword: e.target.value })
                }
              />
              <button type="button" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            <label>Confirmer le nouveau mot de passe</label>
            <input
              type="password"
              value={passwordForm.confirmPassword}
              onChange={(e) =>
                setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })
              }
            />

            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.saveBtn}
                onClick={updatePassword}
                disabled={saving}
              >
                {saving ? "Enregistrement..." : "Enregistrer"}
              </button>

              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() => {
                  setPasswordMode(false);
                  setMessage("");
                  setPasswordMessage("");
                }}
                disabled={saving}
              >
                Annuler
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
