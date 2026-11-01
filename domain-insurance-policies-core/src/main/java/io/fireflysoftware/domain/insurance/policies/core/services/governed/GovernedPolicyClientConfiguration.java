package io.fireflysoftware.domain.insurance.policies.core.services.governed;

import io.fireflysoftware.domain.insurance.policies.core.services.governed.impl.*;
import io.fireflysoftware.domain.insurance.policies.infra.governed.*;
import io.fireflysoftware.domain.insurance.policies.infra.governed.impl.ExchangedGovernedPolicyClientFactoryImpl;
import io.fireflysoftware.domain.insurance.policies.interfaces.governed.GovernedPolicyTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="insurance.policies.governed-client",name="enabled",havingValue="true")
@EnableConfigurationProperties(GovernedPolicyClientSettings.class)
public class GovernedPolicyClientConfiguration {
    @Bean("governedPolicyDomainReceiverDecoder")
    ReactiveJwtDecoder governedPolicyDomainReceiverDecoder(GovernedPolicyClientSettings settings){
        return ExchangedGovernedPolicyClientFactoryImpl.decoder(settings,settings.domainAudience());
    }
    @Bean
    GovernedPolicyClientFactory governedPolicyClientFactory(GovernedPolicyClientSettings settings,ReactiveClientRegistrationRepository registrations){
        return new ExchangedGovernedPolicyClientFactoryImpl(settings,registrations);
    }
    @Bean
    GovernedPolicyTransport governedPolicyTransport(GovernedPolicyClientFactory clients){return new GeneratedGovernedPolicyTransportImpl(clients);}
    @Bean
    GovernedPolicyService governedPolicyService(GovernedPolicyTransport transport){return new GovernedPolicyServiceImpl(transport);}
}
