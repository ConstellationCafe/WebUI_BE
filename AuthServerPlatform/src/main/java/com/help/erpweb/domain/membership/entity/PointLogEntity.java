package com.help.erpweb.domain.membership.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PayLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Access(AccessType.FIELD)
@IdClass(PointLogId.class)
public class PointLogEntity {
    @Id
    @Column(name = "sk", nullable = false, columnDefinition = "TEXT")
    private String sk;

    @Id
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Id
    @Column(name = "at", nullable = false)
    private LocalDateTime at;

    @Column(name = "description", nullable = true)
    private String description;

    public static PointLogEntity of(String sk, Integer amount, LocalDateTime at, String description) {
        return new PointLogEntity(sk, amount, at, description);
    }
}
