package com.example.gateway.repo;

import com.example.gateway.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {
    @Modifying
    @Query("""
    UPDATE DeviceEntity d
    SET d.sentryStatus = 'OK',
        d.sentryError = null,
        d.sentryLastCheck = CURRENT_TIMESTAMP
    WHERE d.serialNumber = :sn
  """)
    void markSentryOk(@Param("sn") String sn);

    @Modifying
    @Query("""
    UPDATE DeviceEntity d
    SET d.sentryStatus = 'DEAD',
        d.sentryError = :err,
        d.sentryLastCheck = CURRENT_TIMESTAMP
    WHERE d.serialNumber = :sn
  """)
    void markSentryDead(@Param("sn") String sn, @Param("err") String err);

}
