import { AlertTriangle, X } from "lucide-react";
import styles from "./ConfirmDialog.module.css";

export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Confirmer",
  cancelLabel = "Annuler",
  tone = "danger",
  onCancel,
  onConfirm,
}) {
  if (!open) return null;

  const isDanger = tone === "danger";

  return (
    <div
      className={styles.overlay}
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onCancel?.();
      }}
    >
      <section
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
      >
        <button
          type="button"
          className={styles.closeBtn}
          onClick={onCancel}
          aria-label="Fermer"
        >
          <X size={18} />
        </button>

        <div className={isDanger ? styles.dangerIcon : styles.icon}>
          <AlertTriangle size={28} />
        </div>

        <h2 id="confirm-dialog-title">{title}</h2>
        <p>{message}</p>

        <div className={styles.actions}>
          <button type="button" className={styles.cancelBtn} onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={isDanger ? styles.dangerBtn : styles.confirmBtn}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
