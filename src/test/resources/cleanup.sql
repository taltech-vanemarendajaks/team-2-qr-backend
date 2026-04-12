-- Cleans up all user-created test data between tests, in FK order.
-- Roles are not touched — they are seeded once at container startup and must persist.
DELETE FROM mystuff.image;
DELETE FROM mystuff.item;
DELETE FROM mystuff."user";
