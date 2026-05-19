import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Mail, Lock, Eye, EyeOff, GraduationCap } from "lucide-react";
import useAuth from "../../hooks/useAuth";
import styles from "./Login.module.css";

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const redirectByRole = (data) => {
    if (data.mustChangePassword) {
      navigate("/change-password");
      return;
    }

    if (data.role === "ADMIN") {
      navigate("/admin/dashboard");
    } else if (data.role === "ENSEIGNANT") {
      navigate("/teacher/dashboard");
    } else if (data.role === "ETUDIANT") {
      navigate("/student/dashboard");
    } else {
      navigate("/login");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await login(form.email, form.password);
      redirectByRole(data);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          "Email ou mot de passe incorrect"
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <section className={styles.leftPanel}>
          <div className={styles.formBox}>
            <div className={styles.logo}>
              <GraduationCap size={34} />
            </div>

            <h1>Bienvenue</h1>

            <p className={styles.subtitle}>
              Connectez-vous à votre plateforme Quiz FSB
            </p>

            {error && <div className={styles.error}>{error}</div>}

            <form className={styles.form} onSubmit={handleSubmit}>
              <div className={styles.inputGroup}>
                <Mail size={18} />
                <input
                  type="email"
                  name="email"
                  placeholder="Entrez votre email"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className={styles.inputGroup}>
                <Lock size={18} />
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  placeholder="Entrez votre mot de passe"
                  value={form.password}
                  onChange={handleChange}
                  required
                  minLength={8}
                />

                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>

              <button className={styles.loginButton} disabled={loading}>
                {loading ? "Connexion..." : "Se connecter"}
              </button>
            </form>

            <p className={styles.note}>
              Les comptes sont créés par l’administrateur.
            </p>
          </div>
        </section>

        <section className={styles.rightPanel}>
          <div className={styles.textBox}>
          <h2>
                Boostez vos
                 
                    connaissances
                   </h2>

                  <p>
                    Testez vos compétences,
                    suivez votre progression
                   et améliorez votre niveau.
                     </p>
          </div>

          <div className={styles.illustration}>
            <img
              src="https://img.freepik.com/free-vector/online-learning-concept-illustration_114360-4767.jpg"
              alt="Quiz"
            />
          </div>

          <div className={styles.quote}>
            Learn smarter. Challenge yourself. Achieve excellence.
          </div>
        </section>
      </div>
    </div>
  );
}