package com.exemple.quiz_app.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // URL de votre application frontend (à configurer dans application.properties)
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * ✅ Envoie les identifiants à un ÉTUDIANT nouvellement créé
     */
    public void sendEtudiantCredentials(String toEmail, String firstName, String lastName,
                                        String password) {
        String subject = "🎓 Vos identifiants - Plateforme Quiz FSB";

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;">
                
                    <!-- HEADER -->
                    <div style="background-color: #1a73e8; padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">🎓 Plateforme Quiz FSB</h1>
                        <p style="color: #d0e8ff; margin: 5px 0 0;">Faculté des Sciences Ben M'Sick</p>
                    </div>
                
                    <!-- BODY -->
                    <div style="padding: 30px; background-color: #ffffff;">
                        <p style="font-size: 16px;">Bonjour <strong>%s %s</strong>,</p>
                        <p>Votre compte étudiant a été créé sur la plateforme de quiz en ligne.</p>
                        <p>Voici vos identifiants de connexion :</p>
                
                        <!-- CREDENTIALS BOX -->
                        <div style="background-color: #f0f7ff; border-left: 4px solid #1a73e8;
                                    padding: 20px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 5px 0;"><strong>📧 Email :</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>🔑 Mot de passe provisoire :</strong>
                                <span style="font-family: monospace; font-size: 16px;
                                             background: #fff; padding: 2px 8px;
                                             border-radius: 4px; border: 1px solid #ccc;">%s</span>
                            </p>
                            <p style="margin: 5px 0;"><strong>👤 Rôle :</strong> Étudiant</p>
                        </div>
                
                        <!-- WARNING -->
                        <div style="background-color: #fff8e1; border-left: 4px solid #f9a825;
                                    padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 0; color: #e65100;">
                                ⚠️ <strong>Important :</strong> Veuillez changer votre mot de passe
                                dès votre première connexion.
                            </p>
                        </div>
                
                        <!-- BUTTON -->
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/login"
                               style="background-color: #1a73e8; color: white; padding: 14px 30px;
                                      text-decoration: none; border-radius: 6px; font-size: 16px;
                                      display: inline-block;">
                                🚀 Se connecter maintenant
                            </a>
                        </div>
                
                        <p style="color: #666; font-size: 13px;">
                            Si vous n'êtes pas à l'origine de cette inscription, veuillez contacter
                            l'administration immédiatement.
                        </p>
                    </div>
                
                    <!-- FOOTER -->
                    <div style="background-color: #f5f5f5; padding: 15px; text-align: center;
                                color: #999; font-size: 12px;">
                        <p style="margin: 0;">Université Hassan II de Casablanca — Faculté des Sciences Ben M'Sick</p>
                        <p style="margin: 5px 0 0;">© 2025-2026 Plateforme Quiz FSB</p>
                    </div>
                </div>
                """.formatted(firstName, lastName, toEmail, password, frontendUrl);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * ✅ Envoie les identifiants à un ENSEIGNANT nouvellement créé
     */
    public void sendEnseignantCredentials(String toEmail, String firstName, String lastName,
                                          String password) {
        String subject = "👨‍🏫 Vos identifiants Enseignant - Plateforme Quiz FSB";

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;">
                
                    <!-- HEADER -->
                    <div style="background-color: #2e7d32; padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">👨‍🏫 Plateforme Quiz FSB</h1>
                        <p style="color: #c8e6c9; margin: 5px 0 0;">Espace Enseignant</p>
                    </div>
                
                    <!-- BODY -->
                    <div style="padding: 30px; background-color: #ffffff;">
                        <p style="font-size: 16px;">Bonjour <strong>%s %s</strong>,</p>
                        <p>Votre compte enseignant a été créé sur la plateforme de quiz en ligne.</p>
                        <p>Avec votre compte, vous pouvez :</p>
                        <ul style="color: #444;">
                            <li>✅ Créer et gérer des quiz</li>
                            <li>✅ Générer des quiz automatiquement via l'IA (Gemini)</li>
                            <li>✅ Consulter les résultats et statistiques de vos étudiants</li>
                        </ul>
                
                        <!-- CREDENTIALS BOX -->
                        <div style="background-color: #f1f8e9; border-left: 4px solid #2e7d32;
                                    padding: 20px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 5px 0;"><strong>📧 Email :</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>🔑 Mot de passe provisoire :</strong>
                                <span style="font-family: monospace; font-size: 16px;
                                             background: #fff; padding: 2px 8px;
                                             border-radius: 4px; border: 1px solid #ccc;">%s</span>
                            </p>
                            <p style="margin: 5px 0;"><strong>👤 Rôle :</strong> Enseignant</p>
                        </div>
                
                        <!-- WARNING -->
                        <div style="background-color: #fff8e1; border-left: 4px solid #f9a825;
                                    padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 0; color: #e65100;">
                                ⚠️ <strong>Important :</strong> Veuillez changer votre mot de passe
                                dès votre première connexion.
                            </p>
                        </div>
                
                        <!-- BUTTON -->
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/login"
                               style="background-color: #2e7d32; color: white; padding: 14px 30px;
                                      text-decoration: none; border-radius: 6px; font-size: 16px;
                                      display: inline-block;">
                                🚀 Se connecter maintenant
                            </a>
                        </div>
                    </div>
                
                    <!-- FOOTER -->
                    <div style="background-color: #f5f5f5; padding: 15px; text-align: center;
                                color: #999; font-size: 12px;">
                        <p style="margin: 0;">Université Hassan II de Casablanca — Faculté des Sciences Ben M'Sick</p>
                        <p style="margin: 5px 0 0;">© 2025-2026 Plateforme Quiz FSB</p>
                    </div>
                </div>
                """.formatted(firstName, lastName, toEmail, password, frontendUrl);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * ✅ Méthode générique pour envoyer un email HTML
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);
            System.out.println("✅ [EmailService] Email envoyé à : " + toEmail);
        } catch (Exception e) {
            // On log l'erreur mais on ne bloque pas la création du compte
            System.err.println("❌ [EmailService] Erreur envoi email à " + toEmail + " : " + e.getMessage());
        }
    }
}