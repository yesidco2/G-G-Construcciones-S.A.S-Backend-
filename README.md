# GYG_Backend - G&G Construcciones (Spring Boot)

Este es el backend desarrollado en Java con Spring Boot para la empresa de ingeniería civil y consultoría en SST **G&G Construcciones**.

El backend sirve de API REST para un frontend desarrollado en React.

## Tecnologías Utilizadas
- **Java 17**
- **Spring Boot 3.3.5**
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
   - `APP_CORS_ALLOWED_ORIGINS` (opcional): orígenes separados por comas. Por defecto permite `http://localhost:5173,http://localhost:3000`.

   Para producción en GitHub Pages, agrega el origen del sitio, por ejemplo `https://<usuario>.github.io`, a `APP_CORS_ALLOWED_ORIGINS`.

> No publiques credenciales de base de datos. `application.properties.example` documenta la configuración sin incluir secretos.

---

## Instrucciones para Ejecución

### En Windows:
Ejecuta el siguiente comando en la terminal desde la raíz del proyecto backend (`G&G_Backend/`):
```powershell
.\mvnw.cmd spring-boot:run
```

### En Linux/macOS:
Concede permisos de ejecución al script `mvnw` y ejecútalo:
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
| **GET** | `/api/obras/seguimiento/{codigo}` | Busca una obra por su código de acceso único (comparado en texto plano). Si es válido, retorna el avance y fotos del proyecto. |
| **POST** | `/api/contacto` | Recibe y valida `{ nombre, empresa, correo, telefono, tipoServicio, mensaje }`. Guarda la solicitud en BD y retorna 201. |
| **POST** | `/api/chat` | Recibe `{ pregunta }`, procesa una respuesta automática basada en palabras clave de normativas SST, la registra en base de datos y la retorna. |
| **GET** | `/api/riesgos/labores` | Retorna la lista de actividades/labores críticas de seguridad laboral. |
| **GET** | `/api/riesgos/{laborId}` | Retorna la matriz de riesgos detallada y los EPP sugeridos para una labor en particular. |
| **GET** | `/api/politicas-ambientales` | Devuelve la lista de políticas y compromisos de mitigación ambiental de la empresa. |

### Prueba de Seguimiento de Proyecto
El `DataLoader` precarga obras y fotos si la tabla de obras está vacía. Códigos disponibles:

| Obra | Código | Avance |
|---|---|---:|
| Viaducto Metropolitano | `GG-2026-00125` | 74% |
| Centro Logístico Industrial | `GG-2026-00218` | 58% |
| Subestación Energía Norte | `GG-2026-00307` | 100% |
| Programa SST Corredor Sur | `GYG-CIVIL-001` | 40% |

Ejemplo: `GET /api/obras/seguimiento/GYG-CIVIL-001`. El código de seguimiento se compara en texto plano.

---

## Ejecución de Pruebas Unitarias y de Integración

Los tests de integración del controlador utilizan una configuración especial con base de datos H2 en memoria (`src/test/resources/application-test.properties`). No requieren que tengas PostgreSQL corriendo para compilar o probar.

Para ejecutar los tests, corre:
```powershell
.\mvnw.cmd test
```
