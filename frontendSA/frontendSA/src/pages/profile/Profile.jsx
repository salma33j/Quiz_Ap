import { useRef, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import styles from './Profile.module.css';

export default function Profile() {
  const { user } = useAuth();
  const formRef = useRef(null);
  const fileInputRef = useRef(null);

  const [formData, setFormData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
    phone: '',
    filiere: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [successMsg, setSuccessMsg] = useState(false);
  const [errors, setErrors] = useState({});
  const [photoPreview, setPhotoPreview] = useState(null);

  const scrollToForm = () => {
    formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setTimeout(() => {
      formRef.current?.querySelector('input')?.focus();
    }, 500);
  };

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: false }));
  };

  const handlePhotoChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.onload = (ev) => setPhotoPreview(ev.target.result);
    reader.readAsDataURL(file);
  };

  const handleSave = (e) => {
    e.preventDefault();
    const newErrors = {};
    if (!formData.firstName.trim()) newErrors.firstName = true;
    if (!formData.lastName.trim()) newErrors.lastName = true;
    if (!formData.email.trim()) newErrors.email = true;
    if (formData.newPassword && formData.newPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = true;
    }
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    setSuccessMsg(true);
    setTimeout(() => {
      setSuccessMsg(false);
      scrollToTop();
    }, 2000);
  };

  const avatarLetter = (user?.firstName?.[0] || user?.email?.[0] || 'E').toUpperCase();
  const fullName = `${formData.firstName} ${formData.lastName}`.trim() || '-';

  return (
    <div className={styles.profilePage}>

      {/* ── Fiche profil ── */}
      <div className={styles.card}>
        <div className={styles.header}>
          <h2>Mon profil</h2>
        </div>

        <div className={styles.avatarWrapper}>
          {photoPreview ? (
            <img src={photoPreview} alt="Photo de profil" className={styles.avatarImg} />
          ) : (
            <div className={styles.avatar}>{avatarLetter}</div>
          )}
        </div>

        <div className={styles.infoGroup}>
          <div className={styles.infoRow}>
            <span className={styles.label}>Nom complet</span>
            <span className={styles.value}>{fullName}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>Email</span>
            <span className={styles.value}>{formData.email || '-'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>Rôle</span>
            <span className={styles.roleBadge}>{user?.role || 'Étudiant'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.label}>Membre depuis</span>
            <span className={styles.value}>{user?.memberSince || 'Septembre 2024'}</span>
          </div>
        </div>

        <button className={styles.editButton} onClick={scrollToForm}>
          Modifier mon profil
        </button>
      </div>

      {/* ── Formulaire de modification ── */}
      <div className={styles.formCard} ref={formRef}>

        {successMsg && (
          <div className={styles.successBanner}>
            ✓ Profil mis à jour avec succès !
          </div>
        )}

        <div className={styles.formHeader}>
          <div className={styles.formIcon}>✎</div>
          <div>
            <h3 className={styles.formTitle}>Modifier mon profil</h3>
            <p className={styles.formSub}>Les champs marqués <span className={styles.required}>*</span> sont obligatoires</p>
          </div>
        </div>

        <form onSubmit={handleSave} noValidate>

          {/* Avatar avec upload */}
          <div className={styles.avatarRow}>
            <div className={styles.avatarPreviewWrapper}>
              {photoPreview ? (
                <img src={photoPreview} alt="Aperçu" className={styles.avatarPreviewImg} />
              ) : (
                <div className={styles.avatarPreview}>{avatarLetter}</div>
              )}
            </div>
            <div className={styles.avatarInfo}>
              <p>{fullName}</p>
              <span>
                {photoPreview ? 'Nouvelle photo sélectionnée' : 'Photo de profil actuelle'}
              </span>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handlePhotoChange}
                className={styles.fileInput}
                aria-label="Choisir une photo de profil"
              />
              <button
                type="button"
                className={styles.changePhoto}
                onClick={() => fileInputRef.current?.click()}
              >
                📷 Changer la photo
              </button>
              {photoPreview && (
                <button
                  type="button"
                  className={styles.removePhoto}
                  onClick={() => {
                    setPhotoPreview(null);
                    if (fileInputRef.current) fileInputRef.current.value = '';
                  }}
                >
                  Supprimer
                </button>
              )}
            </div>
          </div>

          {/* Prénom / Nom */}
          <div className={styles.fieldGrid}>
            <div className={styles.fieldGroup}>
              <label htmlFor="firstName">
                Prénom <span className={styles.required}>*</span>
              </label>
              <input
                id="firstName"
                name="firstName"
                type="text"
                value={formData.firstName}
                onChange={handleChange}
                placeholder="Prénom"
                className={errors.firstName ? styles.inputError : ''}
              />
              {errors.firstName && <span className={styles.errorMsg}>Champ requis</span>}
            </div>
            <div className={styles.fieldGroup}>
              <label htmlFor="lastName">
                Nom <span className={styles.required}>*</span>
              </label>
              <input
                id="lastName"
                name="lastName"
                type="text"
                value={formData.lastName}
                onChange={handleChange}
                placeholder="Nom"
                className={errors.lastName ? styles.inputError : ''}
              />
              {errors.lastName && <span className={styles.errorMsg}>Champ requis</span>}
            </div>
          </div>

          {/* Email */}
          <div className={styles.fieldGroup}>
            <label htmlFor="email">
              Adresse email <span className={styles.required}>*</span>
            </label>
            <input
              id="email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="exemple@etu.quizapp.com"
              className={errors.email ? styles.inputError : ''}
            />
            {errors.email && <span className={styles.errorMsg}>Champ requis</span>}
          </div>

          {/* Téléphone / Filière */}
          <div className={styles.fieldGrid}>
            <div className={styles.fieldGroup}>
              <label htmlFor="phone">Téléphone</label>
              <input
                id="phone"
                name="phone"
                type="tel"
                value={formData.phone}
                onChange={handleChange}
                placeholder="+212 6XX XXX XXX"
              />
            </div>
            <div className={styles.fieldGroup}>
              <label htmlFor="filiere">Filière</label>
              <select
                id="filiere"
                name="filiere"
                value={formData.filiere}
                onChange={handleChange}
              >
                <option value="">Sélectionner</option>
                <option value="informatique">Informatique</option>
                <option value="maths">Mathématiques</option>
                <option value="physique">Physique</option>
                <option value="autre">Autre</option>
              </select>
            </div>
          </div>

          {/* Séparateur sécurité */}
          <div className={styles.divider}>
            <div className={styles.dividerLine} />
            <span>🔒 Sécurité</span>
            <div className={styles.dividerLine} />
          </div>

          {/* Mot de passe */}
          <div className={styles.fieldGrid}>
            <div className={styles.fieldGroup}>
              <label htmlFor="newPassword">Nouveau mot de passe</label>
              <input
                id="newPassword"
                name="newPassword"
                type="password"
                value={formData.newPassword}
                onChange={handleChange}
                placeholder="••••••••"
              />
            </div>
            <div className={styles.fieldGroup}>
              <label htmlFor="confirmPassword">Confirmer</label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="••••••••"
                className={errors.confirmPassword ? styles.inputError : ''}
              />
              {errors.confirmPassword && (
                <span className={styles.errorMsg}>Les mots de passe ne correspondent pas</span>
              )}
            </div>
          </div>

          {/* Boutons */}
          <div className={styles.btnRow}>
            <button type="button" className={styles.btnCancel} onClick={scrollToTop}>
              Annuler
            </button>
            <button type="submit" className={styles.btnSave}>
              ✓ Enregistrer les modifications
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}