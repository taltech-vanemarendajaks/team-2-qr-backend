package ee.valiit.mystuffback.service;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailtrapClient mailtrapClient;

    @Value("${mailtrap.from-email}")
    private String fromEmail;

    @Value("${mailtrap.from-name}")
    private String fromName;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MailtrapMail mail = MailtrapMail.builder()
                    .from(new Address(fromEmail, fromName))
                    .to(List.of(new Address(toEmail)))
                    .subject("Reset your " + fromName + " password")
                    .html(buildResetEmailHtml(resetLink))
                    .text("Click this link to reset your password (valid for 1 hour):\n" + resetLink +
                          "\n\nIf you did not request this, you can safely ignore this email.")
                    .build();
            mailtrapClient.send(mail);
        } catch (Exception e) {
            // Never propagate — forgot-password must always return 200 regardless of email failures
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildResetEmailHtml(String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Reset your password</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f8f6f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f8f6f2;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table width="100%" cellpadding="0" cellspacing="0" style="max-width:480px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(19,79,92,0.10);">

                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#134f5c,#1b6a79);padding:36px 40px;text-align:center;">
                              <p style="margin:0;font-size:28px;font-weight:700;letter-spacing:0.12em;color:#ffffff;">""" + fromName + """
                              </p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <h1 style="margin:0 0 12px;font-size:20px;font-weight:600;color:#1a1a1a;">Reset your password</h1>
                              <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#6c645d;">
                                We received a request to reset the password for your account.
                                Click the button below to choose a new one. This link is valid for <strong>1 hour</strong>.
                              </p>

                              <!-- CTA button -->
                              <table width="100%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center">
                                    <a href=\"""" + resetLink + """
                                \"
                                       style="display:inline-block;padding:14px 32px;background:linear-gradient(135deg,#134f5c,#1b6a79);color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;letter-spacing:0.06em;border-radius:12px;">
                                      Reset password
                                    </a>
                                  </td>
                                </tr>
                              </table>

                              <!-- Fallback link -->
                              <p style="margin:24px 0 0;font-size:13px;color:#6c645d;line-height:1.5;">
                                If the button doesn't work, copy and paste this link into your browser:
                              </p>
                              <p style="margin:6px 0 0;font-size:12px;word-break:break-all;">
                                <a href=\"""" + resetLink + """
                                \" style="color:#134f5c;">""" + resetLink + """
                                </a>
                              </p>
                            </td>
                          </tr>

                          <!-- Divider -->
                          <tr>
                            <td style="padding:0 40px;">
                              <hr style="border:none;border-top:1px solid #ede9e3;margin:0;" />
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:24px 40px 32px;">
                              <p style="margin:0;font-size:13px;color:#6c645d;line-height:1.5;">
                                If you didn't request a password reset, you can safely ignore this email — your password will not change.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """;
    }
}
