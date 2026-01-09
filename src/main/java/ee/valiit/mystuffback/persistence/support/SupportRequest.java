package ee.valiit.mystuffback.persistence.support;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "support_request", schema = "mystuff")
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", length = 255)
    private String email;

    @NotNull
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "NEW";
    }
}
