package com.example.springusr.registration;

import com.example.springusr.appuser.AppUser;
import com.example.springusr.appuser.AppUserRole;
import com.example.springusr.appuser.AppUserService;
import com.example.springusr.email.EmailService;
import com.example.springusr.registration.token.ConfirmationToken;
import com.example.springusr.registration.token.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class RegistrationService {

    private final AppUserService appUserService;
    private final EmailValidator emailValidator;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailService emailService;

    @Transactional
    public String register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.test(request.getEmail());
        if (!isValidEmail)
            throw new IllegalStateException("email not valid");

        String token = appUserService.signUpUser(
                new AppUser(
                        request.getFirstname(),
                        request.getLastname(),
                        request.getEmail(),
                        request.getPassword(),
                        AppUserRole.USER
                )
        );
        String url = "http://localhost:8080/api/v1/registration/confirm?token=" + token;
        emailService.send(request.getEmail(), buildemail(request.getEmail(), url));
        return buildemail(request.getEmail(), url);
    }

    public String buildemail(String email, String url) {
        return "<?DOCTYPE html>" +
                "<body>" +
                "" + email +
                "<br/>" +
                "<a href=" + url +">"+url+"</a>"+
                "</body>" +
                "</html> ";
    }

    @Transactional
    public String confirm(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(()->
                        new IllegalStateException("no token found")
                );
        if(confirmationToken.getConfirmedAt() != null)
            throw new IllegalStateException("already confirmed");

        LocalDateTime expiresAt = confirmationToken.getExpiresAt();

        if(expiresAt.isBefore(LocalDateTime.now()))
            throw new IllegalStateException("confirmation token expired");

        confirmationTokenService.setConfirmedAt(token);

        appUserService.enableUser(confirmationToken.getAppUser().getEmail());
        return "<?DOCTYPE html>" +
                "<body>" +
                "<h1>Confirmed</h1>" +
                "</body>" +
                "</html>";
    }
}
