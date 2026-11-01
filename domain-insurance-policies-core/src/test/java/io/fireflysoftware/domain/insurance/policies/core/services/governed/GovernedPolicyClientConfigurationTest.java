package io.fireflysoftware.domain.insurance.policies.core.services.governed;

import io.fireflysoftware.domain.insurance.policies.infra.governed.GovernedPolicyClientFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyClientConfigurationTest {
    @Test void defaultOffCreatesNoGovernedServiceOrClientFactory(){
        new ApplicationContextRunner().withUserConfiguration(GovernedPolicyClientConfiguration.class).run(context->{
            assertNull(context.getStartupFailure());assertEquals(0,context.getBeanNamesForType(GovernedPolicyService.class).length);assertEquals(0,context.getBeanNamesForType(GovernedPolicyClientFactory.class).length);
        });
    }
    @Test void enabledWithoutExplicitServerBindingFailsClosed(){
        new ApplicationContextRunner().withUserConfiguration(GovernedPolicyClientConfiguration.class).withPropertyValues("insurance.policies.governed-client.enabled=true")
                .run(context->assertNotNull(context.getStartupFailure()));
    }
    @Test void realConfiguredGraphConstructsWithoutIoAndStillRequiresNativeCaller(){
        var calls=new AtomicInteger();
        new ApplicationContextRunner().withUserConfiguration(GovernedPolicyClientConfiguration.class)
                .withBean(ReactiveClientRegistrationRepository.class,()->id->{calls.incrementAndGet();return Mono.empty();})
                .withPropertyValues("insurance.policies.governed-client.enabled=true",
                        "insurance.policies.governed-client.issuer=https://issuer.invalid","insurance.policies.governed-client.jwk-set-uri=https://issuer.invalid/jwks",
                        "insurance.policies.governed-client.domain-audience=domain-policy","insurance.policies.governed-client.inbound-client-id=experience-policy",
                        "insurance.policies.governed-client.signed-tenant=opaque-platform-namespace","insurance.policies.governed-client.resource-tenant-id=10000000-0000-0000-0000-000000000001",
                        "insurance.policies.governed-client.binding-id=10000000-0000-0000-0000-000000000002","insurance.policies.governed-client.binding-version=1",
                        "insurance.policies.governed-client.binding-digest="+"a".repeat(64),"insurance.policies.governed-client.not-after=2099-01-01T00:00:00Z",
                        "insurance.policies.governed-client.endpoint=https://core-policy.invalid","insurance.policies.governed-client.target-audience=core-policy",
                        "insurance.policies.governed-client.delegating-client-id=domain-policy-exchange","insurance.policies.governed-client.registration-id=policy-exchange")
                .run(context->{assertNull(context.getStartupFailure());assertNotNull(context.getBean(GovernedPolicyService.class));assertEquals(0,calls.get());
                    StepVerifier.create(context.getBean(GovernedPolicyClientFactory.class).requireCurrent(java.util.Set.of("insurance:policies:bind")))
                            .expectError(org.springframework.web.server.ResponseStatusException.class).verify();assertEquals(0,calls.get());});
    }
}
