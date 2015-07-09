package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlOTPDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface OTPDeviceRepository extends JpaSpecificationExecutor<SqlOTPDevice>, JpaRepository<SqlOTPDevice, String> {

    SqlOTPDevice findByIdAndUserId(String id, String userId);

    List<SqlOTPDevice> findByUserId(String userId);

    List<SqlOTPDevice> findByUserIdAndMultiFactorDeviceVerified(String userId, boolean multiFactorDeviceVerified);

    int countByUserId(String userId);

    int countByUserIdAndMultiFactorDeviceVerified(String userId, boolean multiFactorDeviceVerified);

    void deleteByUserId(String userId);

}
