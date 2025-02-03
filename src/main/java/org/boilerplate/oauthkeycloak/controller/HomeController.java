package org.boilerplate.oauthkeycloak.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Objects;

@Slf4j
@Controller("/")
public class HomeController {

    /**
     * main 화면 UI 을 보여주는 controller,
     * Authentication 을 통해 받아온 데이터가
     * @param model {@link Model}
     * @param auth {@link Authentication}
     * @return thymeleaf page {@code index.html}
     */
    @GetMapping
    public String home(Model model, Authentication auth) {
        model.addAttribute("name",
                auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc
                        ? oidc.getPreferredUsername()
                        : "");
        model.addAttribute("isAuthenticated",
                auth != null && auth.isAuthenticated());
        model.addAttribute("isNice",
                auth != null && auth.getAuthorities().stream().anyMatch(authority ->
                        Objects.equals("NICE", authority.getAuthority())));

        return "index";
    }

    // 인증된 유저만 해당 ui 에 접근 가능
    @GetMapping("/nice")
    public String nice(Model model, Authentication auth) {
        return "nice";
    }

    // keycloak 이라는 registrationId 를 가진 client 에 한하여 현재 세션에 user 가 존재한다면
    // 그 user 에 대한 access_token 값을 return, 없다면 keycloak login page 로 redirect
    // token 은 오로지 api server 간에 사용하는 것을 권장하므로 화면에서 사용하는걸 지양함.
    @GetMapping("/keycloak/token")
    public ResponseEntity<String> token(
            @RegisteredOAuth2AuthorizedClient("keycloak")
            OAuth2AuthorizedClient authorizedClient
    ) {
        log.info("{}", authorizedClient);
        return ResponseEntity.ok(authorizedClient.getAccessToken().getTokenValue());
    }
}
