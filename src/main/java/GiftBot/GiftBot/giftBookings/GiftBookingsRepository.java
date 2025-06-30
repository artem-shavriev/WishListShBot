package GiftBot.GiftBot.giftBookings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftBookingsRepository extends JpaRepository<GiftBookings, Long> {
    List<GiftBookings> findByUserId(long userId);

    GiftBookings findByGiftId(long giftId);
}
