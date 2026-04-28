package ee.valiit.mystuffback.persistence.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

    public interface ItemRepository extends JpaRepository<Item, Integer> {

        @Query("select i from Item i  where i.user.id = :userId and i.status = 'A' order by i.date, i.name")
        List<Item> findActiveItemsByUserId(@Param("userId") Integer userId);

        @Query("select i from Item i where i.id = :itemId and i.user.id = :userId and i.status = 'A'")
        Optional<Item> findActiveItemByIdAndUserId(@Param("itemId") Integer itemId,
                                                   @Param("userId") Integer userId);

        @Query("select i from Item i where i.qrToken = :qrToken and i.status = 'A'")
        Optional<Item> findActiveItemByQrToken(@Param("qrToken") String qrToken);

        @Query("select i from Item i where i.status = 'A' and i.warrantyNotifyAt is not null and i.warrantyNotifyAt <= :now")
        List<Item> findItemsDueForWarrantyNotification(@Param("now") Instant now);

    }

