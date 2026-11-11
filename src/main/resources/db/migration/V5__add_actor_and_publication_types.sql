-- Organizaciones y desafíos (#38). Los perfiles existentes son profesionales y sus publicaciones,
-- mentorías; el valor por defecto solo completa esas filas y luego se elimina.
ALTER TABLE profiles
    ADD COLUMN actor_type VARCHAR(20) NOT NULL DEFAULT 'PROFESSIONAL' AFTER user_id,
    ADD CONSTRAINT chk_profile_actor_type CHECK (actor_type IN ('PROFESSIONAL', 'ORGANIZATION'));

ALTER TABLE profiles
    ALTER COLUMN actor_type DROP DEFAULT;

ALTER TABLE publications
    ADD COLUMN publication_type VARCHAR(20) NOT NULL DEFAULT 'MENTORSHIP' AFTER author_user_id,
    ADD CONSTRAINT chk_publication_type CHECK (publication_type IN ('MENTORSHIP', 'SERVICE', 'CHALLENGE'));

ALTER TABLE publications
    ALTER COLUMN publication_type DROP DEFAULT;
