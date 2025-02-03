package org.boilerplate.oauthkeycloak.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

// 편의상 interface 따로 구분
public interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {
}
