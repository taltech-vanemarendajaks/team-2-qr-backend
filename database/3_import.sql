INSERT INTO mystuff.role (id, name) VALUES (default, 'admin');
INSERT INTO mystuff.role (id, name) VALUES (default, 'customer');


INSERT INTO mystuff."user" (id, role_id, username, password, email, status) VALUES (default, 1,'admin', '123','admin@example.com', 'A');
INSERT INTO mystuff."user" (id, role_id, username, password, email, status) VALUES (default, 2,'hanna', '321','hanna@example.com', 'A');
INSERT INTO mystuff."user" (id, role_id, username, password, email, status) VALUES (default, 2,'katha', '213','katha@example.com', 'A');

INSERT INTO mystuff."item" (id,user_id, name, date, model, comment, status) VALUES (default, 2,'telekas', '2025-11-06','L6 sd123','super', 'A');
INSERT INTO mystuff."item" (id,user_id, name, date, model, comment, status) VALUES (default, 3,'pesumasin', '2024-11-20','bosh 123','kehva', 'A');
INSERT INTO mystuff."item" (id,user_id, name, date, model, comment, status) VALUES (default, 2,'telefon', '2025-07-22','iphone 17','mega', 'A');

