package ee.valiit.mystuffback.persistence.user;

import ee.valiit.mystuffback.persistence.role.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "\"user\"", schema = "mystuff")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @NotNull
    @Column(name = "username", nullable = false, length = 50)
    private String username;


    @Column(name = "password", length = 255)
    private String password;

    @NotNull
    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @NotNull
    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "google_uid", length = 255, unique = true)
    private String googleId;

}