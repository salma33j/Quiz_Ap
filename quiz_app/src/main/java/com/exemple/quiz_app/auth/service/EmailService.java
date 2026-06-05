package com.exemple.quiz_app.auth.service;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.matiere.entity.Matiere;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // URL de votre application frontend (à configurer dans application.properties)
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${email.provider:smtp}")
    private String emailProvider;

    @Value("${email.from:}")
    private String configuredFromEmail;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.api.url:https://api.resend.com/emails}")
    private String resendApiUrl;

    @Value("${gmail.api.client-id:}")
    private String gmailClientId;

    @Value("${gmail.api.client-secret:}")
    private String gmailClientSecret;

    @Value("${gmail.api.refresh-token:}")
    private String gmailRefreshToken;

    @Value("${gmail.api.user-id:me}")
    private String gmailUserId;

    @Value("${gmail.api.token-url:https://oauth2.googleapis.com/token}")
    private String gmailTokenUrl;

    @Value("${gmail.api.send-url:https://gmail.googleapis.com/gmail/v1/users/%s/messages/send}")
    private String gmailSendUrl;

    @Value("${gmail.from:}")
    private String gmailFromEmail;

    @PostConstruct
    public void logEmailConfiguration() {
        System.out.println("[EmailService] Configuration email: provider=" + resolveProvider()
                + ", resendApiKeyConfigured=" + isConfigured(resendApiKey)
                + ", gmailClientConfigured=" + isConfigured(gmailClientId)
                + ", gmailRefreshTokenConfigured=" + isConfigured(gmailRefreshToken)
                + ", gmailFromConfigured=" + isConfigured(gmailFromEmail)
                + ", emailFromConfigured=" + isConfigured(resolveFromEmail())
                + ", smtpUsernameConfigured=" + isConfigured(fromEmail));
    }

    /**
     * ✅ Envoie les identifiants à un ÉTUDIANT nouvellement créé
     */
    public boolean sendEtudiantCredentials(String toEmail, String firstName, String lastName,
                                        String password) {
        String subject = "🎓 Vos identifiants - Plateforme Quiz FSB";

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;">
                
                    <!-- HEADER -->
                    <div style="background-color: #1a73e8; padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">🎓 Plateforme Quiz APP</h1>
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
 
                        <p style="margin: 5px 0 0;">© 2025-2026 Plateforme Quiz FSB</p>
                    </div>
                </div>
                """.formatted(firstName, lastName, toEmail, password, frontendUrl);

        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * ✅ Envoie les identifiants à un ENSEIGNANT nouvellement créé
     */
    public boolean sendEnseignantCredentials(String toEmail, String firstName, String lastName,
                                          String password) {
        String subject = "👨‍🏫 Vos identifiants Enseignant - Plateforme Quiz APP";

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
                        <p style="margin: 5px 0 0;">© 2025-2026 Plateforme Quiz FSB</p>
                    </div>
                </div>
                """.formatted(firstName, lastName, toEmail, password, frontendUrl);

        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    public boolean sendAdminCredentials(String toEmail, String firstName, String lastName, String password) {
        String subject = "Vos identifiants Admin - Plateforme Quiz APP";
        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;">
                    <div style="background-color: #070a35; padding: 30px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">Plateforme Quiz FSB</h1>
                        <p style="color: #dbeafe; margin: 5px 0 0;">Console administrateur</p>
                    </div>
                    <div style="padding: 30px; background-color: #ffffff;">
                        <p style="font-size: 16px;">Bonjour <strong>%s %s</strong>,</p>
                        <p>Votre compte administrateur a ete cree sur la plateforme.</p>
                        <div style="background-color: #f0f7ff; border-left: 4px solid #070a35;
                                    padding: 20px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 5px 0;"><strong>Email :</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>Mot de passe provisoire :</strong>
                                <span style="font-family: monospace; font-size: 16px;
                                             background: #fff; padding: 2px 8px;
                                             border-radius: 4px; border: 1px solid #ccc;">%s</span>
                            </p>
                            <p style="margin: 5px 0;"><strong>Role :</strong> Admin</p>
                        </div>
                        <p style="color: #e65100;"><strong>Important :</strong> Veuillez changer votre mot de passe
                        des votre premiere connexion.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/login"
                               style="background-color: #070a35; color: white; padding: 14px 30px;
                                      text-decoration: none; border-radius: 6px; font-size: 16px;
                                      display: inline-block;">
                                Se connecter maintenant
                            </a>
                        </div>
                    </div>
                </div>
                """.formatted(firstName, lastName, toEmail, password, frontendUrl);

        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    public boolean sendAnnouncement(String toEmail, String subject, String message) {
        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #dbeafe; border-radius: 12px; overflow: hidden;">
                    <div style="background-color: #070a35; padding: 24px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 22px;">Plateforme Quiz FSB</h1>
                        <p style="color: #dbeafe; margin: 6px 0 0;">Annonce administrateur</p>
                    </div>
                    <div style="padding: 28px; background-color: #ffffff;">
                        <p style="font-size: 16px; color: #111827; white-space: pre-line;">%s</p>
                    </div>
                </div>
                """.formatted(message);

        return sendHtmlEmail(toEmail, subject, htmlContent);
    }

    public boolean sendQuizPublishedEmail(User student, Quiz quiz) {
        String quizTitle = escapeHtml(orDefault(quiz.getTitre(), "Quiz"));
        String studentName = escapeHtml(orDefault(student.getFullName(), "Etudiant"));
        String matiereName = escapeHtml(resolveMatiereName(quiz));
        String classeName = escapeHtml(resolveClasseName(quiz));
        String deadline = quiz.getAvailableUntil() == null
                ? "Non definie"
                : quiz.getAvailableUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String timeLimit = quiz.getTimeLimit() == null ? "Non definie" : quiz.getTimeLimit() + " min";
        String questionCount = quiz.getQuestionCount() == null ? "0" : quiz.getQuestionCount().toString();
        String quizUrl = cleanFrontendUrl() + "/student/quizzes";

        String subject = "Nouveau quiz disponible - " + orDefault(quiz.getTitre(), "Quiz");
        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 620px; margin: auto;
                            border: 1px solid #f2d46b; border-radius: 18px; overflow: hidden;
                            background: #fffdf4;">
                    <div style="background: linear-gradient(135deg, #fffaf0, #ffe98f);
                                padding: 28px 32px; text-align: left;">
                        <p style="margin: 0 0 10px; color: #a16207; font-weight: 700;">
                            QuizApp
                        </p>
                        <h1 style="margin: 0; color: #111827; font-size: 24px;">
                            Nouveau quiz disponible
                        </h1>
                    </div>
                    <div style="padding: 30px 32px; background: #ffffff;">
                        <p style="font-size: 16px; color: #111827; margin-top: 0;">
                            Bonjour <strong>%s</strong>,
                        </p>
                        <p style="font-size: 15px; color: #4b5563; line-height: 1.6;">
                            Un nouveau quiz vient d'etre publie pour votre classe.
                        </p>

                        <div style="border: 1px solid #f2d46b; border-radius: 16px;
                                    padding: 20px; background: #fffbeb; margin: 22px 0;">
                            <h2 style="margin: 0 0 14px; color: #111827; font-size: 21px;">%s</h2>
                            <p style="margin: 7px 0; color: #111827;"><strong>Matiere :</strong> %s</p>
                            <p style="margin: 7px 0; color: #111827;"><strong>Classe :</strong> %s</p>
                            <p style="margin: 7px 0; color: #111827;"><strong>Questions :</strong> %s</p>
                            <p style="margin: 7px 0; color: #111827;"><strong>Duree :</strong> %s</p>
                            <p style="margin: 7px 0; color: #111827;"><strong>Date limite :</strong> %s</p>
                        </div>

                        <div style="text-align: center; margin: 28px 0 10px;">
                            <a href="%s"
                               style="background: linear-gradient(135deg, #ffcc33, #f5ad00);
                                      color: #111827; padding: 14px 30px; text-decoration: none;
                                      border-radius: 14px; font-weight: 800; display: inline-block;">
                                Voir les quiz disponibles
                            </a>
                        </div>
                    </div>
                </div>
                """.formatted(studentName, quizTitle, matiereName, classeName, questionCount,
                timeLimit, deadline, quizUrl);

        return sendHtmlEmail(student.getEmail(), subject, htmlContent);
    }

    /**
     * ✅ Méthode générique pour envoyer un email HTML
     */
    private boolean sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (shouldUseGmailApi()) {
            return sendHtmlEmailWithGmailApi(toEmail, subject, htmlContent);
        }

        if (shouldUseResend()) {
            return sendHtmlEmailWithResend(toEmail, subject, htmlContent);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveFromEmail());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);
            System.out.println("✅ [EmailService] Email envoyé à : " + toEmail);
            return true;
        } catch (Exception e) {
            // On log l'erreur mais on ne bloque pas la création du compte
            System.err.println("❌ [EmailService] Erreur envoi email à " + toEmail + " : " + e.getMessage());
            return false;
        }
    }

    private boolean sendHtmlEmailWithGmailApi(String toEmail, String subject, String htmlContent) {
        String sender = resolveFromEmail();

        if (!isConfigured(gmailClientId) || !isConfigured(gmailClientSecret) || !isConfigured(gmailRefreshToken)) {
            System.err.println("[EmailService] Variables Gmail API manquantes. Email non envoye a : " + toEmail);
            return false;
        }
        if (!isConfigured(sender)) {
            System.err.println("[EmailService] GMAIL_FROM ou EMAIL_FROM manquant. Email non envoye a : " + toEmail);
            return false;
        }

        try {
            String accessToken = fetchGmailAccessToken();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("raw", buildGmailRawMessage(sender, toEmail, subject, htmlContent));

            String userId = isConfigured(gmailUserId) ? gmailUserId.trim() : "me";
            String sendUrl = String.format(gmailSendUrl, userId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(sendUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[EmailService] Email envoye via Gmail API a : " + toEmail);
                return true;
            }

            System.err.println("[EmailService] Gmail API a refuse l'email a " + toEmail
                    + " (HTTP " + response.statusCode() + ") : " + abbreviate(response.body()));
            return false;
        } catch (Exception e) {
            System.err.println("[EmailService] Erreur envoi Gmail API a " + toEmail + " : " + e.getMessage());
            return false;
        }
    }

    private String fetchGmailAccessToken() throws Exception {
        String form = "client_id=" + formEncode(gmailClientId.trim())
                + "&client_secret=" + formEncode(gmailClientSecret.trim())
                + "&refresh_token=" + formEncode(gmailRefreshToken.trim())
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder(URI.create(gmailTokenUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Gmail token refuse HTTP " + response.statusCode()
                    + " : " + abbreviate(response.body()));
        }

        JsonNode json = objectMapper.readTree(response.body());
        String accessToken = json.path("access_token").asText("");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("Gmail token absent dans la reponse");
        }
        return accessToken;
    }

    private String buildGmailRawMessage(String sender, String toEmail, String subject, String htmlContent)
            throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(sender);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        message.writeTo(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
    }

    private boolean sendHtmlEmailWithResend(String toEmail, String subject, String htmlContent) {
        String sender = resolveFromEmail();

        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.err.println("[EmailService] RESEND_API_KEY manquant. Email non envoye a : " + toEmail);
            return false;
        }
        if (sender == null || sender.isBlank()) {
            System.err.println("[EmailService] EMAIL_FROM manquant. Email non envoye a : " + toEmail);
            return false;
        }

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("from", sender);
            ArrayNode to = payload.putArray("to");
            to.add(toEmail);
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            HttpRequest request = HttpRequest.newBuilder(URI.create(resendApiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[EmailService] Email envoye via Resend a : " + toEmail);
                return true;
            }

            System.err.println("[EmailService] Resend a refuse l'email a " + toEmail
                    + " (HTTP " + response.statusCode() + ") : " + abbreviate(response.body()));
            return false;
        } catch (Exception e) {
            System.err.println("[EmailService] Erreur envoi Resend a " + toEmail + " : " + e.getMessage());
            return false;
        }
    }

    private String resolveFromEmail() {
        if (shouldUseGmailApi() && isConfigured(gmailFromEmail)) {
            return gmailFromEmail.trim();
        }
        String configured = configuredFromEmail == null ? "" : configuredFromEmail.trim();
        if (!configured.isBlank()) {
            return configured;
        }
        return fromEmail == null ? "" : fromEmail.trim();
    }

    private boolean shouldUseGmailApi() {
        String provider = resolveProvider();
        return "gmail-api".equalsIgnoreCase(provider) || "gmail".equalsIgnoreCase(provider);
    }

    private boolean shouldUseResend() {
        String provider = resolveProvider();
        if ("resend".equalsIgnoreCase(provider)) {
            return true;
        }
        return "auto".equalsIgnoreCase(provider)
                && resendApiKey != null
                && !resendApiKey.isBlank();
    }

    private String resolveProvider() {
        return emailProvider == null || emailProvider.isBlank() ? "resend" : emailProvider.trim();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }

    private String formEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String resolveMatiereName(Quiz quiz) {
        Matiere matiere = quiz.getMatiere();
        if (matiere != null && matiere.getNom() != null && !matiere.getNom().isBlank()) {
            return matiere.getNom();
        }
        return orDefault(quiz.getTheme(), "Matiere");
    }

    private String resolveClasseName(Quiz quiz) {
        Classe classe = quiz.getClasse();
        if (classe == null && quiz.getMatiere() != null) {
            classe = quiz.getMatiere().getClasse();
        }
        if (classe == null) {
            return "Classe non definie";
        }

        String name = orDefault(classe.getName(), "Classe");
        String filiere = classe.getFiliere();
        String niveau = classe.getNiveau();

        if (filiere != null && !filiere.isBlank()) {
            name += " - " + filiere;
        }
        if (niveau != null && !niveau.isBlank()) {
            name += " - " + niveau;
        }
        return name;
    }

    private String cleanFrontendUrl() {
        return frontendUrl == null ? "http://localhost:5174" : frontendUrl.replaceAll("/+$", "");
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
