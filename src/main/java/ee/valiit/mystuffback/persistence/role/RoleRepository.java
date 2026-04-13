package ee.valiit.mystuffback.persistence.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Query("select r from Role r where r.name = :roleName")
    Optional<Role> getRoleBy(String roleName);




}
