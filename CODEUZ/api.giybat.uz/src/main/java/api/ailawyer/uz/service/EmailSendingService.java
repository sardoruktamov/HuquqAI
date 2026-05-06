package api.ailawyer.uz.service;
import api.ailawyer.uz.enums.AppLanguage;
import api.ailawyer.uz.enums.SmsType;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.util.JwtUtil;
import api.ailawyer.uz.util.RandomUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailSendingService {

    Integer smsLimit = 1;
    @Value("${spring.mail.username}")
    private String fromAccount;

    @Value("${server.domain}")
    private String serverDomain;
    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailHistoryService emailHistoryService;

    @Autowired
    private ResourceBundleService bundleService;

    public void sendEmailForRegistration(String email, Integer profileId, AppLanguage lang){
        Long count = emailHistoryService.getEmailCount(email);
        if (count >= smsLimit) {
            throw new AppBadException(bundleService.getMessage("you.can.send.one.sms.code", lang));
        }

        String token = JwtUtil.encode(profileId);
        emailHistoryService.created(email, token, SmsType.REGISTRATION);

        String subject = "Ro'yxatdan o'tish";
        String body = """
        <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f3f4f6; padding: 40px 20px;">
            <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
                
                <div style="background-color: #2563eb; padding: 30px; text-align: center;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 28px; letter-spacing: 1px;">Qalqon AI</h1>
                </div>
                
                <div style="padding: 40px 30px; text-align: center;">
                    <h2 style="margin-top: 0; color: #1f2937; font-size: 22px;">Xush kelibsiz!</h2>
                    <p style="font-size: 16px; line-height: 1.6; color: #4b5563; margin-bottom: 35px;">
                        Platformamizdan ro'yxatdan o'tganingiz uchun rahmat. Ro'yxatdan o'tishni yakunlash va hisobingizni faollashtirish uchun quyidagi tugmani bosing:
                    </p>
                    
                    <a href="%s/api/v1/auth/registration/email-verification/%s?lang=%s"
                       style="display: inline-block; background-color: #10b981; color: #ffffff; text-decoration: none; padding: 15px 35px; font-size: 16px; font-weight: 600; border-radius: 6px;">
                        Hisobni tasdiqlash
                    </a>
                    
                    <p style="font-size: 13px; color: #9ca3af; margin-top: 40px; border-top: 1px solid #e5e7eb; padding-top: 20px;">
                        Agar ushbu xatni xatolik tufayli olgan bo'lsangiz, iltimos, uni shunchaki e'tiborsiz qoldiring.
                    </p>
                </div>
                
            </div>
        </div>
        """;

        body = String.format(body, serverDomain, token, lang.name());
        sendMimeEmail(email, subject, body);
    }

    public void sendResetPasswordEmail(String email, AppLanguage lang){
        String subject = "Parolni tiklash";
        String code = RandomUtil.getRandomSmsCode();
        String body = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Title</title>\n" +
                "    <style>\n" +
                "        a{\n" +
                "            padding: 10px 30px;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .tugma{\n" +
                "          text-decoration: none;\n" +
                "            color: darkslategrey;\n" +
                "            background-color: wheat;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .tugma:hover{\n" +
                "            color: white;\n" +
                "            background-color: darkgray;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>Parolni tiklash</h1>\n" +
                "<p style=`color: red`>Parolni tiklash uchun kod: <b>%s</b></p>\n" +
                "</body>\n" +
                "</html>";

        body = String.format(body,code);

        checkAndSendMimeEmail(email, code, subject, body, lang);
    }

    public void sendChangeUsernameEmail(String email, AppLanguage lang){
        String subject = "Username change confirm";
        String code = RandomUtil.getRandomSmsCode();
        String body = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Title</title>\n" +
                "    <style>\n" +
                "        a{\n" +
                "            padding: 10px 30px;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .tugma{\n" +
                "          text-decoration: none;\n" +
                "            color: darkslategrey;\n" +
                "            background-color: wheat;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .tugma:hover{\n" +
                "            color: white;\n" +
                "            background-color: darkgray;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>Loginni o'zgartirish</h1>\n" +
                "<p style=`color: red`>loginni o'zgartirish uchun tasdiqlash kodi: <b>%s</b></p>\n" +
                "</body>\n" +
                "</html>";

        body = String.format(body,code);

        checkAndSendMimeEmail(email, code, subject, body, lang);
    }

    private void checkAndSendMimeEmail(String email,String code, String subject, String body, AppLanguage lang){
        // check
        Long count = emailHistoryService.getEmailCount(email);
        if (count >= smsLimit){
            throw new AppBadException(bundleService.getMessage("you.can.send.one.sms.code",lang));
        }
        // create
        emailHistoryService.created(email,code, SmsType.RESET_PASSWORD);
        // send
        sendMimeEmail(email,subject,body);
    }

    private void sendMimeEmail(String email, String subject, String body) {
        try {
            MimeMessage msg = javaMailSender.createMimeMessage();
            msg.setFrom(fromAccount);

            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true);

            // CompletableFuture o‘rniga sinxron yuborish: SMTP xatolari asosiy threadda ko‘rinadi
            // va API javobida ham sezilishi mumkin (504/500 emas, lekin logda aniq).
            javaMailSender.send(msg);
            log.info("Email yuborildi: {}", email);
        } catch (MessagingException e) {
            log.error("Email yuborishda MessagingException: to={}, {}", email, e.getMessage(), e);
            throw new RuntimeException("Email yuborilmadi: " + e.getMessage(), e);
        } catch (org.springframework.mail.MailException e) {
            log.error("Email yuborishda MailException: to={}, {}", email, e.getMessage(), e);
            throw new RuntimeException("Email yuborilmadi: " + e.getMessage(), e);
        }
    }

    // 1-usul oddiy text formatida xabar yuborish
    private void sendOddiyTextEmail(String email, String subject, String body){
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAccount);
        msg.setTo(email);
        msg.setSubject(subject);
        msg.setText(body);
        javaMailSender.send(msg);

    }

}
