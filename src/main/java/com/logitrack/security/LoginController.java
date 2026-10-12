package com.logitrack.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final DemoLoginProperties demoLoginProperties;

    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        boolean demoEnabled = demoLoginProperties != null && demoLoginProperties.enabled();
        model.addAttribute("demoLogin", demoEnabled);
        if (demoEnabled) {
            model.addAttribute("demoUsername", demoLoginProperties.username());
            model.addAttribute("demoPassword", demoLoginProperties.password());
        }

        return "security/login";
    }
}
