# Attendance App — Configuración y Documentación del Backend

## Tabla de contenidos
- [Requisitos](#requisitos)
- [Variables de entorno](#variables-de-entorno)
- [Cloudinary (imágenes)](#cloudinary-imágenes)
- [Endpoints](#endpoints)
- [Roles y seguridad](#roles-y-seguridad)
- [Lógica de asistencia](#lógica-de-asistencia)
- [Reportes (Excel y PDF)](#reportes-excel-y-pdf)
- [Mantenimiento de la base de datos](#mantenimiento-de-la-base-de-datos)
- [Entidades](#entidades)

---

## Requisitos

- Java 21
- PostgreSQL (plan gratuito soportado — ver sección de mantenimiento)
- Cuenta gratuita en [Cloudinary](https://cloudinary.com) para alojar imágenes

---

## Variables de entorno

Crear un archivo `.env.properties` en la raíz del proyecto (está en `.gitignore`):

```properties
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=attendance_db
DB_USER=postgres
DB_PASSWORD=tu_password

# JWT
JWT_SECRET=tu_clave_secreta_muy_larga

# Cloudinary (para firmas y huellas digitales)
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
```

---

## Cloudinary (imágenes)

Las imágenes de **firma**, **huella digital** y **foto de perfil** de los usuarios no se guardan en la base de datos (mala práctica y consume espacio). Se alojan en Cloudinary y solo se guarda la URL en la DB.

> **Nota:** `signatureUrl` y `fingerprintUrl` **nunca se exponen en la plataforma** (ni en `/api/auth/me`, ni en la lista/detalle de usuarios, ni en la lista de asistencias del director) — solo se usan internamente para embeber las imágenes en los reportes Excel/PDF. `photoUrl` sí es visible en la plataforma (navbar, perfil, lista de usuarios).

### Configuración gratuita
1. Registrarse en [cloudinary.com](https://cloudinary.com) — el plan Free incluye **25 GB de almacenamiento** y **25 GB de ancho de banda mensual**, más que suficiente para un colegio.
2. Ir al Dashboard y copiar `Cloud Name`, `API Key` y `API Secret`.
3. Pegarlos en `.env.properties` (ver arriba).

### Subir imágenes
```
POST /api/users/{userId}/signature    — multipart/form-data, campo "file"
POST /api/users/{userId}/fingerprint  — multipart/form-data, campo "file"
POST /api/users/{userId}/photo        — multipart/form-data, campo "file"
```
- Un docente/director solo puede subir su propia imagen.
- Un DIRECTOR puede subir la imagen (firma, huella o foto) de cualquier usuario.
- Tamaño máximo: 5 MB por imagen (configurable en `application.properties`).
- La URL pública queda guardada en el campo `signatureUrl` / `fingerprintUrl` / `photoUrl` del usuario.

---

## Endpoints

### Autenticación (`/api/auth`)

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `POST` | `/api/auth/register` | público | Registrar nuevo usuario |
| `POST` | `/api/auth/login` | público | Login — devuelve JWT + datos del usuario |
| `GET`  | `/api/auth/me` | autenticado | Perfil del usuario actual (nombre, rol, URL de foto) |

**Login response:**
```json
{
  "status": "Login exitoso!",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": 1,
    "username": "jdoe",
    "firstName": "John",
    "lastName": "Doe",
    "role": "TEACHER"
  }
}
```

**GET /api/auth/me response:**
```json
{
  "id": 1,
  "username": "jdoe",
  "firstName": "John",
  "lastName": "Doe",
  "role": "TEACHER",
  "photoUrl": "https://res.cloudinary.com/..."
}
```

---

### Asistencias — Vista TEACHER (`/api/attendances`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET`  | `/api/attendances/today` | Registro de hoy del docente autenticado (204 si no hay) |
| `POST` | `/api/attendances` | Check-in manual con tiempo y estado personalizados |
| `POST` | `/api/attendances/quick-checkin` | Check-in rápido a la hora actual |
| `POST` | `/api/attendances/quick-checkout` | Check-out rápido a la hora actual (solo después de 13:00) |
| `PATCH`| `/api/attendances/{id}` | Actualización parcial de un registro |

**Body para POST /api/attendances:**
```json
{
  "date": "2026-05-28",
  "timeIn": "07:30:00",
  "timeOut": "13:00:00",
  "status": "Present",
  "notes": "Nota opcional"
}
```
> `timeOut` y `notes` son opcionales. `status` acepta: `Present`, `Late`, `Absent`.

**Quick Check-In:** registra la hora actual del servidor. El estado se calcula automáticamente:
- Si la hora de entrada es ≤ 07:30 → `Present`
- Si es después de 07:30 → `Late`

**Quick Check-Out:** solo permitido después de las 13:00 (hora del servidor). Si se intenta antes, devuelve 400.

---

### Asistencias — Vista DIRECTOR (`/api/attendances`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET`    | `/api/attendances` | Lista paginada con filtros (solo DIRECTOR) |
| `DELETE` | `/api/attendances/by-date/{date}` | Elimina todos los registros de una fecha (solo DIRECTOR) |

**Parámetros de filtro para GET /api/attendances:**

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `teacherName` | string | Busca en firstName, lastName y username |
| `status` | string | `Present`, `Late` o `Absent` |
| `fromDate` | date | Fecha desde (ISO: `2026-01-01`) |
| `toDate` | date | Fecha hasta (ISO: `2026-01-31`) |
| `sortBy` | string | `date` (default), `teacherName`, `status` |
| `order` | string | `asc` o `desc` (default: `desc`) |
| `page` | int | Número de página (0-based) |
| `size` | int | Registros por página |

**Ejemplo:**
```
GET /api/attendances?teacherName=garcia&status=Late&fromDate=2026-05-01&sortBy=date&order=desc&page=0&size=10
```

**Response item:**
```json
{
  "id": 42,
  "teacherId": 3,
  "teacherName": "Maria Garcia",
  "date": "2026-05-28",
  "timeIn": "08:05:00",
  "timeOut": "13:00:00",
  "status": "Late",
  "notes": "Tráfico"
}
```

---

### Dashboard (`/api/dashboard`) — Solo DIRECTOR

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/dashboard/summary` | Resumen del día actual |

**Response:**
```json
{
  "totalRecords": 348,
  "presentToday": 12,
  "lateToday": 3,
  "absentToday": 5
}
```
> `absentToday` = total de docentes con rol TEACHER − (presentToday + lateToday)

---

### Reportes (`/api/reports`) — Solo DIRECTOR

Aceptan los mismos parámetros de filtro que `GET /api/attendances`.

| Método | Ruta | Tipo de respuesta |
|--------|------|-------------------|
| `GET` | `/api/reports/excel` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `GET` | `/api/reports/pdf` | `application/pdf` |

**Columnas del reporte:**
`Docente | Rol | Fecha | Entrada | Salida | Estado | Notas | Foto | Firma | Huella`

Foto, Firma y Huella se descargan desde Cloudinary y se **embeben como imágenes reales** en la celda (no como texto/URL). Si una imagen falta o no se puede descargar, la celda queda en blanco sin romper el resto del reporte. Por defecto, los registros se ordenan del más reciente al más antiguo (`order=desc`).

**Ejemplo de descarga con filtro de fecha:**
```
GET /api/reports/excel?fromDate=2026-05-28&toDate=2026-05-28
```

---

### Usuarios (`/api/users`) — Solo DIRECTOR

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET`  | `/api/users` | Lista paginada de usuarios con sus registros |
| `GET`  | `/api/users/{id}` | Detalle de un usuario con todos sus registros |
| `POST` | `/api/users/{id}/signature` | Subir imagen de firma (multipart) |
| `POST` | `/api/users/{id}/fingerprint` | Subir imagen de huella (multipart) |
| `POST` | `/api/users/{id}/photo` | Subir foto de perfil (multipart) |

---

## Roles y seguridad

- **`TEACHER`**: puede registrar y ver su propia asistencia. Puede subir su propia firma y huella.
- **`DIRECTOR`**: acceso completo — lista de todos los registros, dashboard, reportes, eliminación, subida de imágenes de cualquier usuario.

El token JWT se envía en el header:
```
Authorization: Bearer eyJ...
```

- Access token: expira en **1 hora**
- Refresh token: expira en **7 días**

---

## Lógica de asistencia

| Condición | Estado |
|-----------|--------|
| `timeIn` ≤ 07:30 | `Present` |
| `timeIn` > 07:30 | `Late` |
| Sin registro en el día | Cuenta como ausente en el dashboard |

Un docente solo puede tener **un registro por día**. Si intenta crear un segundo, recibe `409 Conflict`.

---

## Reportes (Excel y PDF)

Los reportes incluyen, tanto de docentes como del director:
- Nombre completo y rol (Docente / Director)
- Fecha, hora de entrada, hora de salida
- Estado (`Present` / `Late` / `Absent`)
- Notas
- Foto de perfil, firma y huella digital, **embebidas como imágenes** dentro de la celda (no URLs) — orden por defecto: más recientes primero.

Los reportes se generan al vuelo con los mismos filtros de la vista del director. No hay caché de reportes — cada descarga consulta la DB en el momento, pero las imágenes de un mismo usuario se descargan una sola vez por reporte y se reutilizan en todas sus filas.

---

## Mantenimiento de la base de datos

El plan gratuito de PostgreSQL (ej. Neon, Supabase, Railway) tiene un límite de **512 MB**. Con asistencias de entrada y salida de lunes a viernes, los datos se acumulan rápido.

### Flujo recomendado al final de cada día (o semana):

1. Descargar el reporte Excel o PDF del día:
   ```
   GET /api/reports/excel?fromDate=2026-05-28&toDate=2026-05-28
   ```
2. Verificar que el archivo es correcto.
3. Eliminar los registros de esa fecha:
   ```
   DELETE /api/attendances/by-date/2026-05-28
   ```
   Response:
   ```json
   { "deletedCount": 20, "date": "2026-05-28" }
   ```

> Este flujo puede automatizarse desde el frontend con un botón "Generar y limpiar".

---

## Entidades

### User
```
id           — Long (PK)
username     — String, único (para login)
password     — String (BCrypt)
firstName    — String
lastName     — String
role         — TEACHER | DIRECTOR
signatureUrl — String (URL Cloudinary, solo interno — no se expone en la plataforma)
fingerprintUrl — String (URL Cloudinary, solo interno — no se expone en la plataforma)
photoUrl     — String (URL Cloudinary, visible en la plataforma)
```

### AttendanceRecord
```
id       — Long (PK)
user     — User (FK)
date     — LocalDate
timeIn   — LocalTime
timeOut  — LocalTime (nullable)
status   — Present | Late | Absent
notes    — String (nullable)
```
