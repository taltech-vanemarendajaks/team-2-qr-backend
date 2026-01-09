package ee.valiit.mystuffback.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select u from User u where u.username = :username and u.status = 'A'")
    Optional<User> findActiveUserByUsername(@Param("username") String username);

    @Query("select (count(u) > 0) from User u where u.username = :username")
    boolean usernameExistsBy(@Param("username") String username);

    @Query("select (count(u) > 0) from User u where u.email = :email")
    boolean emailExistsBy(@Param("email") String email);



}