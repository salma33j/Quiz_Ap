import { Clock } from "lucide-react";
import styles from "./Timer.module.css";

const formatTime = (seconds) => {
  const safeSeconds = Math.max(0, Number(seconds || 0));
  const minutes = Math.floor(safeSeconds / 60);
  const remaining = safeSeconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(remaining).padStart(2, "0")}`;
};

export default function Timer({ seconds }) {
  const value = Number(seconds || 0);
  const danger = value > 0 && value <= 60;

  return (
    <div className={`${styles.timer} ${danger ? styles.danger : ""}`}>
      <Clock size={18} />
      <span>{seconds === null || seconds === undefined ? "--:--" : formatTime(value)}</span>
    </div>
  );
}
