-- Kustutab äre schema 'bank' (mis põhimõtteliselt kustutab ära kõik tabelid tolles schemas)
DROP SCHEMA IF EXISTS mystuff CASCADE;

-- Loob uue bank schema
CREATE SCHEMA mystuff
-- ja taastab vajalikud andmebaasi õigused
    GRANT ALL ON SCHEMA mystuff TO postgres;

GRANT ALL ON SCHEMA mystuff TO PUBLIC;