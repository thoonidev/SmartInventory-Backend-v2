-- SmartInventory - Script unico MySQL
-- Escenario: distribuidora pyme de productos de limpieza.
-- Este archivo reinicia la base completa. Ejecutar en MySQL cuando se quiera cargar datos limpios.
--
-- Credenciales iniciales:
-- Administrador: valeria.rivas@limpialux.pe / Admin123*
-- Supervisor: marco.salazar@limpialux.pe / Supervisor123*
-- Consulta: andrea.paredes@limpialux.pe / Consulta123*
--
-- La empresa inicia en plan FREE para simular un SaaS real. Los reportes exportables
-- y el centro operativo se habilitan al cambiar de plan desde la pantalla "Mi plan".

DROP DATABASE IF EXISTS inventory_db;
CREATE DATABASE inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE inventory_db;

SET NAMES utf8mb4;
SET time_zone = '-05:00';

CREATE TABLE organizations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  ruc VARCHAR(20) NOT NULL UNIQUE,
  plan VARCHAR(30) NOT NULL DEFAULT 'FREE',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  email VARCHAR(180) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone_number VARCHAR(30) NOT NULL,
  role VARCHAR(30) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  last_login DATETIME,
  organization_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  organization_id BIGINT,
  CONSTRAINT fk_categories_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE suppliers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  contact_info VARCHAR(180) NOT NULL,
  address VARCHAR(250),
  lead_time_days INT,
  supplier_status VARCHAR(40),
  organization_id BIGINT,
  CONSTRAINT fk_suppliers_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE customers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  document_number VARCHAR(40),
  contact_info VARCHAR(180) NOT NULL,
  address VARCHAR(250),
  customer_status VARCHAR(40),
  organization_id BIGINT,
  CONSTRAINT fk_customers_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  sku VARCHAR(80) NOT NULL,
  price DECIMAL(12,2) NOT NULL DEFAULT 0,
  stock_quantity INT NOT NULL DEFAULT 0,
  description TEXT,
  expiry_date DATETIME,
  image_url VARCHAR(500),
  image_public_id VARCHAR(255),
  image_content_type VARCHAR(80),
  image_size BIGINT,
  abc_class VARCHAR(5),
  reorder_point INT,
  reorder_quantity INT,
  warehouse_location VARCHAR(120),
  inventory_value DECIMAL(14,2),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  category_id BIGINT,
  organization_id BIGINT,
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT fk_products_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT uk_products_sku_org UNIQUE (sku, organization_id)
);

CREATE TABLE transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  total_products INT,
  total_price DECIMAL(14,2),
  transaction_type VARCHAR(40),
  status VARCHAR(40),
  description TEXT,
  note TEXT,
  return_reason VARCHAR(120),
  return_condition VARCHAR(40),
  warehouse_location VARCHAR(120),
  stock_applied BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_at DATETIME,
  product_id BIGINT,
  user_id BIGINT,
  supplier_id BIGINT,
  customer_id BIGINT,
  organization_id BIGINT,
  CONSTRAINT fk_transactions_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_transactions_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  CONSTRAINT fk_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_transactions_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE transaction_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quantity INT NOT NULL,
  unit_price DECIMAL(14,2) NOT NULL DEFAULT 0,
  subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
  transaction_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  CONSTRAINT fk_transaction_items_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
  CONSTRAINT fk_transaction_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  action VARCHAR(80),
  entity VARCHAR(80),
  entity_id BIGINT,
  detail TEXT,
  ip_address VARCHAR(80),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  organization_id BIGINT,
  user_id BIGINT,
  CONSTRAINT fk_audit_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(80),
  title VARCHAR(180),
  message TEXT,
  severity VARCHAR(30),
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  organization_id BIGINT,
  user_id BIGINT,
  CONSTRAINT fk_notifications_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE warehouses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  code VARCHAR(40) NOT NULL,
  type VARCHAR(40) NOT NULL,
  address VARCHAR(250),
  manager_name VARCHAR(150),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  organization_id BIGINT,
  CONSTRAINT fk_warehouses_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT uk_warehouses_code_org UNIQUE (code, organization_id)
);

CREATE TABLE quality_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(60) NOT NULL,
  process_area VARCHAR(150),
  reference_document VARCHAR(150),
  finding_type VARCHAR(60),
  severity VARCHAR(40),
  status VARCHAR(40),
  description TEXT,
  corrective_action TEXT,
  responsible_name VARCHAR(150),
  source_type VARCHAR(80),
  source_id BIGINT,
  product_name VARCHAR(180),
  due_date DATE,
  closed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  organization_id BIGINT,
  user_id BIGINT,
  CONSTRAINT fk_quality_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_quality_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uk_quality_code_org UNIQUE (code, organization_id)
);

CREATE TABLE production_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(60) NOT NULL,
  product_name VARCHAR(180) NOT NULL,
  planned_quantity INT NOT NULL DEFAULT 0,
  completed_quantity INT NOT NULL DEFAULT 0,
  stock_applied_quantity INT NOT NULL DEFAULT 0,
  status VARCHAR(40),
  start_date DATE,
  due_date DATE,
  notes TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  product_id BIGINT,
  organization_id BIGINT,
  user_id BIGINT,
  CONSTRAINT fk_production_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_production_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
  CONSTRAINT fk_production_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uk_production_code_org UNIQUE (code, organization_id)
);

CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_categories_organization ON categories(organization_id);
CREATE INDEX idx_suppliers_organization ON suppliers(organization_id);
CREATE INDEX idx_customers_organization ON customers(organization_id);
CREATE INDEX idx_products_organization ON products(organization_id);
CREATE INDEX idx_products_stock ON products(organization_id, stock_quantity, reorder_point);
CREATE INDEX idx_transactions_organization ON transactions(organization_id);
CREATE INDEX idx_transactions_created_at ON transactions(organization_id, created_at);
CREATE INDEX idx_transactions_status ON transactions(organization_id, status, transaction_type);
CREATE INDEX idx_transaction_items_transaction ON transaction_items(transaction_id);
CREATE INDEX idx_audit_logs_organization ON audit_logs(organization_id, created_at);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at);
CREATE INDEX idx_warehouses_organization ON warehouses(organization_id, active);
CREATE INDEX idx_quality_organization ON quality_records(organization_id, status, due_date);
CREATE INDEX idx_production_organization ON production_orders(organization_id, status, due_date);

INSERT INTO organizations (name, ruc, plan, is_active, created_at)
VALUES ('LimpiaLux Distribuciones SAC', '20604577891', 'FREE', TRUE, DATE_SUB(NOW(), INTERVAL 45 DAY));

SET @org_id := LAST_INSERT_ID();

INSERT INTO users (name, email, password, phone_number, role, is_active, last_login, organization_id, created_at)
VALUES
  ('Valeria Rivas', 'valeria.rivas@limpialux.pe', '$2a$10$.vGAUD3rdttekSckCU1OVO6B8FNjp7bev8R6sfzLa7.XUJLqGBlJe', '987654321', 'ADMIN', TRUE, DATE_SUB(NOW(), INTERVAL 2 HOUR), @org_id, DATE_SUB(NOW(), INTERVAL 45 DAY)),
  ('Marco Salazar', 'marco.salazar@limpialux.pe', '$2a$10$CIe49anz0ZvBY02O6CJaGedHdD//E725RwZxbALz3jj.LWzipiFvy', '976321458', 'MANAGER', TRUE, DATE_SUB(NOW(), INTERVAL 6 HOUR), @org_id, DATE_SUB(NOW(), INTERVAL 42 DAY)),
  ('Andrea Paredes', 'andrea.paredes@limpialux.pe', '$2a$10$SGOXzPHRWERFPNAzgv17YeWOeo3WBJhUTX7Y0jvfuG.gpDLXeMAbC', '965147832', 'VIEWER', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), @org_id, DATE_SUB(NOW(), INTERVAL 40 DAY));

SET @admin_id := (SELECT id FROM users WHERE email = 'valeria.rivas@limpialux.pe' LIMIT 1);
SET @supervisor_id := (SELECT id FROM users WHERE email = 'marco.salazar@limpialux.pe' LIMIT 1);
SET @consulta_id := (SELECT id FROM users WHERE email = 'andrea.paredes@limpialux.pe' LIMIT 1);

INSERT INTO categories (name, organization_id)
VALUES
  ('Detergentes y jabones', @org_id),
  ('Desinfeccion', @org_id),
  ('Papel e higiene', @org_id),
  ('Accesorios de limpieza', @org_id),
  ('Bolsas y descartables', @org_id);

INSERT INTO suppliers (name, contact_info, address, lead_time_days, supplier_status, organization_id)
VALUES
  ('Quimica Andina SAC', 'ventas@quimicaandina.pe / 01-612-4800', 'Av. Argentina 3280, Callao', 3, 'ACTIVO', @org_id),
  ('Higiene Pro Peru SRL', 'pedidos@higienepro.pe / 01-445-2190', 'Jr. Los Almacenes 455, Ate, Lima', 4, 'ACTIVO', @org_id),
  ('Papelera Pacifico SAC', 'corporativo@papelerapacifico.pe / 01-611-9030', 'Av. Nestor Gambetta 910, Callao', 5, 'ACTIVO', @org_id),
  ('Importadora CleanTools SAC', 'compras@cleantools.pe / 01-700-1818', 'Av. Los Frutales 1280, La Molina', 7, 'EN_REVISION', @org_id);

INSERT INTO customers (name, document_number, contact_info, address, customer_status, organization_id)
VALUES
  ('Hotel Costa Verde SAC', '20511890421', 'logistica@hotelcostaverde.pe / 01-241-5600', 'Av. Malecon 230, Miraflores', 'ACTIVO', @org_id),
  ('Clinica San Gabriel SAC', '20147382914', 'almacen@clinicasangabriel.pe / 01-614-7000', 'Av. La Marina 2955, San Miguel', 'ACTIVO', @org_id),
  ('Colegio Santa Maria', '20109988771', 'administracion@santamaria.edu.pe / 01-436-2250', 'Jr. Los Cedros 144, Surco', 'ACTIVO', @org_id),
  ('Restaurante La Plaza SAC', '20601023561', 'compras@laplaza.pe / 01-421-0880', 'Calle Bolognesi 430, Barranco', 'ACTIVO', @org_id),
  ('Condominio Los Cedros', '10445822319', 'administracion@loscedros.pe / 01-367-9090', 'Av. El Derby 640, Surco', 'ACTIVO', @org_id);

INSERT INTO products (
  name, sku, price, stock_quantity, description, expiry_date, image_url, abc_class,
  reorder_point, reorder_quantity, warehouse_location, inventory_value, category_id, organization_id
)
VALUES
  ('Detergente industrial 5L', 'LIM-DETER-5L', 38.90, 36, 'Detergente liquido concentrado para pisos y superficies lavables.', DATE_ADD(NOW(), INTERVAL 540 DAY), NULL, 'A', 18, 40, 'ALM-LIM-01', 1400.40, (SELECT id FROM categories WHERE name = 'Detergentes y jabones' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Lejia concentrada 1L', 'LIM-LEJIA-1L', 4.80, 72, 'Lejia para desinfeccion de banos, cocinas y areas comunes.', DATE_ADD(NOW(), INTERVAL 360 DAY), NULL, 'A', 40, 120, 'ALM-LIM-01', 345.60, (SELECT id FROM categories WHERE name = 'Desinfeccion' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Desinfectante pino 4L', 'LIM-DESPINO-4L', 24.50, 22, 'Desinfectante aromatizado para alto transito.', DATE_ADD(NOW(), INTERVAL 420 DAY), NULL, 'A', 20, 50, 'DES-MIR-01', 539.00, (SELECT id FROM categories WHERE name = 'Desinfeccion' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Alcohol gel 1L', 'LIM-ALCGEL-1L', 18.90, 15, 'Alcohol gel al 70 por ciento para dispensadores.', DATE_ADD(NOW(), INTERVAL 300 DAY), NULL, 'B', 25, 60, 'CAL-CUA-01', 283.50, (SELECT id FROM categories WHERE name = 'Desinfeccion' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Papel toalla interfoliado 200 hojas', 'LIM-PTOALLA-200', 12.50, 44, 'Paquete de papel toalla interfoliado para banos institucionales.', NULL, NULL, 'B', 30, 80, 'ALM-LIM-01', 550.00, (SELECT id FROM categories WHERE name = 'Papel e higiene' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Papel higienico jumbo 250m', 'LIM-PHJ-250', 16.90, 28, 'Rollo jumbo para dispensador industrial.', NULL, NULL, 'B', 24, 60, 'ALM-LIM-01', 473.20, (SELECT id FROM categories WHERE name = 'Papel e higiene' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Guantes nitrilo caja 100', 'LIM-GUANTE-NIT', 32.00, 8, 'Caja de guantes de nitrilo azul talla M.', DATE_ADD(NOW(), INTERVAL 720 DAY), NULL, 'A', 15, 40, 'ALM-LIM-01', 256.00, (SELECT id FROM categories WHERE name = 'Accesorios de limpieza' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Trapeador microfibra industrial', 'LIM-TRAP-MICRO', 21.00, 19, 'Trapeador de microfibra para limpieza profesional.', NULL, NULL, 'C', 12, 30, 'DES-MIR-01', 399.00, (SELECT id FROM categories WHERE name = 'Accesorios de limpieza' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Escoba angular reforzada', 'LIM-ESCOBA-ANG', 14.50, 25, 'Escoba angular con cerdas firmes para interiores.', NULL, NULL, 'C', 15, 35, 'DES-MIR-01', 362.50, (SELECT id FROM categories WHERE name = 'Accesorios de limpieza' AND organization_id = @org_id LIMIT 1), @org_id),
  ('Bolsas negras 140L paquete 10', 'LIM-BOLSA-140', 10.90, 0, 'Bolsas negras resistentes para residuos generales.', NULL, NULL, 'A', 20, 70, 'ALM-LIM-01', 0.00, (SELECT id FROM categories WHERE name = 'Bolsas y descartables' AND organization_id = @org_id LIMIT 1), @org_id);

INSERT INTO warehouses (name, code, type, address, manager_name, active, created_at, organization_id)
VALUES
  ('Sede central Ate', 'SED-ATE', 'MAIN', 'Jr. Los Almacenes 455, Ate, Lima', 'Valeria Rivas', TRUE, DATE_SUB(NOW(), INTERVAL 45 DAY), @org_id),
  ('Almacen principal limpieza', 'ALM-LIM-01', 'WAREHOUSE', 'Nave 2 - Zona A, Jr. Los Almacenes 455, Ate', 'Marco Salazar', TRUE, DATE_SUB(NOW(), INTERVAL 44 DAY), @org_id),
  ('Zona de calidad y cuarentena', 'CAL-CUA-01', 'QUALITY', 'Nave 2 - Zona C, Jr. Los Almacenes 455, Ate', 'Andrea Paredes', TRUE, DATE_SUB(NOW(), INTERVAL 42 DAY), @org_id),
  ('Punto de despacho Miraflores', 'DES-MIR-01', 'STORE', 'Av. Benavides 1180, Miraflores', 'Marco Salazar', TRUE, DATE_SUB(NOW(), INTERVAL 35 DAY), @org_id);

INSERT INTO quality_records (
  code, process_area, reference_document, finding_type, severity, status, description,
  corrective_action, responsible_name, source_type, source_id, product_name, due_date, closed_at, created_at, organization_id, user_id
)
VALUES
  ('ISO-QA-2026-001', 'Recepcion de mercaderia', 'PR-CAL-01 Inspeccion de ingreso', 'INSPECTION', 'MEDIUM', 'IN_PROGRESS',
   'Tres galoneras de alcohol gel llegaron con tapas flojas y fueron separadas antes de ingresar a stock.',
   'Solicitar reposicion al proveedor y reforzar revision por lote antes de registrar la compra.',
   'Marco Salazar', 'INSPECTION', NULL, 'Alcohol gel 1L', DATE_ADD(CURDATE(), INTERVAL 3 DAY), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), @org_id, @supervisor_id),
  ('ISO-QA-2026-002', 'Despacho institucional', 'PR-DES-02 Control de guia', 'NON_CONFORMITY', 'HIGH', 'OPEN',
   'Cliente reporto etiqueta deteriorada en dos envases de desinfectante pino.',
   'Revisar empaque de salida y aplicar doble verificacion para productos liquidos.',
   'Valeria Rivas', 'RETURN_FROM_CUSTOMER', NULL, 'Desinfectante pino 4L', DATE_ADD(CURDATE(), INTERVAL 2 DAY), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), @org_id, @admin_id),
  ('ISO-QA-2026-003', 'Control documental', 'ISO-9001 Matriz de procesos', 'DOCUMENT_REVIEW', 'LOW', 'CLOSED',
   'Se actualizo el responsable del procedimiento de reposicion por punto minimo.',
   'Documento revisado y comunicado al equipo operativo.',
   'Andrea Paredes', 'DOCUMENT_REVIEW', NULL, 'Procedimiento de reposicion', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), @org_id, @consulta_id);

INSERT INTO production_orders (
  code, product_name, planned_quantity, completed_quantity, stock_applied_quantity, status, start_date, due_date,
  notes, created_at, product_id, organization_id, user_id
)
VALUES
  ('OP-2026-001', 'Detergente industrial 5L', 80, 48, 0, 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
   'Kit armado con detergente 5L, papel toalla y escoba angular para pedido recurrente de hoteleria.',
   DATE_SUB(NOW(), INTERVAL 2 DAY), (SELECT id FROM products WHERE sku = 'LIM-DETER-5L' AND organization_id = @org_id LIMIT 1), @org_id, @supervisor_id),
  ('OP-2026-002', 'Bolsas negras 140L paquete 10', 120, 0, 0, 'PLANNED', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
   'Preparacion pendiente hasta recibir compra OC-2026-009.',
   NOW(), (SELECT id FROM products WHERE sku = 'LIM-BOLSA-140' AND organization_id = @org_id LIMIT 1), @org_id, @supervisor_id),
  ('OP-2026-003', 'Desinfectante pino 4L', 50, 50, 50, 'DONE', DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),
   'Lote terminado y despachado a cartera corporativa.',
   DATE_SUB(NOW(), INTERVAL 8 DAY), (SELECT id FROM products WHERE sku = 'LIM-DESPINO-4L' AND organization_id = @org_id LIMIT 1), @org_id, @admin_id);

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  140, 2036.00, 'PURCHASE', 'COMPLETED', 'OC-2026-001 - Reposicion inicial de quimicos',
  'Ingreso recibido en almacen principal.', 'ALM-LIM-01', TRUE,
  DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-DETER-5L' AND organization_id = @org_id LIMIT 1),
  @admin_id,
  (SELECT id FROM suppliers WHERE name = 'Quimica Andina SAC' AND organization_id = @org_id LIMIT 1),
  NULL, @org_id
);
SET @trx_purchase_1 := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (40, 38.90, 1556.00, @trx_purchase_1, (SELECT id FROM products WHERE sku = 'LIM-DETER-5L' AND organization_id = @org_id LIMIT 1)),
  (100, 4.80, 480.00, @trx_purchase_1, (SELECT id FROM products WHERE sku = 'LIM-LEJIA-1L' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  60, 882.00, 'PURCHASE', 'COMPLETED', 'OC-2026-002 - Papel e higiene para cartera institucional',
  'Compra entregada por proveedor regular.', 'ALM-LIM-01', TRUE,
  DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-PTOALLA-200' AND organization_id = @org_id LIMIT 1),
  @supervisor_id,
  (SELECT id FROM suppliers WHERE name = 'Papelera Pacifico SAC' AND organization_id = @org_id LIMIT 1),
  NULL, @org_id
);
SET @trx_purchase_2 := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (30, 12.50, 375.00, @trx_purchase_2, (SELECT id FROM products WHERE sku = 'LIM-PTOALLA-200' AND organization_id = @org_id LIMIT 1)),
  (30, 16.90, 507.00, @trx_purchase_2, (SELECT id FROM products WHERE sku = 'LIM-PHJ-250' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  42, 700.20, 'SALE', 'COMPLETED', 'VT-2026-018 - Abastecimiento mensual Hotel Costa Verde',
  'Entrega programada a area de housekeeping.', 'DES-MIR-01', TRUE,
  DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-DETER-5L' AND organization_id = @org_id LIMIT 1),
  @supervisor_id, NULL,
  (SELECT id FROM customers WHERE name = 'Hotel Costa Verde SAC' AND organization_id = @org_id LIMIT 1),
  @org_id
);
SET @trx_sale_1 := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (10, 38.90, 389.00, @trx_sale_1, (SELECT id FROM products WHERE sku = 'LIM-DETER-5L' AND organization_id = @org_id LIMIT 1)),
  (24, 4.80, 115.20, @trx_sale_1, (SELECT id FROM products WHERE sku = 'LIM-LEJIA-1L' AND organization_id = @org_id LIMIT 1)),
  (8, 24.50, 196.00, @trx_sale_1, (SELECT id FROM products WHERE sku = 'LIM-DESPINO-4L' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  15, 349.00, 'SALE', 'COMPLETED', 'VT-2026-021 - Insumos sanitarios Clinica San Gabriel',
  'Pedido despachado con guia de remision.', 'DES-MIR-01', TRUE,
  DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-ALCGEL-1L' AND organization_id = @org_id LIMIT 1),
  @supervisor_id, NULL,
  (SELECT id FROM customers WHERE name = 'Clinica San Gabriel SAC' AND organization_id = @org_id LIMIT 1),
  @org_id
);
SET @trx_sale_2 := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (10, 18.90, 189.00, @trx_sale_2, (SELECT id FROM products WHERE sku = 'LIM-ALCGEL-1L' AND organization_id = @org_id LIMIT 1)),
  (5, 32.00, 160.00, @trx_sale_2, (SELECT id FROM products WHERE sku = 'LIM-GUANTE-NIT' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  50, 967.00, 'PURCHASE', 'PENDING', 'OC-2026-009 - Reposicion urgente de bolsas y guantes',
  'Pendiente de aprobacion por control de compras.', 'ALM-LIM-01', FALSE,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-BOLSA-140' AND organization_id = @org_id LIMIT 1),
  @supervisor_id,
  (SELECT id FROM suppliers WHERE name = 'Importadora CleanTools SAC' AND organization_id = @org_id LIMIT 1),
  NULL, @org_id
);
SET @trx_purchase_pending := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (30, 10.90, 327.00, @trx_purchase_pending, (SELECT id FROM products WHERE sku = 'LIM-BOLSA-140' AND organization_id = @org_id LIMIT 1)),
  (20, 32.00, 640.00, @trx_purchase_pending, (SELECT id FROM products WHERE sku = 'LIM-GUANTE-NIT' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, return_reason, return_condition, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  3, 56.70, 'RETURN_TO_SUPPLIER', 'COMPLETED', 'DV-2026-004 - Devolucion por envases observados',
  'Lote separado en cuarentena antes de recojo.', 'Envases con tapa defectuosa', 'OBSERVADO', 'CAL-CUA-01', TRUE,
  DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-ALCGEL-1L' AND organization_id = @org_id LIMIT 1),
  @admin_id,
  (SELECT id FROM suppliers WHERE name = 'Quimica Andina SAC' AND organization_id = @org_id LIMIT 1),
  NULL, @org_id
);
SET @trx_return_supplier := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (3, 18.90, 56.70, @trx_return_supplier, (SELECT id FROM products WHERE sku = 'LIM-ALCGEL-1L' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, return_reason, return_condition, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  2, 49.00, 'RETURN_FROM_CUSTOMER', 'PENDING', 'DC-2026-003 - Revision de devolucion Hotel Costa Verde',
  'Cliente reporta dos galoneras con etiqueta deteriorada.', 'Etiqueta deteriorada en despacho', 'EN_REVISION', 'CAL-CUA-01', FALSE,
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-DESPINO-4L' AND organization_id = @org_id LIMIT 1),
  @supervisor_id, NULL,
  (SELECT id FROM customers WHERE name = 'Hotel Costa Verde SAC' AND organization_id = @org_id LIMIT 1),
  @org_id
);
SET @trx_return_customer := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (2, 24.50, 49.00, @trx_return_customer, (SELECT id FROM products WHERE sku = 'LIM-DESPINO-4L' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  12, 130.80, 'SALE', 'PROCESSING', 'VT-2026-026 - Reserva Condominio Los Cedros',
  'Pendiente por falta de stock de bolsas negras.', 'DES-MIR-01', FALSE,
  NOW(), NOW(),
  (SELECT id FROM products WHERE sku = 'LIM-BOLSA-140' AND organization_id = @org_id LIMIT 1),
  @supervisor_id, NULL,
  (SELECT id FROM customers WHERE name = 'Condominio Los Cedros' AND organization_id = @org_id LIMIT 1),
  @org_id
);
SET @trx_sale_processing := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (12, 10.90, 130.80, @trx_sale_processing, (SELECT id FROM products WHERE sku = 'LIM-BOLSA-140' AND organization_id = @org_id LIMIT 1));

INSERT INTO transactions (
  total_products, total_price, transaction_type, status, description, note, warehouse_location, stock_applied,
  created_at, update_at, product_id, user_id, supplier_id, customer_id, organization_id
)
VALUES (
  50, 1225.00, 'PRODUCTION_OUTPUT', 'COMPLETED', 'Entrada por produccion OP-2026-003',
  'Lote promocional desinfeccion oficinas terminado y disponible para despacho.', 'DES-MIR-01', TRUE,
  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
  (SELECT id FROM products WHERE sku = 'LIM-DESPINO-4L' AND organization_id = @org_id LIMIT 1),
  @admin_id, NULL, NULL, @org_id
);
SET @trx_production_output := LAST_INSERT_ID();

INSERT INTO transaction_items (quantity, unit_price, subtotal, transaction_id, product_id)
VALUES
  (50, 24.50, 1225.00, @trx_production_output, (SELECT id FROM products WHERE sku = 'LIM-DESPINO-4L' AND organization_id = @org_id LIMIT 1));

INSERT INTO audit_logs (action, entity, entity_id, detail, ip_address, created_at, organization_id, user_id)
VALUES
  ('CARGA_INICIAL', 'Base de datos', @org_id, 'Se cargo el entorno inicial de LimpiaLux con catalogo, usuarios y movimientos operativos.', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 21 DAY), @org_id, @admin_id),
  ('INICIO_SESION', 'Usuario', @admin_id, 'Valeria Rivas ingreso al sistema para revisar el estado general del inventario.', '192.168.1.21', DATE_SUB(NOW(), INTERVAL 2 HOUR), @org_id, @admin_id),
  ('CREACION_PRODUCTO', 'Producto', (SELECT id FROM products WHERE sku = 'LIM-BOLSA-140' AND organization_id = @org_id LIMIT 1), 'Se registro Bolsas negras 140L paquete 10 con punto de reposicion de 20 unidades.', '192.168.1.21', DATE_SUB(NOW(), INTERVAL 19 DAY), @org_id, @admin_id),
  ('COMPRA_REGISTRADA', 'Transaccion', @trx_purchase_1, 'Compra recibida desde Quimica Andina SAC por S/ 2036.00.', '192.168.1.22', DATE_SUB(NOW(), INTERVAL 21 DAY), @org_id, @admin_id),
  ('VENTA_REGISTRADA', 'Transaccion', @trx_sale_1, 'Venta atendida para Hotel Costa Verde SAC por S/ 700.20.', '192.168.1.34', DATE_SUB(NOW(), INTERVAL 9 DAY), @org_id, @supervisor_id),
  ('ALERTA_STOCK', 'Producto', (SELECT id FROM products WHERE sku = 'LIM-GUANTE-NIT' AND organization_id = @org_id LIMIT 1), 'Guantes nitrilo caja 100 quedo por debajo del punto de reposicion.', '192.168.1.34', DATE_SUB(NOW(), INTERVAL 6 DAY), @org_id, @supervisor_id),
  ('DEVOLUCION_PROVEEDOR', 'Transaccion', @trx_return_supplier, 'Se registro devolucion a proveedor por envases observados.', '192.168.1.21', DATE_SUB(NOW(), INTERVAL 4 DAY), @org_id, @admin_id),
  ('APROBACION_PENDIENTE', 'Transaccion', @trx_purchase_pending, 'Orden de compra pendiente de aprobacion por reposicion urgente.', '192.168.1.34', DATE_SUB(NOW(), INTERVAL 2 DAY), @org_id, @supervisor_id),
  ('REVISION_DEVOLUCION', 'Transaccion', @trx_return_customer, 'Devolucion de cliente pendiente de inspeccion de calidad.', '192.168.1.34', DATE_SUB(NOW(), INTERVAL 1 DAY), @org_id, @supervisor_id),
  ('CONSULTA_REPORTE', 'Reporte', @org_id, 'Andrea Paredes reviso indicadores de inventario y movimientos.', '192.168.1.40', DATE_SUB(NOW(), INTERVAL 8 HOUR), @org_id, @consulta_id),
  ('Registro creado', 'Almacen o sede', (SELECT id FROM warehouses WHERE code = 'ALM-LIM-01' AND organization_id = @org_id LIMIT 1), 'Se configuro el almacen principal de limpieza para controlar ubicaciones fisicas.', '192.168.1.21', DATE_SUB(NOW(), INTERVAL 44 DAY), @org_id, @admin_id),
  ('Registro creado', 'Calidad ISO 9001', (SELECT id FROM quality_records WHERE code = 'ISO-QA-2026-001' AND organization_id = @org_id LIMIT 1), 'Se registro inspeccion de ingreso por envases observados.', '192.168.1.34', DATE_SUB(NOW(), INTERVAL 4 DAY), @org_id, @supervisor_id),
  ('Registro creado', 'Orden de produccion', (SELECT id FROM production_orders WHERE code = 'OP-2026-001' AND organization_id = @org_id LIMIT 1), 'Se planifico armado de kit limpieza institucional hotelero.', '192.168.1.34', DATE_SUB(NOW(), INTERVAL 2 DAY), @org_id, @supervisor_id);

INSERT INTO notifications (type, title, message, severity, is_read, created_at, organization_id, user_id)
VALUES
  ('STOCK_ALERT', 'Stock agotado', 'Bolsas negras 140L paquete 10 no tiene unidades disponibles. Revisar orden pendiente.', 'DANGER', FALSE, NOW(), @org_id, @admin_id),
  ('STOCK_ALERT', 'Stock bajo', 'Guantes nitrilo caja 100 esta por debajo del punto de reposicion.', 'WARNING', FALSE, DATE_SUB(NOW(), INTERVAL 3 HOUR), @org_id, @admin_id),
  ('STOCK_ALERT', 'Stock bajo', 'Alcohol gel 1L requiere reposicion antes del siguiente despacho institucional.', 'WARNING', FALSE, DATE_SUB(NOW(), INTERVAL 5 HOUR), @org_id, @supervisor_id),
  ('APPROVAL_PENDING', 'Compra pendiente', 'OC-2026-009 espera aprobacion para reponer bolsas y guantes.', 'INFO', FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), @org_id, @admin_id),
  ('RETURN_REVIEW', 'Devolucion por revisar', 'DC-2026-003 necesita inspeccion antes de reincorporar stock.', 'INFO', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), @org_id, @supervisor_id);
