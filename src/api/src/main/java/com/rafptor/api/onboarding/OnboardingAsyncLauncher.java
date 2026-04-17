package com.rafptor.api.onboarding;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Separate Spring-managed bean so that {@link Async} is intercepted by the AOP proxy.
 * Self-invocation within {@link OnboardingController} would bypass the proxy.
 */
@Component
public class OnboardingAsyncLauncher {

    private final AgentDeploymentService deploymentService;

    public OnboardingAsyncLauncher(AgentDeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @Async("deploymentExecutor")
    public void deploy(ConnectRequest req, String connectionId, String tenantId) {
        deploymentService.deploy(req, connectionId, tenantId);
    }
}
