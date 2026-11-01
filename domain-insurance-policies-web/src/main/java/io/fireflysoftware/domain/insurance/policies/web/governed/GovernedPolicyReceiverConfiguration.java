package io.fireflysoftware.domain.insurance.policies.web.governed;

import io.fireflysoftware.domain.insurance.policies.web.governed.impl.GovernedPolicyRouteGuardImpl;
import java.util.List;
import org.fireflyframework.security.core.authority.*;
import org.fireflyframework.security.core.context.PrincipalFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.authorization.*;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/** Uses native verified JWT plus rich principal without replacing the application's global security chain. */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="insurance.policies.governed-client",name="enabled",havingValue="true")
public class GovernedPolicyReceiverConfiguration {
    @Bean @Order(-60)
    GovernedPolicyRouteGuardImpl governedPolicyOnlyRoutes(){return new GovernedPolicyRouteGuardImpl();}
    @Bean @Order(-55)
    ExceptionTranslationWebFilter governedPolicyExceptionTranslation(){var filter=new ExceptionTranslationWebFilter();filter.setAuthenticationEntryPoint(new BearerTokenServerAuthenticationEntryPoint());return filter;}
    @Bean @Order(-50)
    AuthenticationWebFilter governedPolicyAuthentication(@Qualifier("governedPolicyDomainReceiverDecoder") ReactiveJwtDecoder decoder){
        var principals=new PrincipalFactory(new ConfigurableAuthorityMapper(new AuthorityMappingProperties(List.of("roles"),List.of("scope"),"")));
        var manager=new JwtReactiveAuthenticationManager(decoder);
        manager.setJwtAuthenticationConverter(jwt->Mono.fromSupplier(()->{
            Object raw=jwt.getClaims().get("tenant_id");
            if(!(raw instanceof String tenant) || tenant.isBlank() || tenant.length()>512 || tenant.codePoints().anyMatch(Character::isISOControl))
                throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
            var principal=principals.fromClaims(jwt.getSubject(),jwt.getIssuer().toString(),jwt.getClaims()).toBuilder().tenantId(tenant).build();
            return new JwtAuthenticationToken(jwt,principal,principal.authorities().stream().map(SimpleGrantedAuthority::new).toList());
        }));
        var filter=new AuthenticationWebFilter(manager);filter.setServerAuthenticationConverter(new ServerBearerTokenAuthenticationConverter());
        filter.setRequiresAuthenticationMatcher(GovernedPolicyRouteGuardImpl.SUPPORTED);filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        filter.setAuthenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(new BearerTokenServerAuthenticationEntryPoint()));return filter;
    }
    @Bean @Order(-40)
    AuthorizationWebFilter governedPolicyAuthorization(){return new AuthorizationWebFilter(AuthenticatedReactiveAuthorizationManager.authenticated());}
}
