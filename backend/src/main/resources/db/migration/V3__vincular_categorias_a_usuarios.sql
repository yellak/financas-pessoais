TRUNCATE TABLE categorias;

ALTER TABLE categorias
    ADD COLUMN usuario_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_categorias_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;

DROP INDEX uk_categorias_nome;
CREATE UNIQUE INDEX uk_categorias_usuario_nome ON categorias (usuario_id, LOWER(nome));
