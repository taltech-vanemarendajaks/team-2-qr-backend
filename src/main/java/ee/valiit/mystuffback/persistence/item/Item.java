package ee.valiit.mystuffback.persistence.item;

import ee.valiit.mystuffback.persistence.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "item", schema = "mystuff")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 50)
    @NotNull
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotNull
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Size(max = 250)
    @Column(name = "model", length = 250)
    private String model;

    @Size(max = 500)
    @Column(name = "comment", length = 500)
    private String comment;

    @Size(max = 1)
    @NotNull
    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "qr_token", length = 64, unique = true)
    private String qrToken;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "warranty_notify_at")
    private Instant warrantyNotifyAt;

}
