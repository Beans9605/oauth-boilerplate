package org.boilerplate.oauthkeycloak.security;

import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Authorities {

    /**
     * login 시도 및 성공 시 access_token 의 복호화 결과를 claims 로 받아
     * keycloak 에 있는 realm_access, 즉 realm 접근 가능한 groups, roles 등에 대한 정보를 추출하여
     * Spring Boot 내에서 사용할 수 있는 Authorities = 접근 가능 권한 으로 치환하여 사용하는 방법
     *
     * @return {@code Converter<Map<String, Object>, Collection<GrantedAuthority>>}
     * claims 값을 인자로 받고, 그 인자를 변환해서 {@code Collection<GrantedAuthority>} 값으로 만들어주는 함수 return bean
     */
    @Bean
    @SuppressWarnings("unchecked")
    public AuthoritiesConverter realmRolesAuthoritiesConverter() {
        // Convert FunctionalInterface 생성을 위한 과정
        return claims -> {
            // realm_access 값을 꼭 받아야함, 받지 못하면 exception = authorization failed
            var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
            // roles 값을 꼭 받아야함, 받지 못하면 exception = authorization failed
            var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
            // 받은 roles 를 GrantedAuthority class 의 role 값으로 넣어 List 반환
            return roles.stream().flatMap(Collection::stream)
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        };
    }

    /**
     * 받은 claim 값을 기준으로 등록된 bean 에 의해 roles 에 대한 GrantedAuthority 로 치환된 값을 이용하여
     * OidcUserAuthority 값에 spring boot 에서 필요한 roles 값을 authorities 값으로 치환해주는 과정
     * 정상적으로 인증된 결과에 Spring boot oauth2, OidcUserAuthority class 에서
     * SecurityFilter 값 내에 {@code hasAuthority} 체크를 하도록 만듦, 즉 roles 값으로 인가를 시행함.
     *
     * @param authoritiesConverter {{@link #realmRolesAuthoritiesConverter()}}
     * realm 접근 권한에 대해 Bean 에 등록된 메소드에 따른 result 를 param 으로 받음.
     * @return {@link GrantedAuthoritiesMapper}
     *
     */
    @Bean
    GrantedAuthoritiesMapper authenticationConverter(
            Converter<Map<String, Object>, Collection<GrantedAuthority>> authoritiesConverter) {
        // GrantedAuthorities 를 추출 및 비교를 위한 값으로 사용
        return (authorities) -> authorities.stream()
                // 정상적으로 인증 절차를 받고 받은 authority 값 중에 OidcUserAuthority 값으로 filtering 하여 사용
                .filter(authority -> authority instanceof OidcUserAuthority)
                // OAuth 인증을 받고 나온 유저 인가 값은 Oidc protocol 을 수행해 나온 결과이므로 OidcUserAuthority class 로 캐스팅
                .map(OidcUserAuthority.class::cast)
                // User 정보에서 IdToken 을 추출
                .map(OidcUserAuthority::getIdToken)
                // IdToken 으로 부터 claims 값을 추출
                .map(OidcIdToken::getClaims)
                // Bean 에 등록된 Authority converter 를 사용하여 roles 를 분리
                .map(authoritiesConverter::convert)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Bean
    SecurityFilterChain clientSecurityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        // login 에 대한 authorization_code, issuance access_token from refresh_token 등의
        // 동작을 모두 oauth2 로 등록된 서비스로 이관시키는 방법
        http.oauth2Login(Customizer.withDefaults());
        /*
            사용자에게는 두 개의 독립된 세션이 존재함

            1. keycloak 에서 관리하는 login session
            2. spring boot session 에 저장되는 state, session_state 등의 login session
                -> 따라서 session clustering 필요

            완전한 로그아웃을 위해서는 두 세션을 모두 종료시켜야지 되므로,
            registrationId 에 따른 spring boot 내 세션 제거
            keycloak 에 있는 session 제거 요청을 시도함
         */
        http.logout((logout) -> {
            var logoutSuccessHandler =
                    new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
            logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");
            logout.logoutSuccessHandler(logoutSuccessHandler);
        });

        http.authorizeHttpRequests(requests -> {
            // 해당 경로는 인증 절차를 거치지 않음
            requests.requestMatchers("/", "/favicon.ico").permitAll();
            // nice 경로는 NICE 라는 role 을 keycloak 에서 가지고 있어야만 접근할 수 있음.
            // GrantedAuthoritiesMapper 를 통해 authorities 에 NICE 가 있는지 확인
            requests.requestMatchers("/nice").hasAuthority("NICE");
            // 그 외 다른 경로는 전부 비활성화 상태, 접근할 수 없음
            requests.anyRequest().denyAll();
        });

        return http.build();
    }
}
