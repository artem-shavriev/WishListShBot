package GiftBot.GiftBot.user;

import GiftBot.GiftBot.gift.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT g FROM User u JOIN u.usersGifts g " +
            "WHERE u.userName = :userName AND g.available = :available")
    List<Gift> findUsersGiftsByAvailable(@Param("available") boolean available,
                                         @Param("userName") String userName);

    Optional<User> findByUserName(String userName);

    Optional <List<User>> findAllByIdNot(long id);
}
