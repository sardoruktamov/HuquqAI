package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {

    List<DeviceTokenEntity> findAllByProfileIdAndActiveTrue(Integer profileId);

    Optional<DeviceTokenEntity> findByProfileIdAndToken(Integer profileId, String token);

    Optional<DeviceTokenEntity> findByToken(String token);
}
