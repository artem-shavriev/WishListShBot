/*DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS gifts CASCADE;
DROP TABLE IF EXISTS users_gifts CASCADE;*/

create TABLE IF NOT EXISTS users (
  id BIGINT,
  name VARCHAR(250) NOT NULL,
  user_name VARCHAR(250) NOT NULL,
  registered_at Timestamp NOT NULL,
  CONSTRAINT pk_user PRIMARY KEY (id),
  CONSTRAINT UQ_USER_user_name UNIQUE (user_name)
);

create TABLE IF NOT EXISTS gifts (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name VARCHAR(250) NOT NULL,
    link VARCHAR(1000) NOT NULL,
    available boolean NOT NULL,
    CONSTRAINT pk_gifts PRIMARY KEY (id)
);

create TABLE IF NOT EXISTS users_gifts (
    user_id BIGINT NOT NULL,
    gift_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, gift_id),
    CONSTRAINT fk_users_gifts_to_user FOREIGN KEY (user_id) REFERENCES users(id) ON delete CASCADE,
    CONSTRAINT fk_users_gifts_to_gift FOREIGN KEY (gift_id) REFERENCES gifts(id) ON delete CASCADE
);

create TABLE IF NOT EXISTS gift_bookings (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    gift_id BIGINT NOT NULL,
    PRIMARY KEY (id, user_id, friend_id, gift_id),
    CONSTRAINT fk_gift_bookings_to_user FOREIGN KEY (user_id) REFERENCES users(id) ON delete CASCADE,
    CONSTRAINT fk_gift_bookings_to_friend FOREIGN KEY (friend_id) REFERENCES users(id) ON delete CASCADE,
    CONSTRAINT fk_gift_bookings_to_gift FOREIGN KEY (gift_id) REFERENCES gifts(id) ON delete CASCADE
);