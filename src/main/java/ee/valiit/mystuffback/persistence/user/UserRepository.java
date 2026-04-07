package ee.valiit.mystuffback.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select u from User u where u.email = :email and u.status = 'A'")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    @Query("select u from User u where u.googleId = :googleId and u.status = 'A'")
    Optional<User> findActiveUserByGoogleId(@Param("googleId") String googleId);

    boolean existsByEmail(String email);

}