package com.help.backend.domain.membership.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PointLogId implements Serializable {
    private String sk;
    private Integer amount;
    private LocalDateTime at;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PointLogId)) return false;
        PointLogId that = (PointLogId) o;
        return Objects.equals(sk, that.sk)
                && Objects.equals(amount, that.amount)
                && Objects.equals(at, that.at);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sk, amount, at);
    }
}
