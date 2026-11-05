-- Mantiene los filtros de texto y habilidad iguales en MySQL y en el repositorio en memoria.
ALTER TABLE publications
    MODIFY title VARCHAR(200)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY description TEXT
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY category VARCHAR(150)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

ALTER TABLE publication_tags
    MODIFY tag VARCHAR(150)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
