# GYG_Backend - G&G Construcciones (Spring Boot)

Este es el backend desarrollado en Java con Spring Boot para la empresa de ingeniería civil y consultoría en SST **G&G Construcciones**.

El backend sirve de API REST para un frontend desarrollado en React.

## Tecnologías Utilizadas
- **Java 17**
- **Spring Boot 3.5.16**
- **Maven**
- **PostgreSQL** (Entorno de desarrollo/producción)
- **H2 Database** (Entorno de pruebas/tests de integración)
- **SpringDoc OpenAPI (Swagger UI)**

---

## Requisitos Previos

1. **PostgreSQL** instalado y corriendo localmente.
2. Crear una base de datos en PostgreSQL llamada:
   ```sql
   CREATE DATABASE gygdatabase;
   ```
3. Configura estas variables de entorno para conectar la base de datos:
   - `DB_URL`: URL JDBC de PostgreSQL, por ejemplo `jdbc:postgresql://localhost:5432/gygdatabase`
   - `DB_USER`: usuario de PostgreSQL
   - `DB_PASSWORD`: contraseña de PostgreSQL
   - `APP_CORS_ALLOWED_ORIGINS`: orígenes de frontend separados por comas; en producción no se permiten orígenes locales por defecto.
   - `APP_TRACKING_CODE_PEPPER`: valor aleatorio de al menos 32 bytes, necesario para generar y verificar códigos de seguimiento. Guárdalo en el gestor de secretos; no lo compartas ni lo cambies después de migrar la base de datos.
   - `APP_TRUSTED_PROXIES`: expresión regular de las direcciones de los proxies inversos que realmente controlas. Spring usa `RemoteIpValve` y nunca se toma directamente la primera entrada de `X-Forwarded-For`. Por defecto no se confía en ningún proxy.
   - `APP_CAPTCHA_ENABLED`, `APP_CAPTCHA_PROVIDER` y `APP_CAPTCHA_SECRET`: configuración de CAPTCHA para los formularios; la clave secreta de Turnstile es obligatoria siempre que CAPTCHA esté habilitado.
   - `FIREBASE_WEB_API_KEY`: clave API usada por el backend para solicitar a Firebase el correo de restablecimiento. Si la clave `VITE_FIREBASE_API_KEY` del frontend está restringida por HTTP referrer, crea una clave aparte para el backend sin esa restricción de aplicación y restríngela a la Identity Toolkit API en Google Cloud.
   - `APP_RATE_LIMIT_PASSWORD_RESET_PER_HOUR`: máximo de solicitudes de restablecimiento por IP por hora (5 por defecto).
   - `APP_MAX_REQUEST_BODY_SIZE`: máximo para cuerpos HTTP no multipart (`1MB` por defecto).
   - `APP_MULTIPART_MAX_FILE_SIZE` y `APP_MULTIPART_MAX_REQUEST_SIZE`: límites de subida multipart (`5MB` por archivo y `25MB` por petición, por defecto).
   - `APP_PRIVACY_POLICY_VERSION`, `APP_CHAT_LOG_RETENTION_MONTHS` y `APP_CHAT_LOG_RETENTION_CRON`: versión de consentimiento y política de retención de consultas del chat.
   - `SPRING_PROFILES_ACTIVE=dev` solo para desarrollo local; añade orígenes localhost y carga datos demostrativos.

   Para producción en GitHub Pages, agrega el origen del sitio, por ejemplo `https://<usuario>.github.io`, a `APP_CORS_ALLOWED_ORIGINS`.

4. Para ejecutar con Firebase habilitado, descarga el archivo JSON de credenciales de la cuenta de servicio desde Firebase Console. No lo agregues al repositorio. La ruta del archivo y el ID del proyecto deben estar configurados en el entorno de ejecución.

> No publiques credenciales de base de datos. `application.properties.example` documenta la configuración sin incluir secretos.
> Copia `.env.example` como `.env` para preparar variables localmente y completa valores solo en ese archivo ignorado por Git o en el gestor de secretos del despliegue. En producción, Hibernate usa `ddl-auto=validate`; Flyway crea el esquema en instalaciones nuevas y toma como baseline version 1 los esquemas existentes no vacíos, sin recrear sus tablas ni borrar sus datos.

---

## Instrucciones para Ejecución

### En Windows:
Desde PowerShell, ejecuta el script de desarrollo en la raíz del proyecto:
```powershell
.\run-dev.ps1
```

Antes de usarlo, revisa `run-dev.ps1` y ajusta `FIREBASE_PROJECT_ID`, `FIREBASE_CREDENTIALS_PATH`, `APP_ADMIN_EMAILS`, `DB_URL` y `DB_USER` a tu entorno. El script comprueba que exista el archivo de credenciales de Firebase y solicita la contraseña de PostgreSQL al ejecutarse; no la guarda en el archivo. Si `APP_CAPTCHA_SECRET` no está definida, solicita de forma oculta la Secret key de Turnstile. Si `FIREBASE_WEB_API_KEY` no está definida, la solicita opcionalmente. Si la clave web del frontend tiene una restricción de HTTP referrer, usa una clave API separada para el backend, sin esa restricción de aplicación y limitada a Identity Toolkit API. La base de datos indicada en `DB_URL` debe existir.
Activa el perfil `dev` en la terminal antes de iniciarlo para habilitar los datos de demostración y los orígenes localhost:
```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

Como alternativa, puedes iniciar Spring Boot directamente con el wrapper Maven, siempre que hayas configurado las variables de entorno requeridas (`DB_URL`, `DB_USER`, `DB_PASSWORD` y las de Firebase cuando corresponda):
```powershell
.\mvnw.cmd spring-boot:run
```

### En Linux/macOS:
Configura las variables de entorno requeridas (`DB_URL`, `DB_USER`, `DB_PASSWORD` y las de Firebase cuando corresponda), concede permisos de ejecución al script `mvnw` y ejecútalo desde la raíz del proyecto:
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

El servidor web arrancará por defecto en el puerto **`8080`**: `http://localhost:8080`.

---

## Documentación de la API (Swagger UI)

Una vez el servidor esté corriendo, puedes visualizar e interactuar con toda la API REST y probar los endpoints desde el navegador web ingresando a:
- [Swagger UI - G&G API](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI Spec JSON](http://localhost:8080/v3/api-docs)

---

## Conexión con el Frontend (React)

Para que el frontend consuma este backend, configura las variables de entorno de tu aplicación React (generalmente en un archivo `.env` en la raíz de tu frontend React):
```properties
VITE_API_URL=http://localhost:8080
```
El backend cuenta con CORS configurado para permitir peticiones desde `http://localhost:5173` (el puerto por defecto de Vite/React).

---

## Estructura de Endpoints de la API

| Método | Endpoint | Descripción |
|---|---|---|
| **GET** | `/api/institucional` | Devuelve la misión, visión y valores de la empresa. |
| **GET** | `/api/servicios` | Retorna los servicios ofrecidos organizados por bloque (`CIVIL` o `SST`). |
| **GET** | `/api/datos-legales` | Devuelve el NIT y la dirección física de la empresa. |
| **GET** | `/api/proyectos` | Retorna el portafolio de proyectos. Opcionalmente filtrable por query param `?categoria=CIVIL` o `?categoria=SST`. |
| **POST** | `/api/obras/seguimiento` | Recibe `{ "codigo": "..." }` y retorna solo el nombre, avance, hitos y fotos. Los códigos nuevos son aleatorios de alta entropía; la base de datos almacena únicamente su HMAC-SHA256. |
| **POST** | `/api/contacto` | Recibe `{ nombre, empresa, correo, telefono, tipoServicio, mensaje, aceptaPolitica }`. El consentimiento es obligatorio; cuando CAPTCHA está activo, requiere `X-Captcha-Token`. Guarda la solicitud y retorna 201. |
| **POST** | `/api/newsletter` | Registra una suscripción con `{ correo, aceptaPolitica }`; usa `X-Captcha-Token` cuando CAPTCHA está activo. |
| **POST** | `/api/auth/password-reset` | Solicita a Firebase un correo de restablecimiento; público, protegido por `X-Captcha-Token` y limitado por IP. Responde de forma genérica para no revelar si el correo está autorizado o registrado. |
| **POST** | `/api/chat` | Recibe `{ pregunta }`, procesa una respuesta automática basada en palabras clave de normativas SST, la registra en base de datos y la retorna. |
| **GET** | `/api/admin/consultas-chat?page=0&size=20` | Lista registros de chat paginados; requiere usuario administrador con sesión verificada. |
| **GET** | `/api/riesgos/labores` | Retorna la lista de actividades/labores críticas de seguridad laboral. |
| **GET** | `/api/riesgos/{laborId}` | Retorna la matriz de riesgos detallada y los EPP sugeridos para una labor en particular. |
| **GET** | `/api/politicas-ambientales` | Devuelve la lista de políticas y compromisos de mitigación ambiental de la empresa. |

### Consentimiento y retención de datos

Las solicitudes de contacto y las suscripciones al newsletter deben incluir `aceptaPolitica: true`. La versión vigente se configura con `APP_PRIVACY_POLICY_VERSION` y se guarda junto con la fecha del consentimiento. La verificación CAPTCHA de contacto y newsletter se controla con `APP_CAPTCHA_ENABLED`, `APP_CAPTCHA_PROVIDER` y `APP_CAPTCHA_SECRET`; si está habilitada, el token del proveedor se envía en el encabezado `X-Captcha-Token`.

Los registros del chat se eliminan automáticamente según `APP_CHAT_LOG_RETENTION_MONTHS` (12 meses por defecto), en el horario definido por `APP_CHAT_LOG_RETENTION_CRON`.

### Prueba de Seguimiento de Proyecto
En el perfil `dev`, `DataLoader` precarga obras y fotos si la tabla de obras está vacía. Los códigos de demostración disponibles son:

| Obra | Código | Avance |
|---|---|---:|
| Viaducto Metropolitano | `GG-2026-00125` | 74% |
| Centro Logístico Industrial | `GG-2026-00218` | 58% |
| Subestación Energía Norte | `GG-2026-00307` | 100% |
| Programa SST Corredor Sur | `GYG-CIVIL-001` | 40% |

Ejemplo: `POST /api/obras/seguimiento` con el cuerpo `{"codigo":"GYG-CIVIL-001"}`. Al migrar los códigos existentes, Flyway requiere `APP_TRACKING_CODE_PEPPER` y reemplaza los valores en texto plano por HMAC.

---

## Ejecución de Pruebas Unitarias y de Integración

Los tests de integración del controlador utilizan una configuración especial con base de datos H2 en memoria (`src/test/resources/application-test.properties`). No requieren que tengas PostgreSQL corriendo para compilar o probar.

Para ejecutar los tests, corre:
```powershell
.\mvnw.cmd test
```
