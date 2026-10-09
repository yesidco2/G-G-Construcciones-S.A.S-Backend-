# GYG_Backend - G&G Construcciones (Spring Boot)

Este es el backend desarrollado en Java con Spring Boot para la empresa de ingeniería civil y consultoría en SST **G&G Construcciones**.

El backend sirve de API REST para un frontend desarrollado en React.

## Tecnologías Utilizadas
- **Java 17** (o superior)
- **Spring Boot 4.0.7 / 3.x**
- **Maven**
- **PostgreSQL** (Entorno de desarrollo/producción)
- **H2 Database** (Entorno de pruebas/tests de integración)
- **SpringDoc OpenAPI (Swagger UI)**
- **JBcrypt** (Seguridad y hashing de códigos de acceso de proyectos)

---

## Requisitos Previos

1. **PostgreSQL** instalado y corriendo localmente.
2. Crear una base de datos en PostgreSQL llamada:
   ```sql
   CREATE DATABASE gygdatabase;
   ```
3. Configura tus credenciales locales en el archivo `src/main/resources/application.properties` (este archivo no debe subirse a GitHub ni compartirse públicamente).
   - **Usuario**: ejemplo `postgres`
   - **Contraseña**: usa tu contraseña local privada, nunca la subas al repositorio
   - **Puerto**: `5432`

> Nunca compartas ni publiques la contraseña real de tu base de datos. Si necesitas compartir la configuración, usa un archivo de ejemplo como `application.properties.example` sin secretos reales.

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
| **GET** | `/api/proyectos/seguimiento/{codigo}` | Busca un proyecto por su código de acceso único (hasheado). Si es válido, retorna el avance y fotos del proyecto. |
| **POST** | `/api/contacto` | Recibe y valida `{ nombre, empresa, correo, telefono, tipoServicio, mensaje }`. Guarda la solicitud en BD y retorna 201. |
| **POST** | `/api/chat` | Recibe `{ pregunta }`, procesa una respuesta automática basada en palabras clave de normativas SST, la registra en base de datos y la retorna. |
| **GET** | `/api/riesgos/labores` | Retorna la lista de actividades/labores críticas de seguridad laboral. |
| **GET** | `/api/riesgos/{laborId}` | Retorna la matriz de riesgos detallada y los EPP sugeridos para una labor en particular. |
| **GET** | `/api/politicas-ambientales` | Devuelve la lista de políticas y compromisos de mitigación ambiental de la empresa. |

### Prueba de Seguimiento de Proyecto
El proyecto viene precargado (`DataLoader`) con datos de prueba si detecta las tablas vacías.
- **Código de acceso piloto para pruebas (texto plano):** `GYG-CIVIL-001`
- Para probar el endpoint de seguimiento, consume: `GET /api/proyectos/seguimiento/GYG-CIVIL-001`
- Esto buscará de forma segura (usando comparación hash por BCrypt) y retornará el avance y las fotos de seguimiento de la obra vial Corredor Sur.

---

## Ejecución de Pruebas Unitarias y de Integración

Los tests de integración del controlador utilizan una configuración especial con base de datos H2 en memoria (`src/test/resources/application-test.properties`). No requieren que tengas PostgreSQL corriendo para compilar o probar.

Para ejecutar los tests, corre:
```powershell
.\mvnw.cmd test
```
