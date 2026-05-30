import styles from "./Timer.module.css";

const formatTime = (seconds) => {
  if (seconds == null) return "--:--";
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
};

export default function Timer({ seconds }) {
  return (
    <div className={styles.timerBox}>
      <span className={styles.label}>Temps restant</span>
      <strong className={styles.time}>{formatTime(seconds)}</strong>
    </div>
  );
}
