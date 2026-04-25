package api.giybat.uz.api.giybat.uz.entity;

import api.giybat.uz.api.giybat.uz.enums.SmsType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "email_type")
    @Enumerated(EnumType.STRING)
    private SmsType emailType;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;


}
