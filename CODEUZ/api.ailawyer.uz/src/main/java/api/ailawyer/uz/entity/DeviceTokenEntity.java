package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.DevicePlatform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Foydalanuvchi mobil qurilmasining FCM device tokeni.
 */
@Entity
@Table(
        name = "device_token",
        indexes = {
                @Index(name = "idx_device_token_profile_id", columnList = "profile_id"),
                @Index(name = "idx_device_token_token", columnList = "token")
        }
)
@Getter
@Setter
public class DeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private Integer profileId;

    @Column(name = "token", nullable = false, length = 512, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private DevicePlatform platform;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_used_date")
    private LocalDateTime lastUsedDate;
}
