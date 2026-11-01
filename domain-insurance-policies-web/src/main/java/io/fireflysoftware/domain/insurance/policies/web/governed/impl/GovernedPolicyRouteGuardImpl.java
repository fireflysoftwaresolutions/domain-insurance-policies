package io.fireflysoftware.domain.insurance.policies.web.governed.impl;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.util.matcher.*;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

/** Explicit governed mode closes unsupported legacy paths before caller authentication. */
public final class GovernedPolicyRouteGuardImpl implements WebFilter {
    public static final ServerWebExchangeMatcher SUPPORTED=new OrServerWebExchangeMatcher(
            ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST,"/api/v1/policies/governed/intents","/api/v1/policies/governed/bindings","/api/v1/policies/*/governed/issue"),
            ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET,"/api/v1/policies/governed/recovery/*"));
    @Override public Mono<Void> filter(ServerWebExchange exchange,WebFilterChain chain){return SUPPORTED.matches(exchange).flatMap(result->{
        if(result.isMatch())return chain.filter(exchange);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);return exchange.getResponse().setComplete();
    });}
}
