package ee.valiit.mystuffback.persistence.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

import java.util.List;

    public interface ItemRepository extends JpaRepository<Item, Integer> {

        @Query("select i from Item i  where i.user.id = :userId and i.status = 'A' order by i.date, i.name")
        List<Item> findActiveItemsBy(Integer userId);

        @Query("select (count(i) > 0) from Item i where i.name = :itemName")
        boolean itemExistsBy(String itemName);

        @Query("select i from Item i where i.qrToken = :qrToken and i.status = 'A'")
        Optional<Item> findActiveItemByQrToken(String qrToken);


    }

