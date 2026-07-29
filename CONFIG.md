# Attendance App — Configuración y Documentación del Backend

## Tabla de contenidos
- [Requisitos](#requisitos)
- [Variables de entorno](#variables-de-entorno)
- [Cloudinary (imágenes)](#cloudinary-imágenes)
- [Endpoints](#endpoints)
- [Roles y seguridad](#roles-y-seguridad)
- [Lógica de asistencia](#lógica-de-asistencia)
- [Reportes (Excel y PDF)](#reportes-excel-y-pdf)
- [Reportes mensuales, resumen diario y aviso de purga](#reportes-mensuales-resumen-diario-y-aviso-de-purga)
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

# EmailJS (una sola plantilla genérica para verificación, reset, reporte mensual y resumen diario)
EMAILJS_SERVICE_ID=tu_service_id
EMAILJS_TEMPLATE_ID=tu_template_id
EMAILJS_PUBLIC_KEY=tu_public_key
EMAILJS_PRIVATE_KEY=tu_private_key

# URL del frontend (se usa como link dentro de los correos)
FRONTEND_URL=http://localhost:4200
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
| `POST` | `/api/auth/forgot-password` | público | Solicita un código de restablecimiento de contraseña |
| `POST` | `/api/auth/reset-password` | público | Restablece la contraseña con el código recibido |

> **`username` acepta usuario o correo:** el campo `username` de `login`, `forgot-password` y `reset-password` acepta tanto el nombre de usuario como el correo electrónico registrado — el backend resuelve el identificador contra ambos campos (`findByUsernameOrEmail`).

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
| `GET`  | `/api/attendances/day-status` | Indica si hoy es feriado (Perú) o fin de semana — cualquier usuario autenticado |
| `POST` | `/api/attendances` | Check-in manual con tiempo y estado personalizados |
| `POST` | `/api/attendances/quick-checkin` | Check-in rápido a la hora actual (solo entre 7:30 am y 9:00 am) |
| `POST` | `/api/attendances/quick-checkout` | Check-out rápido a la hora actual (solo entre 1:00 pm y 2:00 pm) |
| `PATCH`| `/api/attendances/{id}` | Actualización parcial de un registro |
| `GET`  | `/api/attendances/me` | Historial paginado y filtrable de las **propias** asistencias (docente o director) |

**Body para POST /api/attendances:**
```json
{
  "date": "2026-05-28",
  "timeIn": "07:30:00",
  "timeOut": "13:15:00",
  "status": "Present",
  "notes": "Nota opcional"
}
```
> `timeOut` y `notes` son opcionales. `status` acepta: `Present`, `Late`, `Absent`. `timeIn` debe estar entre las 7:30 am y las 9:00 am (`400` si no). Si se envía `timeOut`, debe estar entre la 1:00 pm y las 2:00 pm (`400` si no); estas mismas validaciones aplican también a `PATCH /api/attendances/{id}` cuando se modifica `timeIn`/`timeOut`.

**Quick Check-In:** registra la hora actual del servidor. Solo habilitado entre las 7:30 am y las 9:00 am (fuera de ese rango, `400` — el registro de entrada queda cerrado por el resto del día). El estado se calcula automáticamente:
- Si la hora de entrada es ≤ 08:20 → `Present`
- Si es después de 08:20 (hasta las 9:00 am) → `Late`

**Quick Check-Out:** solo permitido entre la 1:00 pm y las 2:00 pm (hora del servidor; 1:30 pm es la hora objetivo — de 1:00 a 1:32 se considera a tiempo, de 1:32 a 2:00 es tolerancia — y a las 2:00 pm cierra por completo). Fuera de esa ventana, devuelve `400`. La salida nunca afecta el `status` del día (que depende solo de la entrada); si nunca se registra, el frontend simplemente indica "No registró salida".

**Feriados de Perú (`util/PeruHolidays`):** `POST /api/attendances`, `/quick-checkin` y `/quick-checkout` devuelven `400` si la fecha es un feriado nacional (calendario 2026 hardcodeado — Año Nuevo, Semana Santa, Fiestas Patrias, etc., 16 fechas en total). `GET /api/attendances/day-status` expone `{ "holiday": true, "holidayName": "Fiestas Patrias", "weekend": false }` para que el frontend deshabilite el check-in/out proactivamente. El resumen del dashboard (`/api/dashboard/summary`) también devuelve todo en 0 los días feriados, igual que fines de semana. `POST /api/attendances/for-user/{userId}` (backfill del director) **sí** valida feriado (nadie debía asistir ese día, no hay nada que "corregir") y además rechaza fechas futuras (`400`) — solo puede completar asistencias de días laborables ya pasados o de hoy. Solo omite las ventanas horarias de entrada/salida, no el resto de reglas. > **Mantenimiento:** la lista de feriados está hardcodeada para 2026 y debe actualizarse cada año (`Jueves Santo`/`Viernes Santo` son móviles).

**GET /api/attendances/me:** acepta los mismos parámetros de filtro que la vista DIRECTOR (`status`, `fromDate`, `toDate`, `dayOfWeek`, `sortBy` — solo `date`/`status`, no `teacherName` — `order`, `page`, `size`) pero siempre restringido al usuario autenticado; devuelve `AttendanceRecordResponseDTO` (sin nombre de docente, ya que es siempre el propio).

---

### Asistencias — Vista DIRECTOR (`/api/attendances`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET`    | `/api/attendances` | Lista paginada con filtros (solo DIRECTOR) |
| `POST`   | `/api/attendances/for-user/{userId}` | Crea un registro para otro usuario (backfill), sin validar las ventanas de entrada/salida (solo DIRECTOR) |
| `DELETE` | `/api/attendances/by-date/{date}` | Elimina todos los registros de una fecha (solo DIRECTOR) |

**`POST /api/attendances/for-user/{userId}`:** pensado para cuando un docente tuvo una falla/interrupción y no pudo marcar dentro de su ventana — el director registra el mismo body que `POST /api/attendances` (`date`, `timeIn`, `timeOut`, `status`, `notes`) pero **sin** las validaciones de ventana horaria. Sigue respetando la regla de un registro por día por usuario (`409` si ya existe), sigue bloqueando feriados (`400`) y rechaza fechas futuras (`400`) — solo corrige asistencias de días laborables pasados o de hoy.

**Parámetros de filtro para GET /api/attendances:**

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `teacherName` | string | Busca en firstName, lastName y username |
| `status` | string | `Present`, `Late` o `Absent` |
| `fromDate` | date | Fecha desde (ISO: `2026-01-01`) |
| `toDate` | date | Fecha hasta (ISO: `2026-01-31`) |
| `dayOfWeek` | string | `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY` — filtra por día de la semana (derivado de `date`, no requiere una columna nueva) |
| `sortBy` | string | `date` (default), `teacherName`, `status` |
| `order` | string | `asc` o `desc` (default: `desc`) |
| `page` | int | Número de página (0-based) |
| `size` | int | Registros por página |

**Ejemplo:**
```
GET /api/attendances?teacherName=garcia&status=Late&fromDate=2026-05-01&dayOfWeek=MONDAY&sortBy=date&order=desc&page=0&size=10
```

**Response item:**
```json
{
  "id": 42,
  "teacherId": 3,
  "teacherName": "Maria Garcia",
  "date": "2026-05-28",
  "dayOfWeek": "Jueves",
  "timeIn": "08:05:00",
  "timeOut": "13:15:00",
  "status": "Late",
  "notes": "Tráfico"
}
```

---

### Dashboard (`/api/dashboard`) — Solo DIRECTOR

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/dashboard/summary` | Resumen del día actual |
| `GET` | `/api/dashboard/purge-warning` | Aviso de purga próxima (últimos 7 días del mes) |

**Response de `/summary`:**
```json
{
  "totalRecords": 348,
  "presentToday": 12,
  "lateToday": 3,
  "absentToday": 5
}
```
> `absentToday` = total de docentes con rol TEACHER − (presentToday + lateToday). **Sábado y domingo el resumen completo devuelve todo en 0** (no se registra asistencia esos días, así que no tiene sentido calcular ausentes).

**Response de `/purge-warning`:**
```json
{ "active": true, "daysRemaining": 5, "purgeDate": "2026-07-31" }
```
> `active` es `true` cuando quedan 6 días o menos para el fin del mes calendario actual. Es un cálculo al vuelo (no persiste nada), pensado para que el frontend muestre un banner recordándole al director que verifique que todas las asistencias del mes estén registradas (usando `POST /api/attendances/for-user/{userId}` para completar las que falten) antes de que el job mensual las elimine.

---

### Reportes (`/api/reports`) — Solo DIRECTOR

Aceptan los mismos parámetros de filtro que `GET /api/attendances`.

| Método | Ruta | Tipo de respuesta |
|--------|------|-------------------|
| `GET` | `/api/reports/excel` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `GET` | `/api/reports/pdf` | `application/pdf` |

**Columnas del reporte:**
`Docente | Rol | Fecha | Día | Entrada | Salida | Estado | Notas | Foto | Firma`

`Día` es el día de la semana (Lunes..Domingo) derivado de `Fecha`. `Fecha` se formatea `dd/MM/yyyy`, `Entrada`/`Salida` solo muestran hora y minuto (`HH:mm`), y `Estado` se muestra en español (`Presente`/`Tarde`/`Ausente`). Foto y Firma se descargan desde Cloudinary y se **embeben como imágenes reales** en la celda (no como texto/URL) — la **Foto se recorta a un círculo** (centrado, esquinas transparentes, vía `Graphics2D`/`Ellipse2D`, sin dependencias nuevas), la **Firma se deja rectangular** tal cual. Si una imagen falta o no se puede descargar, la celda queda en blanco sin romper el resto del reporte. Por defecto, los registros se ordenan del más reciente al más antiguo (`order=desc`).

**Ejemplo de descarga con filtro de fecha:**
```
GET /api/reports/excel?fromDate=2026-05-28&toDate=2026-05-28
```

---

### Reportes mensuales, resumen diario y aviso de purga

> Los recordatorios de entrada/salida por correo (cada 25 min, a docentes y directores) fueron **eliminados**: consumían demasiada cuota del plan gratuito de EmailJS y ponían en riesgo el envío del reporte mensual a fin de mes. Ahora los únicos correos automáticos, además de verificación/reset, son estos dos, y van **solo a usuarios DIRECTOR**.

**Reporte mensual (`MonthlyReportService`, `service/reports/`):** corre el día 1 de cada mes a la 1:00 am (`@Scheduled(cron = "0 0 1 1 * *", zone = "America/Lima")`):
1. Genera el Excel y PDF del mes calendario **anterior** completo (mismo filtro/columnas que `/api/reports`, incluye docentes y director).
2. Sube ambos archivos a Cloudinary como recursos `raw` (carpeta `monthly-reports`) y guarda las URLs en la tabla `monthly_report_log` (una fila por mes).
3. Envía un correo con ambos links de descarga a cada usuario DIRECTOR con correo registrado.
4. **Solo si al menos un DIRECTOR recibió el correo**, elimina los registros de asistencia de ese mes (`DELETE` equivalente a `by-date` pero para todo el rango del mes).

Si el envío falla para todos los directores (ej. EmailJS caído), la fila queda marcada como no entregada y el propio job la reintenta al inicio del ciclo del mes siguiente (con las mismas URLs ya subidas, sin regenerar el reporte) antes de purgar — la purga de ese mes solo ocurre una vez que la entrega se confirma.

**Resumen diario (`DailyDigestService`, `service/reports/`):** corre de lunes a viernes a las 2:30 pm (`@Scheduled(cron = "0 30 14 * * MON-FRI", zone = "America/Lima")`, justo después de que cierra la ventana de salida) y envía a cada DIRECTOR un correo con el total de asistencias del día y el desglose de presentes/tarde/ausentes por rol (docente y director).

Ambos usan la **misma plantilla genérica de EmailJS** (`EMAILJS_TEMPLATE_ID`) que `sendVerificationCode`/`sendPasswordResetCode` — el `subject` y el HTML se arman en Java, EmailJS solo los inyecta vía `{{to_email}}`/`{{subject}}`/`{{message}}`.

---

### Usuarios (`/api/users`)

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `GET`    | `/api/users` | DIRECTOR | Lista paginada de usuarios con sus registros (filtros: `username`, `status`, `role` — `TEACHER`/`DIRECTOR`, `fromDate`, `toDate`) |
| `GET`    | `/api/users/{id}` | DIRECTOR | Detalle de un usuario con todos sus registros |
| `PATCH`  | `/api/users/{id}` | Propio usuario o DIRECTOR | Actualiza `firstName`/`lastName` (nunca `email`, no existe ese campo en el DTO). `role` solo se aplica si quien llama es DIRECTOR |
| `DELETE` | `/api/users/{id}` | Propio usuario o DIRECTOR | Un DIRECTOR puede eliminar a cualquier otro usuario. Cualquiera (TEACHER o DIRECTOR) puede eliminar **su propia cuenta**, excepto un DIRECTOR eliminándose a sí mismo (`400 SelfDeleteException`, para no dejar el sistema sin director). Al eliminar un usuario también se eliminan en cascada sus registros de asistencia (`AttendanceRepository.deleteByUserId`) para evitar un error de FK |
| `POST`   | `/api/users/{id}/signature` | Propio usuario o DIRECTOR | Subir imagen de firma (multipart) |
| `POST`   | `/api/users/{id}/fingerprint` | Propio usuario o DIRECTOR | Subir imagen de huella (multipart) |
| `POST`   | `/api/users/{id}/photo` | Propio usuario o DIRECTOR | Subir foto de perfil (multipart) |

---

## Roles y seguridad

- **`TEACHER`**: puede registrar y ver su propia asistencia. Puede subir su propia firma y huella, editar su nombre (no su correo) y eliminar su propia cuenta.
- **`DIRECTOR`**: acceso completo — lista de todos los registros, dashboard, reportes, eliminación, subida de imágenes de cualquier usuario.

El token JWT se envía en el header:
```
Authorization: Bearer eyJ...
```

- Access token: expira en **1 hora**
- Refresh token: expira en **7 días**

---

## Lógica de asistencia

**Entrada** — ventana de **7:30 am a 9:00 am**; fuera de ese rango no se puede registrar entrada en absoluto (`400`), y el docente queda sin registro del día (cuenta como ausente):

| Condición | Estado |
|-----------|--------|
| `timeIn` < 07:30 o > 09:00 | No se puede registrar (`400`) |
| `timeIn` ≤ 08:20 | `Present` |
| `timeIn` entre 08:20 y 09:00 | `Late` |
| Sin registro en el día | Cuenta como ausente en el dashboard |

**Salida** — ventana de **1:00 pm a 2:00 pm**; fuera de ese rango no se puede registrar salida (`400`). Dentro de la ventana, de 1:00 a 1:32 pm se considera a tiempo y de 1:32 a 2:00 pm es tolerancia — ninguna de las dos cambia el `status` del día, que depende únicamente de la entrada. Si nunca se registra, no cambia el estado — se muestra como "No registró salida".

Un docente solo puede tener **un registro por día**. Si intenta crear un segundo, recibe `409 Conflict`.

El **día de la semana** (Lunes..Domingo) es un campo derivado de `date` — no se persiste, se calcula al vuelo para las respuestas de la API, el filtro `dayOfWeek` y la columna `Día` de los reportes.

---

## Reportes (Excel y PDF)

Los reportes incluyen, tanto de docentes como del director:
- Nombre completo y rol (Docente / Director)
- Fecha y día de la semana, hora de entrada, hora de salida
- Estado (`Present` / `Late` / `Absent`)
- Notas
- Foto de perfil (recortada a círculo) y firma (rectangular), **embebidas como imágenes** dentro de la celda (no URLs) — orden por defecto: más recientes primero.

Los reportes se generan al vuelo con los mismos filtros de la vista del director. No hay caché de reportes — cada descarga consulta la DB en el momento, pero las imágenes de un mismo usuario se descargan una sola vez por reporte y se reutilizan en todas sus filas.

---

## Mantenimiento de la base de datos

El plan gratuito de PostgreSQL (ej. Neon, Supabase, Railway) tiene un límite de **512 MB**. Con asistencias de entrada y salida de lunes a viernes, los datos se acumulan rápido.

Desde la introducción del reporte mensual automático (ver [arriba](#reportes-mensuales-resumen-diario-y-aviso-de-purga)), la limpieza **ya no depende de que el director la haga manualmente cada día** — el mes se archiva y se purga solo. El flujo manual sigue disponible para limpiezas puntuales fuera de ciclo:

### Flujo manual (limpieza puntual):

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

### MonthlyReportLog
```
id          — Long (PK)
period      — LocalDate, único (primer día del mes reportado)
excelUrl    — String (URL Cloudinary, recurso raw)
pdfUrl      — String (URL Cloudinary, recurso raw)
generatedAt — LocalDateTime
delivered   — boolean (true una vez que al menos un DIRECTOR recibió el correo; solo entonces se purga ese mes)
```
