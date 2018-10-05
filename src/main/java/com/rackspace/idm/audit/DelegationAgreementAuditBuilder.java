package com.rackspace.idm.audit;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;

public class DelegationAgreementAuditBuilder {
        String delegateId;
        String delegateType;
        String delegationAgreementId;
        String roleId;
        RoleAssignments roleAssignments;

        public DelegationAgreementAuditBuilder delegationAgreementId(String delegationAgreementId) {
            this.delegationAgreementId = delegationAgreementId;
            return this;
        }

        public DelegationAgreementAuditBuilder delegateType(String delegateType) {
            this.delegateType = delegateType;
            return this;
        }

        public DelegationAgreementAuditBuilder delegateId(String delegateId) {
            this.delegateId = delegateId;
            return this;
        }

        public DelegationAgreementAuditBuilder roleAssignments(RoleAssignments roleAssignments) {
            this.roleAssignments = roleAssignments;
            return this;
        }

        public DelegationAgreementAuditBuilder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public String build() {
            StringBuilder stringBuilder = new StringBuilder();

            if (delegationAgreementId != null) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append("DelegationAgreement(id=");
                stringBuilder.append(delegationAgreementId);
                stringBuilder.append(")");
            }

            if (delegateId != null || delegateType != null) {
                StringBuilder stringBuilderForDelegateRef = new StringBuilder();
                if (delegateId != null) {
                    stringBuilderForDelegateRef.append("id=");
                    stringBuilderForDelegateRef.append(delegateId);
                }
                if (delegateType != null) {
                    if (stringBuilderForDelegateRef.length() > 0) {
                        stringBuilderForDelegateRef.append(",");
                    }
                    stringBuilderForDelegateRef.append("type=");
                    stringBuilderForDelegateRef.append(delegateType);
                }
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append("DelegateReference(");

                stringBuilder.append(stringBuilderForDelegateRef.toString());

                stringBuilder.append(")");
            }

            if (roleAssignments != null) {
                for (TenantAssignment tenantAssignment : roleAssignments.getTenantAssignments().getTenantAssignment()) {
                    StringBuilder stringBuilderForTenants = new StringBuilder();

                    for (String tenant : tenantAssignment.getForTenants()) {
                        if (stringBuilderForTenants.length() > 0) {
                            stringBuilderForTenants.append(",");
                        }
                        stringBuilderForTenants.append(tenant);
                    }

                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(" ");
                    }
                    stringBuilder.append("Role(id=");
                    stringBuilder.append(tenantAssignment.getOnRole());
                    stringBuilder.append(",tenants=[");
                    stringBuilder.append(stringBuilderForTenants.toString());
                    stringBuilder.append("])");
                }
            }

            if (roleId != null) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append("Role(id=");
                stringBuilder.append(roleId);
                stringBuilder.append(")");
            }

            return stringBuilder.toString();
        }
    }
