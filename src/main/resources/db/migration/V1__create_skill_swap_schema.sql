
-- SkillSwap: esquema inicial de persistencia.
-- Compatible con MySQL 8.0.16 o posterior.

CREATE TABLE profiles (
    id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    biography TEXT,
    time_zone VARCHAR(100) NOT NULL,
    reputation DECIMAL(3,2) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_profile_reputation
        CHECK (reputation >= 0 AND reputation <= 5),
    CONSTRAINT chk_profile_rating_count
        CHECK (rating_count >= 0)
);

CREATE TABLE profile_skills (
    profile_id VARCHAR(100) NOT NULL,
    skill VARCHAR(150) NOT NULL,
    PRIMARY KEY (profile_id, skill),
    CONSTRAINT fk_profile_skills_profile
        FOREIGN KEY (profile_id) REFERENCES profiles(id)
);

CREATE TABLE profile_interests (
    profile_id VARCHAR(100) NOT NULL,
    interest VARCHAR(150) NOT NULL,
    PRIMARY KEY (profile_id, interest),
    CONSTRAINT fk_profile_interests_profile
        FOREIGN KEY (profile_id) REFERENCES profiles(id)
);

CREATE TABLE profile_notification_channels (
    profile_id VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    PRIMARY KEY (profile_id, channel),
    CONSTRAINT fk_profile_channels_profile
        FOREIGN KEY (profile_id) REFERENCES profiles(id),
    CONSTRAINT chk_profile_channel
        CHECK (channel IN ('EMAIL', 'PUSH', 'IN_APP'))
);

CREATE TABLE profile_availability (
    id VARCHAR(100) PRIMARY KEY,
    profile_id VARCHAR(100) NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    capacity INT NOT NULL,
    reserved INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_profile_availability_profile
        FOREIGN KEY (profile_id) REFERENCES profiles(id),
    CONSTRAINT chk_profile_availability_dates
        CHECK (ends_at > starts_at),
    CONSTRAINT chk_profile_availability_capacity
        CHECK (capacity >= 1 AND reserved >= 0 AND reserved <= capacity),
    INDEX idx_profile_availability_dates (profile_id, starts_at, ends_at)
);

CREATE TABLE publications (
    id VARCHAR(100) PRIMARY KEY,
    author_user_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(150) NOT NULL,
    modality VARCHAR(20) NOT NULL,
    duration_minutes INT NOT NULL,
    capacity INT NOT NULL,
    base_price INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    last_requested_at DATETIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_publications_author
        FOREIGN KEY (author_user_id) REFERENCES profiles(user_id),
    CONSTRAINT chk_publication_modality
        CHECK (modality IN ('REMOTE', 'IN_PERSON', 'HYBRID')),
    CONSTRAINT chk_publication_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'PAUSED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT chk_publication_duration
        CHECK (duration_minutes > 0),
    CONSTRAINT chk_publication_capacity
        CHECK (capacity > 0),
    CONSTRAINT chk_publication_price
        CHECK (base_price >= 0),
    CONSTRAINT chk_publication_requests
        CHECK (request_count >= 0),
    INDEX idx_publications_author (author_user_id),
    INDEX idx_publications_discovery (status, category, modality, base_price, created_at)
);

CREATE TABLE publication_tags (
    publication_id VARCHAR(100) NOT NULL,
    tag VARCHAR(150) NOT NULL,
    PRIMARY KEY (publication_id, tag),
    CONSTRAINT fk_publication_tags_publication
        FOREIGN KEY (publication_id) REFERENCES publications(id)
);

CREATE TABLE publication_availability (
    id VARCHAR(100) PRIMARY KEY,
    publication_id VARCHAR(100) NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    capacity INT NOT NULL,
    reserved INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_publication_availability_publication
        FOREIGN KEY (publication_id) REFERENCES publications(id),
    CONSTRAINT chk_publication_availability_dates
        CHECK (ends_at > starts_at),
    CONSTRAINT chk_publication_availability_capacity
        CHECK (capacity >= 1 AND reserved >= 0 AND reserved <= capacity),
    CONSTRAINT uq_publication_availability_publication_id
    UNIQUE (publication_id, id),
INDEX idx_publication_availability_dates (publication_id, starts_at, ends_at)
);

CREATE TABLE publication_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publication_id VARCHAR(100) NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    actor_user_id VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    occurred_at DATETIME NOT NULL,
    CONSTRAINT fk_publication_history_publication
        FOREIGN KEY (publication_id) REFERENCES publications(id),
    INDEX idx_publication_history (publication_id, occurred_at)
);

CREATE TABLE bookings (
    id VARCHAR(100) PRIMARY KEY,
    publication_id VARCHAR(100) NOT NULL,
    buyer_user_id VARCHAR(100) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    objective VARCHAR(1000) NOT NULL,
    slot_id VARCHAR(100) NOT NULL,
    proposed_slot_id VARCHAR(100) NULL,
    buyer_accepted_reschedule BOOLEAN NOT NULL DEFAULT FALSE,
    provider_accepted_reschedule BOOLEAN NOT NULL DEFAULT FALSE,
    price INT NOT NULL,
    refund INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    paid_at DATETIME NULL,
    CONSTRAINT fk_bookings_publication
        FOREIGN KEY (publication_id) REFERENCES publications(id),
    CONSTRAINT fk_bookings_buyer
        FOREIGN KEY (buyer_user_id) REFERENCES profiles(user_id),
    CONSTRAINT fk_bookings_provider
        FOREIGN KEY (provider_user_id) REFERENCES profiles(user_id),
    CONSTRAINT fk_bookings_slot
    FOREIGN KEY (publication_id, slot_id)
    REFERENCES publication_availability(publication_id, id),
CONSTRAINT fk_bookings_proposed_slot
    FOREIGN KEY (publication_id, proposed_slot_id)
    REFERENCES publication_availability(publication_id, id),
    CONSTRAINT chk_booking_price
        CHECK (price >= 0),
    CONSTRAINT chk_booking_refund
        CHECK (refund >= 0),
    INDEX idx_bookings_publication (publication_id, status),
    INDEX idx_bookings_buyer (buyer_user_id, created_at),
    INDEX idx_bookings_provider (provider_user_id, created_at)
);

CREATE TABLE booking_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id VARCHAR(100) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    actor_user_id VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    occurred_at DATETIME NOT NULL,
    CONSTRAINT fk_booking_history_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_booking_history (booking_id, occurred_at)
);

CREATE TABLE wallets (
    user_id VARCHAR(100) PRIMARY KEY,
    balance INT NOT NULL,
    CONSTRAINT fk_wallets_profile
        FOREIGN KEY (user_id) REFERENCES profiles(user_id),
    CONSTRAINT chk_wallet_balance
        CHECK (balance >= 0)
);

CREATE TABLE wallet_ledger (
    id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    concept VARCHAR(250) NOT NULL,
    amount INT NOT NULL,
    resulting_balance INT NOT NULL,
    occurred_at DATETIME NOT NULL,
    CONSTRAINT fk_wallet_ledger_wallet
        FOREIGN KEY (user_id) REFERENCES wallets(user_id),
    CONSTRAINT chk_wallet_ledger_amount
        CHECK (amount <> 0),
    CONSTRAINT chk_wallet_ledger_balance
        CHECK (resulting_balance >= 0),
    INDEX idx_wallet_ledger_history (user_id, occurred_at, id)
);

CREATE TABLE ratings (
    id VARCHAR(100) PRIMARY KEY,
    booking_id VARCHAR(100) NOT NULL,
    author_user_id VARCHAR(100) NOT NULL,
    subject_user_id VARCHAR(100) NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_ratings_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_ratings_author
        FOREIGN KEY (author_user_id) REFERENCES profiles(user_id),
    CONSTRAINT fk_ratings_subject
        FOREIGN KEY (subject_user_id) REFERENCES profiles(user_id),
    CONSTRAINT uq_rating_booking_author
        UNIQUE (booking_id, author_user_id),
    CONSTRAINT chk_rating_score
        CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT chk_rating_different_users
        CHECK (author_user_id <> subject_user_id),
    INDEX idx_ratings_subject (subject_user_id, created_at)
);

CREATE TABLE notifications (
    id VARCHAR(100) PRIMARY KEY,
    recipient_user_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    detail VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    type VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES profiles(user_id),
    CONSTRAINT chk_notification_channel
        CHECK (channel IN ('EMAIL', 'PUSH', 'IN_APP')),
    INDEX idx_notifications_inbox (recipient_user_id, is_read, created_at)
);