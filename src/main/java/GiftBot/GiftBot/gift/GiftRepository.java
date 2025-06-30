package GiftBot.GiftBot.gift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findAllByAvailable(Boolean available);

    Optional<Gift> findByName(String name);

    @Query("SELECT g.name FROM Gift g WHERE g.id IN :ids")
    List<String> findAllByIdList(@Param("ids") List<Long> ids);
}
