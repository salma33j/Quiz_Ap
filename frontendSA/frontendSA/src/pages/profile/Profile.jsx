import useAuth from '../../hooks/useAuth';
import styles from './Profile.module.css';

export default function Profile() {
  const { user } = useAuth();

  return (
    <div className={styles.card}>
      <h2>Mon profil</h2>
      <div className={styles.avatar}>{(user?.firstName?.[0] || user?.email?.[0] || 'E').toUpperCase()}</div>
      <p><b>Nom :</b> {user?.firstName || ''} {user?.lastName || ''}</p>
      <p><b>Email :</b> {user?.email || '-'}</p>
      <p><b>Rôle :</b> {user?.role || 'ENSEIGNANT'}</p>
    </div>
  );
}
