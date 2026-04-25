package api.giybat.uz.api.giybat.uz.service;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.enums.SmsType;
import api.giybat.uz.api.giybat.uz.exps.AppBadException;
import api.giybat.uz.api.giybat.uz.util.JwtUtil;
import api.giybat.uz.api.giybat.uz.util.RandomUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        String subject = "Ro'yxatdan o'tish";
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
                "<h1>Ro'yxatdan o'tish</h1>\n" +
                "<p>Ro'yxatdan o'tishni yakunlash uchun tugmani bosing:\n" +
                "    <a  class=\"tugma\"\n style=`hover:  color: white;background-color: darkgray;`" +
                "            href=\"%s/auth/registration/email-verification/%s?lang=%s\" target=\"_blank\">tasdiqlash</a></p>\n" +
                "</body>\n" +
                "</html>";

        body = String.format(body,serverDomain, JwtUtil.encode(profileId), lang.name());
        sendMimeEmail(email,subject,body);
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

    private void sendMimeEmail(String email, String subject, String body){

        MimeMessage msg = javaMailSender.createMimeMessage();
        try {
            msg.setFrom(fromAccount);

            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true);

            // threadga olish
            CompletableFuture.runAsync(() ->{
                javaMailSender.send(msg);
            });
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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
