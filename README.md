# DemoInduztek – Microservicios

Backend de **InduztekApp**, la aplicación Android de **Induztek Ingeniería SpA** para registrar protocolos de prueba y mantención de equipos eléctricos. Son cuatro microservicios Spring Boot, un servidor Eureka y un API Gateway que comparten un proyecto Maven. Los microservicios se conectan a MySQL.

Proyecto de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC), Evaluación Parcial 3. Todos los datos son ficticios.

Repositorio de la app: [InduztekApp](https://github.com/eldeldeldede/InduztekApp)

## Servicios

| Servicio | Puerto | Base de datos | Qué hace |
| --- | --- | --- | --- |
| `eureka-server` | 8761 | — | Registro y descubrimiento de los microservicios |
| `api-gateway` | 8080 | — | Punto único de entrada: enruta `/api/...` al servicio correspondiente |
| `usuarios-service` | 8081 | `usuarios_db` | Roles, usuarios, inicio de sesión, cambio y recuperación de contraseña |
| `equipos-service` | 8082 | `equipos_db` | Regiones, comunas, tipos de equipo, instalaciones y equipos |
| `protocolos-service` | 8083 | `protocolos_db` | Protocolos, pruebas y mediciones, instrumentos y calibraciones, visitas, actas y cierre de visita |
| `reportes-service` | 8084 | `reportes_db` | Consultas que combinan los otros tres: historial de equipos, actas y protocolos atrasados |

`reportes-service` no guarda datos propios: consulta a los otros servicios por HTTP y arma las respuestas.

La app Android habla solo con el gateway (`http://<host>:8080`). Cada microservicio se registra en Eureka con su nombre y el gateway lo encuentra ahí (`lb://usuarios-service`, etc.).

## Requisitos

- Java 21
- MySQL 8 (en Laragon: usuario `root`, **sin contraseña**, puerto 3306)
- No hace falta instalar Maven: el proyecto incluye `mvnw`

Las bases de datos se crean solas al arrancar (`createDatabaseIfNotExist=true`) y las tablas se generan con JPA (`ddl-auto=update`).

## Cómo ejecutarlos

1. Encender MySQL en Laragon.
2. Desde la raíz del repositorio, abrir cada módulo en su propia terminal y en este orden:

```
.\mvnw -pl eureka-server spring-boot:run
.\mvnw -pl usuarios-service spring-boot:run
.\mvnw -pl equipos-service spring-boot:run
.\mvnw -pl protocolos-service spring-boot:run
.\mvnw -pl reportes-service spring-boot:run
.\mvnw -pl api-gateway spring-boot:run
```

También puede usarse `iniciar-servicios.bat` (doble clic), que abre todos en ese orden.

3. Esperar el mensaje `Started ...Application` en cada ventana.

Para comprobar: `http://localhost:8761` muestra los servicios registrados y `http://localhost:8080/api/roles` debe mostrar los cuatro roles en JSON a través del gateway.

## Swagger / OpenAPI

Cada microservicio documenta sus endpoints con springdoc:

- `http://localhost:8081/swagger-ui.html` (usuarios)
- `http://localhost:8082/swagger-ui.html` (equipos)
- `http://localhost:8083/swagger-ui.html` (protocolos)
- `http://localhost:8084/swagger-ui.html` (reportes)

## Usuario inicial

Al primer arranque `usuarios-service` crea los cuatro roles y un único **Jefe Técnico**:

| RUT | Correo | Contraseña temporal |
| --- | --- | --- |
| 11111111-1 | jefe@induztek.cl | `Induztek2026` |

El Jefe debe cambiar la contraseña en su primer ingreso. Los demás usuarios los crea el Jefe: elige el rol, pero **no** la contraseña. El sistema genera una temporal y la envía al correo del nuevo usuario. El correo se envía por SMTP real (ver más abajo).

Roles: Técnico, Ingeniero, Jefe Técnico y Cliente.

## Endpoints principales

**usuarios-service** (`/api`)
- `GET /roles`
- `GET /usuarios`, `GET /usuarios/{id}`, `POST /usuarios`, `PUT /usuarios/{id}`
- `POST /usuarios/login`
- `PUT /usuarios/{id}/contrasena`, `PUT /usuarios/{id}/estado`
- `POST /usuarios/recuperacion`, `POST /usuarios/recuperacion/confirmar`

**equipos-service** (`/api`)
- `GET /regiones`, `GET /comunas`, `GET /tipos-equipo`
- `/instalaciones` y `/equipos`: listar, obtener por id, crear (`POST`) y actualizar (`PUT`). Actualizar un equipo exige la cabecera `X-Usuario-Id` de un Jefe Técnico; no se pueden eliminar equipos

**protocolos-service** (`/api`)
- `/protocolos`: listar, obtener, crear, actualizar
- `POST /protocolos/{id}/pruebas`, `DELETE /protocolos/{id}/pruebas/{pruebaId}` (lo único que se puede eliminar; protocolos, visitas y actas no se eliminan)
- `POST /protocolos/{id}/registrar`, `/observar`, `/formalizar`, `/anular`
- `/instrumentos` y `/instrumentos/{id}/calibraciones`
- `GET /tipos-prueba`
- `/visitas`: listar, obtener, crear, `POST /visitas/{id}/recepcion` y `POST /visitas/{id}/cierre`

**reportes-service** (`/api/reportes`)
- `GET /equipos?clienteId=`
- `GET /equipos/{id}/historial`
- `GET /actas?instalacionId=`
- `GET /protocolos-atrasados`

## Cómo se identifica quién hace la acción

No hay tokens. Las operaciones que requieren permisos reciben la cabecera `X-Usuario-Id` con el id del usuario, y el servicio valida su rol y que la cuenta esté activa. En `reportes-service` un cliente solo ve datos de su propia empresa y todos sus protocolos salvo borradores y anulados; los registrados en terreno y los observados aparecen como pendientes de formalización.

Los errores se responden en JSON con un campo `message` en español (por ejemplo `{"status":400,"message":"RUT ya registrado"}`).

## Reglas de negocio implementadas

Contraseñas temporales de primer ingreso, RUT válido, correo único, estados del protocolo (borrador, registrado en terreno, observado, formalizado, anulado), validación de instrumentos con calibración vigente, numeración única de protocolos (`PT-AAAA-NNNN`), cálculo de resultados por prueba, cierre de visita con acta y detección de protocolos atrasados por días hábiles.

## Estructura

```
DemoInduztek-microservicio/
├── pom.xml                 Proyecto Maven padre (Spring Boot 4.2.0, Spring Cloud 2026.0.0, Java 21)
├── mvnw / mvnw.cmd         Maven Wrapper
├── eureka-server/
├── api-gateway/
├── usuarios-service/
├── equipos-service/
├── protocolos-service/
└── reportes-service/
    └── src/main/java/com/induztek/<servicio>/
        ├── controller/     Endpoints REST
        ├── service/        Lógica de negocio
        ├── repository/     Acceso a la base de datos (Spring Data JPA)
        ├── model/          Entidades
        ├── dto/            Objetos de entrada y salida (records)
        ├── client/         Llamadas HTTP a otros servicios
        ├── config/         Configuración y datos iniciales
        └── util/           Utilidades
```

## Pruebas y cobertura

Cada servicio tiene pruebas con JUnit 5, Mockito y MockMvc, y se mide con JaCoCo.

```
.\mvnw verify
```

El reporte queda en `<servicio>/target/site/jacoco/index.html`.

| Servicio | Instrucciones | Ramas |
| --- | --- | --- |
| usuarios-service | 99 % | 95 % |
| equipos-service | 99 % | 99 % |
| protocolos-service | 97 % | 95 % |
| reportes-service | 99 % | 100 % |

## Pendiente

- Variación mayor al 25 % entre mediciones (RN-09).
- Autenticación con tokens.

## Equipo

- Gonzalo Bahamondes ([@eldeldeldede](https://github.com/eldeldeldede))
- Pedro Cid ([@Pedro420-z](https://github.com/Pedro420-z))

## Correo real (SMTP) y datos de ejemplo

- `usuarios-service` envía por SMTP real (Gmail) la contraseña temporal y el código de recuperación. Las credenciales no están en el repositorio: se entregan por variables de entorno antes de arrancar el servicio.

```
set MAIL_USUARIO=cuenta@gmail.com
set MAIL_CLAVE=contraseña-de-aplicación
```

La cuenta necesita verificación en 2 pasos y una "contraseña de aplicación" de Google. Si el envío falla, el correo se escribe en la consola del servicio.
- Al arrancar por primera vez se cargan 2 empresas clientes, un técnico y un ingeniero de ejemplo (contraseña inicial `Induztek2026`, deben cambiarla al ingresar), 4 instrumentos, 4 equipos y 2 actas con protocolos. Se puede desactivar con `demo.cargar=false` en `protocolos-service`.
- Orden de arranque: Eureka, usuarios, equipos, protocolos, reportes y gateway. Si protocolos arranca antes, reintenta hasta 1 minuto.

## Reportes de acta del cliente

- `POST /api/visitas/{id}/reportes` (cliente): crea un reporte; si indica un protocolo formalizado, este pasa a OBSERVADO.
- `GET /api/reportes-acta` y `GET /api/reportes-acta/{id}`: el cliente ve los suyos; el personal ve todos.
- `PUT /api/reportes-acta/{id}` (ingeniero o jefe técnico): estado EN_REVISION, RESUELTO o RECHAZADO con respuesta escrita.