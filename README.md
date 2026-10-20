# Reto Técnico Back End - Sistema de Pedidos e Inventario

Solución con arquitectura de microservicios: **Java 17**, **Spring Boot 3.2**, **Spring Cloud Gateway**, **Spring Data JPA**, **PostgreSQL**, **Keycloak (OAuth 2.0 / OpenID Connect)** y **Docker Compose**.

## Contenido

1. [Arquitectura](#arquitectura)
2. [Cómo ejecutar](#cómo-ejecutar)
3. [Autenticación](#autenticación)
4. [API](#api)
5. [Modelo de errores](#modelo-de-errores)
6. [Estados del pedido y flujo de creación](#estados-del-pedido-y-flujo-de-creación)
7. [Probar con Postman y Swagger](#probar-con-postman-y-swagger)
8. [Decisiones de diseño](#decisiones-de-diseño)
9. [Trazabilidad y logging](#trazabilidad-y-logging)
10. [Pruebas](#pruebas)
11. [Estructura del proyecto](#estructura-del-proyecto)
12. [Limitaciones conocidas](#limitaciones-conocidas)
13. [Solución de problemas](#solución-de-problemas)

## Arquitectura

```
                      +-----------+
   Cliente ---------> |  Gateway  | ---- valida JWT, X-Trace-Id, enruta
 (Postman/Swagger)    |   :8080   |
                      +-----+-----+
              +-------------+--------------+
              v                            v
      +---------------+  WebClient  +------------------+
      | Order Service | ----------> | Inventory Service|
      |  (interno)    |             |    (interno)     |
      +-------+-------+             +--------+---------+
              v                              v
         [orders-db]                   [inventory-db]        [keycloak :8180]
        PostgreSQL (interna)          PostgreSQL (interna)    emite los JWT
```

**Solo el Gateway (8080) y Keycloak (8180) se publican en el host.** Order, Inventory y las bases de datos viven únicamente en la red interna de Docker (`backend-net`) y no son accesibles desde fuera.

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `api-gateway` | 8080 | Punto de entrada: enrutamiento, validación del JWT, `X-Trace-Id`, Swagger UI agregado |
| `order-service` | 8081 (solo red interna) | Pedidos: creación, consulta, cancelación, historial. Consulta a Inventory vía WebClient |
| `inventory-service` | 8082 (solo red interna) | Productos, stock y disponibilidad |
| `keycloak` | 8180 | Servidor de autenticación (realm `reto` importado automáticamente) |
| `orders-db` | 5432 (solo red interna) | PostgreSQL **exclusivo** de Order Service (`orders_db`) |
| `inventory-db` | 5432 (solo red interna) | PostgreSQL **exclusivo** de Inventory Service (`inventory_db`) |

**Persistencia independiente:** cada microservicio tiene su propia base de datos, su propio usuario y su propio volumen. Ningún servicio accede a las tablas del otro; solo se comunican por HTTP.

## Cómo ejecutar

Requisitos: Docker Desktop (o Docker Engine + Compose v2).

1. Clonar o descargar este repositorio.
2. Asegurarse de tener Docker Desktop iniciado.
3. Ejecutar el siguiente comando en la raíz del proyecto:

```bash
docker compose up --build
```

El primer arranque tarda 1–2 minutos (descarga de imágenes, build de los servicios y arranque de Keycloak). Los servicios arrancan en orden gracias a los healthchecks: bases de datos → Inventory → Order → Gateway.

Verificar que todo esté arriba:

```bash
docker compose ps          # todos "healthy" / "running"
curl http://localhost:8080/actuator/health
```

Datos de prueba que carga Inventory al iniciar:

| Código | Producto | Stock | Precio |
|---|---|---|---|
| `PROD-001` | Laptop Gamer | 10 | 1200.00 |
| `PROD-002` | Teclado Mecánico | 0 | 80.00 |

Detener y limpiar (incluye las bases de datos):

```bash
docker compose down -v
```

Para desarrollo local (depurar un servicio desde el IDE o conectarse a las bases) existe un override opcional que publica los puertos internos; **no es necesario para probar la solución**:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

Con ese override los valores por defecto de `application.yml` aplican: `localhost:5433` (orders), `localhost:5434` (inventory) y Keycloak en `localhost:8180`. Para ejecutar un servicio sin Docker: `mvn spring-boot:run` dentro de su carpeta.

## Autenticación (Keycloak)

El Gateway valida tokens JWT emitidos por Keycloak (y cada microservicio los vuelve a validar como OAuth 2.0 Resource Server).

| Dato | Valor |
|---|---|
| Realm | `reto` |
| Client ID (público) | `reto-client` |
| Usuario de prueba | `nayeli` |
| Contraseña | `nayeli123` |
| Consola de Keycloak | http://localhost:8180 (`admin` / `admin`) |

Obtener un token:

```bash
curl -s -X POST http://localhost:8180/realms/reto/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=reto-client" \
  -d "username=nayeli" -d "password=nayeli123"
```

Usar el `access_token` en cada llamada: `Authorization: Bearer <token>`. Solo son públicos `/actuator/health` y la documentación OpenAPI/Swagger.

## API

Todas las llamadas pasan por el Gateway (`http://localhost:8080`) y requieren token.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Registrar un pedido | 201, 400, 401, 503 |
| `GET` | `/api/v1/orders/{id}` | Consultar un pedido | 200, 401, 404 |
| `GET` | `/api/v1/orders/{id}/history` | Historial de estados (orden cronológico) | 200, 401, 404 |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancelar un pedido | 200, 401, 404, 409 |
| `GET` | `/api/v1/inventory/check?productCode=&quantity=` | Consultar disponibilidad | 200, 400, 401 |
| `PUT` | `/api/v1/inventory/reduce-stock?productCode=&quantity=` | Descontar stock (atómico) | 200, 400, 401, 404, 409 |
| `POST` | `/api/v1/inventory/products` | Registrar un producto | 201, 400, 401, 409 |
| `GET` | `/api/v1/inventory/products` | Listar productos | 200, 401 |
| `GET` | `/api/v1/inventory/products/{code}` | Consultar un producto | 200, 401, 404 |
| `PUT` | `/api/v1/inventory/products/{code}/stock` | Ajustar el stock (valor absoluto ≥ 0) | 200, 400, 401, 404 |

Ejemplo — crear un pedido:

```bash
TOKEN=<access_token>
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -H "X-Trace-Id: demo-001" \
  -d '{"productCode":"PROD-001","quantity":2}'
```

```json
{
  "id": 1,
  "productCode": "PROD-001",
  "quantity": 2,
  "totalPrice": 2400.00,
  "status": "CONFIRMED",
  "createdAt": "2026-10-20T01:15:30.123"
}
```

Validaciones del cuerpo: `productCode` obligatorio (máx. 50 caracteres) y `quantity` entre 1 y 10000.

Ejemplo — registrar un producto y ajustar su stock:

```bash
curl -s -X POST http://localhost:8080/api/v1/inventory/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"code":"PROD-003","name":"Mouse Inalambrico","stock":25,"price":35.50}'

curl -s -X PUT http://localhost:8080/api/v1/inventory/products/PROD-003/stock \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"stock":50}'
```

## Modelo de errores

Todos los errores (Gateway, Order e Inventory) usan el mismo formato; `traceId` coincide con la cabecera `X-Trace-Id`:

```json
{
  "timestamp": "2026-10-20T01:15:30.123Z",
  "status": 404,
  "code": "ORDER_NOT_FOUND",
  "message": "Pedido no encontrado con ID: 999999",
  "traceId": "demo-001"
}
```

| Situación | HTTP | `code` | Origen |
|---|---|---|---|
| Datos inválidos / JSON mal formado | 400 | `INVALID_REQUEST` | Order / Inventory |
| Stock insuficiente | 400 | `STOCK_INSUFFICIENT` | Order |
| Token ausente | 401 | `UNAUTHORIZED` | Gateway / servicios |
| Token inválido o expirado | 401 | `TOKEN_INVALID_OR_EXPIRED` | Gateway / servicios |
| Sin permisos | 403 | `FORBIDDEN` | Gateway / servicios |
| Pedido inexistente | 404 | `ORDER_NOT_FOUND` | Order |
| Producto inexistente | 404 | `PRODUCT_NOT_FOUND` | Inventory |
| Producto duplicado | 409 | `PRODUCT_ALREADY_EXISTS` | Inventory |
| Descuento de stock sin existencias | 409 | `STOCK_INSUFFICIENT` | Inventory |
| Ruta inexistente | 404 | `ROUTE_NOT_FOUND` / `RESOURCE_NOT_FOUND` | Gateway / servicios |
| Método no permitido | 405 | `METHOD_NOT_ALLOWED` | Todos |
| Transición de estado inválida | 409 | `INVALID_TRANSITION` | Order |
| Error interno (sin exponer detalle) | 500 | `INTERNAL_ERROR` | Todos |
| Inventory no disponible | 503 | `INVENTORY_UNAVAILABLE` | Order |
| Servicio destino caído | 503 | `SERVICE_UNAVAILABLE` | Gateway |

## Estados del pedido y flujo de creación

```
            +--> CONFIRMED --> CANCELLED
 PENDING ---+--> CANCELLED
            +--> FAILED
```

| Estado | Significado | Transiciones permitidas |
|---|---|---|
| `PENDING` | Pedido registrado, pendiente de validar stock | `CONFIRMED`, `CANCELLED`, `FAILED` |
| `CONFIRMED` | Stock validado y descontado | `CANCELLED` |
| `CANCELLED` | Cancelado por el usuario (final) | — |
| `FAILED` | No pudo confirmarse: stock insuficiente o inventario no disponible (final) | — |

Cualquier otra transición (p. ej. cancelar dos veces) responde **409 `INVALID_TRANSITION`**.

Flujo de `POST /api/v1/orders`:

1. Se registra el pedido en `PENDING` (historial `NONE → PENDING`).
2. Order Service consulta la disponibilidad en Inventory (WebClient, timeout 5 s).
3. Sin stock → el pedido pasa a `FAILED` y se responde 400 `STOCK_INSUFFICIENT`.
4. Con stock → se descuenta el stock **de forma atómica** (`UPDATE ... SET stock = stock - :q WHERE stock >= :q`) y el pedido pasa a `CONFIRMED` (201). Si otro pedido consumió el stock entre la consulta y el descuento, Inventory responde 409 y el pedido pasa a `FAILED` con 400 `STOCK_INSUFFICIENT`. Si Inventory falla, el pedido pasa a `FAILED` y se responde 503 `INVENTORY_UNAVAILABLE`.

Cada cambio de estado queda en el historial (estado anterior, nuevo, motivo y fecha).

## Probar con Postman y Swagger

**Postman:** 
1. Importar `postman/Reto Backend Microservicios.postman_collection.json`.
2. Ejecutar primero **"Obtener Token (Keycloak)"**; guarda el token en la variable `access_token`.
3. El resto de requests usa ese token automáticamente (autenticación Bearer a nivel de colección). 
4. Las requests incluyen tests: flujo feliz, sin stock, datos inválidos, 404, 409 por cancelación duplicada, 401 sin token / token inválido y documentos OpenAPI. Se pueden correr en orden con el *Collection Runner*; el `id` del pedido se guarda en `order_id`.
> Nota: Keycloak tarda ~30-60 s en iniciar la primera vez. Si el token falla, espera y reintenta.

**Swagger UI** 
1. (a través del Gateway): http://localhost:8080/swagger-ui.html — selector *Order Service* / *Inventory Service*. 
2. Para probar: obtener el token, pulsar **Authorize** y pegar el `access_token`. 
3. Los documentos JSON están en `/docs/orders/api-docs` y `/docs/inventory/api-docs`.

## Decisiones de diseño

- **Gateway como único punto público:** valida el JWT, genera/propaga `X-Trace-Id` y publica la documentación. Cada microservicio revalida el token (defensa en profundidad) y Order reenvía el token del usuario a Inventory.
- **Una base de datos por microservicio:** autonomía y aislamiento; la integración es solo por HTTP.
- **Estados en el dominio:** el enum `OrderStatus` define las transiciones permitidas; `OrderTransitionService` es el único punto donde cambia un estado y valida, persiste y registra el historial en una transacción corta, con bloqueo de fila (`PESSIMISTIC_WRITE`) para evitar cancelaciones simultáneas.
- **Sin transacciones abiertas durante llamadas HTTP:** `OrderService` orquesta el caso de uso y no es transaccional; cada paso de BD es una transacción corta.
- **Puerto/adaptador para Inventory:** `OrderService` depende de la interfaz `InventoryClient`; `WebClientInventoryClient` es el adaptador (timeout, propagación de `X-Trace-Id` y token, traducción de errores a `InventoryServiceException`).
- **DTOs + MapStruct:** el controller nunca expone entidades JPA; `OrderMapper` se genera en compilación.
- **Errores uniformes:** un `GlobalExceptionHandler` por servicio y manejadores de seguridad/enrutamiento en el Gateway producen siempre el mismo formato, sin filtrar detalles internos.
- **AOP:** aspectos de logging para preocupaciones transversales (entrada, salida, duración, fallos) sin ensuciar la lógica de negocio.
- **Validación del emisor (`iss`) desacoplada de la descarga de llaves:** los servicios descargan las llaves de `keycloak:8080` (red interna) pero validan el emisor público `http://localhost:8180/realms/reto`; las llaves se piden de forma perezosa, así que los servicios arrancan aunque Keycloak aún no esté listo.
- **Descuento de stock atómico en Inventory:** la condición `stock >= cantidad` se evalúa dentro del propio `UPDATE`, por lo que no hay sobreventa ni stock negativo bajo concurrencia. Si se pierde la carrera, Inventory responde 409 y Order marca el pedido `FAILED`.
- **Cancelar no repone stock (decisión deliberada):** reponerlo implicaría una compensación distribuida entre servicios, que el reto declara fuera de alcance. Un operador puede reponer existencias con `PUT /api/v1/inventory/products/{code}/stock`.
- **Order Service usa Spring MVC + `WebClient`:** el cliente HTTP reactivo se invoca con timeout de 5 s dentro de un flujo secuencial (consultar → descontar → confirmar), que necesita el resultado antes de continuar; el Gateway sí es totalmente reactivo (WebFlux).
- **Sin Saga ni compensaciones distribuidas** (fuera del alcance del reto): si algo falla después de descontar stock no hay reversa automática.

## Trazabilidad y logging

- El Gateway genera un `X-Trace-Id` (UUID) si el cliente no lo envía, lo reenvía a los servicios y lo devuelve en la respuesta, incluso en errores 401/403.
- Order e Inventory lo guardan en el MDC; Logback lo imprime en cada línea: `... INFO [traceId=demo-001] [http-nio-8081-exec-1] c.e.o.service.OrderService ...`. Order lo propaga a Inventory.
- En el Gateway (reactivo) el `traceId` se escribe explícitamente en los logs de entrada y salida de cada petición.
- Seguir una petición completa: `docker compose logs | grep demo-001`.

## Pruebas (JUnit 5)

```bash
# Dentro de la carpeta de cada servicio (JDK 17 + Maven)
mvn test

# sin instalar nada, con Docker
docker run --rm -v "$PWD/order-service":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn test
```

Cubren: reglas de transición de estados, servicio de pedidos (con stock, sin stock, Inventory caído, fallo al descontar), historial, mapper, controllers con el formato de error (400/404/409/503/500), seguridad (401 sin token, token inválido, acceso con token) en Order e Inventory, y el filtro de `X-Trace-Id` y el 401 del Gateway. Los Dockerfiles construyen con `-Dmaven.test.skip=true` para acelerar la imagen; los tests se ejecutan aparte.

## Estructura del proyecto

```
.
├── docker-compose.yml
├── keycloak/realm-export.json          # realm, cliente y usuario de prueba
├── docker-compose.dev.yml              # opcional: publica puertos internos solo para desarrollo
├── postman/Reto Backend Microservicios.postman_collection.json
├── api-gateway/                        # Spring Cloud Gateway + seguridad + Swagger agregado
├── order-service/                      # controller, service, client (puerto/adaptador), mapper, model, aspect
└── inventory-service/                  # controller, service, repository, model, aspect
```

## Limitaciones conocidas

- Cancelar un pedido no devuelve el stock a Inventory (no se implementan compensaciones; ver *Decisiones de diseño*).
- Todas las rutas autenticadas, incluidas las de administración de productos, están disponibles para cualquier usuario con token válido (no hay roles).
- Las credenciales de Keycloak y PostgreSQL son de desarrollo; el esquema se crea con `ddl-auto: update` (sin migraciones Flyway/Liquibase).
- No hay pruebas de integración con base de datos real.

## Solución de problemas

| Síntoma | Causa probable / solución |
|---|---|
| 401 `TOKEN_INVALID_OR_EXPIRED` con un token recién emitido | El token se pidió a una URL distinta de `http://localhost:8180`. Debe usarse `localhost:8180` para que el `iss` coincida |
| Falla al pedir el token | Keycloak aún está iniciando (30–60 s); reintentar |
| Un servicio se reinicia al arrancar | Esperar: depende de que su base de datos esté `healthy` |
| Error de esquema o columnas tras actualizar el código | Recrear las bases: `docker compose down -v && docker compose up --build` |
| Puerto ocupado | Cambiar el mapeo de puertos en `docker-compose.yml` (8080 y 8180) |

---

## 👩‍💻 Desarrollado por

**Nayeli Alison Paitan Ramirez**  
*Desarrolladora Backend / Full Stack*

Proyecto desarrollado como demostración técnica de una arquitectura de microservicios con Java 17, Spring Boot, API Gateway, OAuth 2.0 y Docker[cite: 2].

📬 **¿Te interesa mi perfil o buscas sumar talento a tu equipo?**
* **LinkedIn:** [Nayeli Alison Paitan Ramirez](https://www.linkedin.com/in/nayeli-alison-paitan-ramirez/)
* **WhatsApp:** [+51 950 595 875](https://wa.me/51950595875)
* **Correo:** [paitannayeli@gmail.com](mailto:paitannayeli@gmail.com)
* **GitHub:** [@NayeliPaitan](https://github.com/NayeliPaitan)

¡Estoy abierta a nuevas oportunidades laborales y proyectos profesionales! 🚀