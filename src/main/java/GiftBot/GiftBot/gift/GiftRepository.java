package GiftBot.GiftBot.gift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findAllByAvailable(Boolean available);

    Optional<Gift> findByName(String name);
}
