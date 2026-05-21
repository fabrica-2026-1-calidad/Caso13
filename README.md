[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=fabrica-2026-1-calidad_Caso13&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=fabrica-2026-1-calidad_Caso13)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fabrica-2026-1-calidad_Caso13&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fabrica-2026-1-calidad_Caso13)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=fabrica-2026-1-calidad_Caso13&metric=bugs)](https://sonarcloud.io/summary/new_code?id=fabrica-2026-1-calidad_Caso13)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fabrica-2026-1-calidad_Caso13&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fabrica-2026-1-calidad_Caso13)

# Domésticas API

Backend REST para la gestión de tareas del hogar. Construido con Spring Boot 3.5, PostgreSQL, Flyway y Docker.

---

## Tecnologías

- **Java 21** + **Spring Boot 3.5.13**
- **Spring Security** + **JWT** (filtro de autenticación implementado)
- **Spring Data JPA** + **Hibernate** para persistencia
- **Flyway** para migraciones versionadas de base de datos
- **PostgreSQL 15** como base de datos
- **Docker Desktop** para correr la base de datos localmente
- **Mailtrap** para pruebas de envío de correo
- **Springdoc OpenAPI** (Swagger UI) para documentación interactiva de endpoints

---

## Requisitos previos

Antes de clonar y correr el proyecto, asegúrate de tener instalado lo siguiente en tu máquina.

- [Java 21](https://adoptium.net/) — puedes verificar con `java -version`
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — para levantar la base de datos
- Una cuenta gratuita en [Mailtrap](https://mailtrap.io) — para probar el envío de correos de recuperación de contraseña

---

## Configuración paso a paso

### 1. Clonar y configurar variables de entorno

```bash
git clone https://github.com/tu-usuario/domesticas.git
cd domesticas
cp .env.example .env
# Edita .env con tus credenciales de Mailtrap y ajusta el JWT_SECRET
```

### 2. Levantar la base de datos con Docker

El proyecto incluye un `docker-compose.yml` en la raíz con toda la configuración necesaria. El primer `docker compose up` ejecuta automáticamente el script `init-db/01_schema.sql` que crea todas las tablas del esquema.

```bash
docker compose up -d
```

Puedes verificar que las tablas se crearon correctamente con este comando:

```bash
docker exec -it postgres_domesticas psql -U postgres -d domesticas_db -c "\dt"
```

Deberías ver las tablas `hogar`, `usuario`, `usuario_hogar`, `tarea` , `password_reset_token`,`invitacion_hogar`y`flyway_schema_history`.

> **Importante:** si ya tenías una versión anterior del volumen de Docker, elimínalo antes de levantar el contenedor para que el script de inicialización se ejecute desde cero. Puedes hacerlo desde la sección **Volumes** de Docker Desktop.
### 3. Ejecutar migraciones (Flyway)

```bash
./scripts/deploy-db.sh local
```

### 4. Configurar Mailtrap

Edita `.env` con tus credenciales de Mailtrap [mailtrap.io](https://mailtrap.io) (Email Testing → My Sandbox → Integration).

### 5. Ejecutar la aplicación

Desde la raíz del proyecto ejecuta:

```bash
.\mvnw spring-boot:run        # Windows (PowerShell)
./mvnw spring-boot:run        # Mac / Linux
```

No necesitas tener Maven instalado globalmente — el `mvnw` es un wrapper que descarga automáticamente la versión correcta de Maven para este proyecto.

Cuando veas este mensaje en la consola, la aplicación está lista:

```
Started DomesticasApplication in X seconds
```
La API estará disponible en `http://localhost:8080`.

---

## Documentación interactiva (Swagger UI)

Una vez que la aplicación esté corriendo, abre el navegador y ve a:

```
http://localhost:8080/swagger-ui/index.html
```
Ahí encontrarás todos los endpoints documentados y podrás probarlos directamente desde el navegador sin necesidad de instalar ninguna herramienta adicional.
Para generar documentación estática:

```bash
./scripts/generate-api-docs.sh
# Archivos generados en docs/: openapi.json, openapi.yaml, postman_collection.json
```

---

## Endpoints disponibles

Todos los prefijos bajo `/api`.

### Autenticación — `/api/auth`

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/auth/register` | Registrar nuevo usuario | No |
| `POST` | `/api/auth/login` | Iniciar sesión, obtener JWT | No |
| `POST` | `/api/auth/logout` | Cerrar sesión (cliente descarta token) | No |
| `POST` | `/api/auth/forgot-password` | Solicitar recuperación de contraseña | No |
| `POST` | `/api/auth/reset-password` | Establecer nueva contraseña con token | No |

### Hogares — `/api/households`

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/households` | Crear un nuevo hogar | JWT |
| `POST` | `/api/households/{id}/invite` | Invitar miembro al hogar | JWT (Admin) |
| `POST` | `/api/households/invitations/{token}/respond` | Aceptar/rechazar invitación | JWT |
| `GET` | `/api/households/{id}/members` | Listar miembros del hogar | JWT (Miembro) |

### Tareas — `/api/households/{id}/tasks` y `/api/tasks`

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/households/{hogarId}/tasks` | Crear tarea en un hogar | JWT (Miembro) |
| `GET` | `/api/households/{hogarId}/tasks?estado=&categoria=&asignadoA=` | Listar tareas con filtros | JWT (Miembro) |
| `GET` | `/api/tasks/{tareaId}` | Obtener detalle de tarea | JWT (Miembro) |
| `PUT` | `/api/tasks/{tareaId}` | Actualizar tarea | JWT (Miembro) |
| `PATCH` | `/api/tasks/{tareaId}/status` | Cambiar estado de tarea | JWT (Miembro) |
| `DELETE` | `/api/tasks/{tareaId}` | Eliminar tarea | JWT (Admin) |

### Flujo completo de uso

1. **Registro** → `POST /api/auth/register`
2. **Login** → `POST /api/auth/login` → obtienes JWT
3. **Crear hogar** → `POST /api/households` (Header: `Authorization: Bearer <token>`)
4. **Invitar miembros** → `POST /api/households/{id}/invite`
5. **Crear tareas** → `POST /api/households/{id}/tasks`
6. **Gestionar tareas** → `GET`, `PUT`, `PATCH` sobre `/api/tasks/{id}`

### Estados de tarea

`Pendiente` → `En_progreso` → `Completada`

### Categorías de tarea

`Limpieza`, `Cocina`, `Compras`, `Mantenimiento`, `Otro`

---

## Estructura del proyecto

```
src/main/java/com/eap08/domesticas/
├── DomesticasApplication.java    ← punto de entrada de Spring Boot
├── controller/                   ← recibe peticiones HTTP y delega al servicio
├── dto/                          ← objetos de transferencia de datos (entrada/salida de la API)
├── model/                        ← entidades JPA que mapean las tablas de la BD
├── repository/                   ← interfaces de acceso a datos (Spring Data JPA)
├── security/                     ← configuración de Spring Security, JWT y manejo de errores
└── service/
    ├── (interfaces)              ← contratos de la lógica de negocio
    └── impl/                     ← implementaciones concretas de los servicios

src/main/resources/
├── application.properties         
└── db/migration/
    ├── V1__init_schema.sql        ← esquema db
    └── V2__invitacion_hogar.sql   ← tabla de invitaciones

scripts/
├── deploy-db.sh                   ← despliegue de migraciones
├── generate-api-docs.sh           ← generación de OpenAPI + Postman
└── test-api.sh                    ← prueba peticiones rest, genera valores por defecto para probar

docs/
├── openapi.json                   ← especificación OpenAPI 3.0
├── domesticas.postman_collection.json
└── vulnerability-report.md        ← informe de vulnerabilidades
```

---

## Manejo de errores

Todos los errores de la API siguen una estructura uniforme:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Los datos enviados no son válidos",
  "details": ["El formato del correo no es válido"],
  "traceId": "a1b2c3d4-...",
  "timestamp": "2026-04-03T10:00:00"
}
```

Códigos: `VALIDATION_ERROR` (400), `INVALID_CREDENTIALS` (401), `BUSINESS_ERROR` (409), `INTERNAL_ERROR` (500).

---

## Autenticación

Todas las peticiones a endpoints protegidos requieren el header:

```
Authorization: Bearer <jwt_token>
```

El token se obtiene mediante `POST /api/auth/login`. El filtro `JwtAuthFilter` valida automáticamente el token en cada petición. Endpoints públicos (`/api/auth/**`, Swagger) no requieren token.

---

## Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña BD |
| `APP_JWT_SECRET` | Secreto para firmar JWT |
| `APP_JWT_EXPIRATION` | Expiración del token (ms) |
| `SPRING_MAIL_HOST` | Servidor SMTP |
| `SPRING_MAIL_PORT` | Puerto SMTP |
| `SPRING_MAIL_USERNAME` | Usuario SMTP |
| `SPRING_MAIL_PASSWORD` | Contraseña SMTP |
| `APP_FRONTEND_URL` | URL del frontend |
| `APP_CORS_ORIGINS` | Orígenes CORS (coma) |

---

## Seguridad — Informe de vulnerabilidades

Ver `docs/vulnerability-report.md` para el informe completo con 8 hallazgos documentados (corregidos y recomendaciones pendientes).

---

## Flujo de trabajo con Git

El equipo trabaja con **feature branches**. Cada Historia de Usuario tiene su propia rama con el prefijo `feature/`. Cuando el trabajo esté listo, se abre un Pull Request hacia `main` para revisión antes de hacer merge.

```bash
# Crear una rama para una HU nueva
git checkout -b feature/nombre-de-la-hu

# Subir cambios durante el desarrollo
git add .
git commit -m "feat: descripción breve del cambio"
git push origin feature/nombre-de-la-hu
```
