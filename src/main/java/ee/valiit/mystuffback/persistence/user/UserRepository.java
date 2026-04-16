package ee.valiit.mystuffback.persistence.user;

import ee.valiit.mystuffback.controller.admin.dto.AdminUserDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select u from User u where u.email = :email and u.status = 'A'")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    @Query("select u from User u where u.googleId = :googleId and u.status = 'A'")
    Optional<User> findActiveUserByGoogleId(@Param("googleId") String googleId);

    boolean existsByEmail(String email);

    @Query("""
        select new ee.valiit.mystuffback.controller.admin.dto.AdminUserDto(
            u.id,
            u.username,
            u.email,
            u.status,
            u.role.name,
            u.authProvider,
            u.createdAt,
            u.lastLoginAt,
            count(i)
        )
        from User u
        left join Item i on i.user.id = u.id and i.status = 'A'
        group by u.id, u.username, u.email, u.status, u.role.name, u.authProvider, u.createdAt, u.lastLoginAt
        order by u.id
    """)
    List<AdminUserDto> findAllUsersWithStats();

}