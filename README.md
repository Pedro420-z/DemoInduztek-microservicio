# DemoInduztek – Microservicios

Backend de **InduztekApp**, la aplicación Android de **Induztek Ingeniería SpA** para registrar protocolos de prueba y mantención de equipos eléctricos. Son cuatro microservicios Spring Boot que comparten un proyecto Maven y se conectan a MySQL.

Proyecto de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC), Evaluación Parcial 3. Todos los datos son ficticios.

Repositorio de la app: [InduztekApp](https://github.com/eldeldeldede/InduztekApp)

## Servicios

| Servicio | Puerto | Base de datos | Qué hace |
| --- | --- | --- | --- |
| `usuarios-service` | 8081 | `usuarios_db` | Roles, usuarios, inicio de sesión, cambio y recuperación de contraseña |
| `equipos-service` | 8082 | `equipos_db` | Regiones, comunas, tipos de equipo, instalaciones y equipos |
| `protocolos-service` | 8083 | `protocolos_db` | Protocolos, pruebas y mediciones, instrumentos y calibraciones, visitas, actas y cierre de visita |
| `reportes-service` | 8084 | `reportes_db` | Consultas que combinan los otros tres: historial de equipos, actas y protocolos atrasados |

`reportes-service` no guarda datos propios: consulta a los otros servicios por HTTP y arma las respuestas.

## Requisitos

- Java 21
- MySQL 8 (en Laragon: usuario `root`, **sin contraseña**, puerto 3306)
- No hace falta instalar Maven: el proyecto incluye `mvnw`

Las bases de datos se crean solas al arrancar (`createDatabaseIfNotExist=true`) y las tablas se generan con JPA (`ddl-auto=update`).

## Cómo ejecutarlos

1. Encender MySQL en Laragon.
2. Desde la raíz del repositorio, abrir cada servicio en su propia terminal:

```
.\mvnw -pl usuarios-service spring-boot:run
.\mvnw -pl equipos-service spring-boot:run
.\mvnw -pl protocolos-service spring-boot:run
.\mvnw -pl reportes-service spring-boot:run
```

También puede usarse `iniciar-servicios.bat` (doble clic), que abre los cuatro, o el Spring Boot Dashboard de VS Code.

3. Esperar el mensaje `Started ...Application` en cada ventana.

Para comprobar que responden: `http://localhost:8081/api/roles` debe mostrar los cuatro roles en JSON.

Los servicios `protocolos` y `reportes` consultan a los demás, así que conviene levantar primero `usuarios` y `equipos`.

## Usuario inicial

Al primer arranque `usuarios-service` crea los cuatro roles y un único **Jefe Técnico**:

| RUT | Correo | Contraseña temporal |
| --- | --- | --- |
| 11111111-1 | jefe@induztek.cl | `Induztek2026` |

El Jefe debe cambiar la contraseña en su primer ingreso. Los demás usuarios los crea el Jefe: elige el rol, pero **no** la contraseña. El sistema genera una temporal y la envía al correo del nuevo usuario. Por ahora el envío se simula: el mensaje aparece en la consola de `usuarios-service` como `[CORREO SIMULADO]`.

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
- `/instalaciones` y `/equipos`: listar, obtener por id, crear (`POST`) y actualizar (`PUT`)

**protocolos-service** (`/api`)
- `/protocolos`: listar, obtener, crear, actualizar
- `POST /protocolos/{id}/pruebas`, `DELETE /protocolos/{id}/pruebas/{pruebaId}`
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

No hay tokens. Las operaciones que requieren permisos reciben la cabecera `X-Usuario-Id` con el id del usuario, y el servicio valida su rol y que la cuenta esté activa. En `reportes-service` un cliente solo ve datos de su propia empresa y solo protocolos formalizados.

Los errores se responden en JSON con un campo `message` en español (por ejemplo `{"status":400,"message":"RUT ya registrado"}`).

## Reglas de negocio implementadas

Contraseñas temporales de primer ingreso, RUT válido, correo único, estados del protocolo (borrador, registrado en terreno, observado, formalizado, anulado), validación de instrumentos con calibración vigente, numeración única de protocolos (`PT-AAAA-NNNN`), cálculo de resultados por prueba, cierre de visita con acta y detección de protocolos atrasados por días hábiles.

## Estructura

```
DemoInduztek-microservicio/
├── pom.xml                 Proyecto Maven padre (Spring Boot 4.1.2, Java 21)
├── mvnw / mvnw.cmd         Maven Wrapper
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
| usuarios-service | 99 % | 94 % |
| equipos-service | 99 % | 98 % |
| protocolos-service | 99 % | 97 % |
| reportes-service | 99 % | 100 % |

## Pendiente

- Variación mayor al 25 % entre mediciones (RN-09).
- Envío real de correos por SMTP (hoy simulado en consola).
- Autenticación con tokens.

## Equipo

- Gonzalo Bahamondes ([@eldeldeldede](https://github.com/eldeldeldede))
- Pedro Cid ([@Pedro420-z](https://github.com/Pedro420-z))
