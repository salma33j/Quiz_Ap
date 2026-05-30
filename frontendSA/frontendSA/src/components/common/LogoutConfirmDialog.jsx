import { LogOut, X } from "lucide-react";
import styles from "./LogoutConfirmDialog.module.css";

export default function LogoutConfirmDialog({ open, onCancel, onConfirm }) {
  if (!open) return null;

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.dialog}>
        <button
          type="button"
          className={styles.closeBtn}
          onClick={onCancel}
          aria-label="Fermer"
        >
          <X size={18} />
        </button>

        <div className={styles.icon}>
          <LogOut size={28} />
        </div>

        <h2>Confirmer la déconnexion</h2>
        <p>Voulez-vous vraiment quitter votre session maintenant ?</p>

        <div className={styles.actions}>
          <button type="button" className={styles.cancelBtn} onClick={onCancel}>
            Annuler
          </button>
          <button type="button" className={styles.confirmBtn} onClick={onConfirm}>
            Se déconnecter
          </button>
        </div>
      </div>
    </div>
  );
}
