# SmartInventory — Backend v2

API REST para la gestión de inventarios y operaciones de SmartInventory. Administra organizaciones, productos y movimientos de stock, con autenticación y acceso por roles.

## Funcionalidades

- Gestión de productos, categorías, proveedores, clientes y almacenes.
- Compras, ventas, devoluciones y seguimiento de transacciones.
- Indicadores logísticos, alertas de stock y sugerencias de reposición.
- Control de calidad, órdenes de producción y reportes en PDF y Excel.
- Organizaciones con planes y límites de uso, auditoría y notificaciones.
- Autenticación JWT, eventos por WebSocket y almacenamiento de imágenes local o en Cloudinary.

## Tecnologías

Java 21 · Spring Boot 3.5 · Spring Security · Spring Data JPA · MySQL · Maven · JWT · WebSocket · Cloudinary · OpenPDF

## Instalación y configuración

Requiere **JDK 21** y **MySQL**. El repositorio incluye Maven Wrapper.

```bash
git clone https://github.com/thoonidev/SmartInventory-Backend-v2.git
cd SmartInventory-Backend-v2
```

Crea la base de datos en MySQL:

```sql
CREATE DATABASE inventory_db CHARACTER SET utf8mb4;
```

Copia `.env.example` como `.env.local` en la raíz del proyecto. Ajusta estas variables y conserva las demás del ejemplo:

```properties
SERVER_PORT=5050
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/inventory_db
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_password
JWT_SECRET=reemplaza_por_una_clave_aleatoria_de_al_menos_32_bytes
SMARTINVENTORY_STORAGE_PROVIDER=local
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

Spring Boot carga `.env.local` automáticamente. Hibernate crea o actualiza las tablas al iniciar. Para guardar imágenes en Cloudinary, cambia el proveedor a `cloudinary` y configura `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET`. No publiques credenciales reales.

## Ejecución

En Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

La API estará disponible en `http://localhost:5050/api` y el WebSocket en `/ws/live`.

Registra una organización mediante `POST /api/organizations/register` e inicia sesión con `POST /api/auth/login`. Envía el token recibido en las solicitudes protegidas:

```http
Authorization: Bearer <token>
```

## Comandos

| Comando (Windows) | Descripción |
| --- | --- |
| `.\mvnw.cmd test` | Ejecuta las pruebas. |
| `.\mvnw.cmd clean package` | Compila, ejecuta las pruebas y genera el JAR en `target/`. |

En Linux o macOS, sustituye `.\mvnw.cmd` por `./mvnw`. Las pruebas que cargan el contexto de Spring pueden requerir MySQL y las variables de configuración.

## Frontend

Disponible en [SmartInventory-Frontend-v2](https://github.com/thoonidev/SmartInventory-Frontend-v2). Configura allí `VITE_API_URL=http://localhost:5050/api`.
