import styles from "./StudentDashboard.module.css";

export default function StudentDashboard() {
  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1>Bienvenue sur QuizApp</h1>
        <p>
          Vous pouvez consulter les quiz publiés et démarrer une session lorsque
          vous êtes prêt. Utilisez le lien de votre quiz pour accéder à la page
          de détails et cliquer sur "Commencer le quiz".
        </p>
      </div>
    </div>
  );
}
