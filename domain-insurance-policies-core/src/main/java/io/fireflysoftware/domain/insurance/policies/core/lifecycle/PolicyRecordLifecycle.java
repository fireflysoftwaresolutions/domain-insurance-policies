/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.insurance.policies.core.lifecycle;

import io.fireflysoftware.core.insurance.policies.sdk.model.PolicyDTO;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Moves copied from the policy lifecycle on {@code core-insurance-policies}.
 * A missing status is treated as QUOTE, which is the only status the core will delete.
 */
public final class PolicyRecordLifecycle {

    private static final Map<PolicyDTO.PolicyStatusEnum, Set<PolicyDTO.PolicyStatusEnum>> ALLOWED = Map.of(
            PolicyDTO.PolicyStatusEnum.QUOTE, EnumSet.of(PolicyDTO.PolicyStatusEnum.BOUND, PolicyDTO.PolicyStatusEnum.CANCELLED),
            PolicyDTO.PolicyStatusEnum.BOUND, EnumSet.of(PolicyDTO.PolicyStatusEnum.IN_FORCE, PolicyDTO.PolicyStatusEnum.CANCELLED),
            PolicyDTO.PolicyStatusEnum.IN_FORCE, EnumSet.of(PolicyDTO.PolicyStatusEnum.LAPSED, PolicyDTO.PolicyStatusEnum.CANCELLED, PolicyDTO.PolicyStatusEnum.EXPIRED),
            PolicyDTO.PolicyStatusEnum.LAPSED, EnumSet.of(PolicyDTO.PolicyStatusEnum.IN_FORCE, PolicyDTO.PolicyStatusEnum.CANCELLED, PolicyDTO.PolicyStatusEnum.EXPIRED)
    );

    private PolicyRecordLifecycle() {
    }

    public static boolean canRemove(PolicyDTO.PolicyStatusEnum status) {
        return status == null || status == PolicyDTO.PolicyStatusEnum.QUOTE;
    }

    public static boolean canTransition(PolicyDTO.PolicyStatusEnum from, PolicyDTO.PolicyStatusEnum to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Set<PolicyDTO.PolicyStatusEnum> next = ALLOWED.get(from);
        return next != null && next.contains(to);
    }
}
