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

package io.fireflysoftware.domain.insurance.policies.core.constants;

/**
 * Names shared by the policy sagas, their steps, and the saga engine.
 */
public final class PolicySagaConstants {

    public static final String SAGA_REGISTER = "RegisterPolicySaga";
    public static final String SAGA_UPDATE_STATUS = "UpdatePolicyStatusSaga";

    public static final String STEP_REGISTER = "registerPolicy";
    public static final String STEP_RECORD_INITIAL_STATUS = "recordInitialStatus";
    public static final String STEP_RETRIEVE = "retrievePolicy";
    public static final String STEP_UPDATE_STATUS = "updatePolicyStatus";
    public static final String STEP_RECORD_STATUS_HISTORY = "recordStatusHistory";

    public static final String COMPENSATE_REMOVE = "removePolicy";
    public static final String COMPENSATE_REMOVE_INITIAL_STATUS = "removeInitialStatus";
    public static final String COMPENSATE_RESTORE = "restorePolicyStatus";
    public static final String COMPENSATE_REMOVE_STATUS_HISTORY = "removeStatusHistory";

    public static final String EVENT_REGISTERED = "policies.registered";
    public static final String EVENT_INITIAL_STATUS_RECORDED = "policies.status.recorded";
    public static final String EVENT_RETRIEVED = "policies.retrieved";
    public static final String EVENT_STATUS_UPDATED = "policies.status.updated";
    public static final String EVENT_STATUS_HISTORY_RECORDED = "policies.status.history.recorded";

    public static final String CTX_RECORD = "policyIdRecord";
    public static final String CTX_PREVIOUS_STATUS = "previousPolicyStatus";

    private PolicySagaConstants() {
    }
}
