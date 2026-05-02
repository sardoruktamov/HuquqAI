package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
@Getter
@Setter
public class CaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private ProfileEntity client;

    @Column(name = "lawyer_id")
    private Integer lawyerId; // Hozircha null bo'ladi (AI uchun). Keyinchalik advokat jalb qilinganda to'ldiriladi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", insertable = false, updatable = false)
    private ProfileEntity lawyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CaseStatus status;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}