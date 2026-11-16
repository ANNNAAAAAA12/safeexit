# SafeExit CO — explicación funcional y técnica

Este documento explica qué hace SafeExit CO, cómo está construido, qué tecnologías usa, cómo se organiza el código y qué está realmente activo en la versión publicada. Está pensado como material de estudio y apoyo para una exposición académica.

## 1. Resumen del proyecto

SafeExit CO es una aplicación web para administrar residentes y vehículos de un conjunto residencial y ayudar a prevenir salidas con documentos vencidos. Se registran las fechas de vencimiento del SOAT y de la revisión técnico-mecánica (RTM); al procesar una salida, el sistema compara esas fechas con la fecha actual y genera una alerta cuando un documento está vencido o próximo a vencer.

La aplicación tiene un frontend para administradores, residentes y auditores, una API REST, una base de datos relacional y módulos desacoplados para OCR, verificación documental y WhatsApp.

### Estado de la publicación

- Frontend público: [safeexit-frontend.onrender.com](https://safeexit-frontend.onrender.com)
- Backend público: [safeexit-backend.onrender.com](https://safeexit-backend.onrender.com)
- Salud del backend: [safeexit-backend.onrender.com/actuator/health](https://safeexit-backend.onrender.com/actuator/health)
- La cuenta de administrador ya fue probada por la usuaria.
- En Render, WhatsApp está en modo `mock`: la aplicación registra el intento, pero **no envía mensajes reales**.
- La cámara de administrador es una simulación. No hay una cámara física conectada al sistema.

## 2. Tecnologías

| Componente | Tecnología | Uso |
|---|---|---|
| Frontend | React 19, TypeScript 7 y Vite 8 | Aplicación web de una sola página (SPA), interfaz y compilación |
| OCR en el navegador | Tesseract.js 7 | Lectura local de una placa desde la cámara del dispositivo del residente |
| Backend | Java 25 y Spring Boot 3.5.16 | API REST, reglas de negocio y seguridad |
| Persistencia | PostgreSQL, Spring Data JPA e Hibernate | Guardado y consulta de los datos relacionales |
| Seguridad | Spring Security, JWT y BCrypt | Sesiones sin estado, autorización por rol y almacenamiento cifrado de contraseñas |
| Validación y estado | Spring Validation y Actuator | Validación de solicitudes y comprobación de salud |
| Contenedores | Docker y Docker Compose | Ejecución local y empaquetado del backend |
| Alojamiento | Render | Frontend estático, backend Docker y PostgreSQL |

Las versiones y dependencias del backend están en [pom.xml](./pom.xml); las del frontend, en [frontend/package.json](./frontend/package.json).

## 3. Arquitectura

El diseño separa la presentación, la API, las reglas de negocio, la persistencia y las integraciones:

```text
Navegador
  React / TypeScript
       │ HTTPS + JSON + JWT
       ▼
API REST — Spring Boot
  Controladores → Servicios → Repositorios JPA
                         │              │
                         │              ▼
                         │          PostgreSQL
                         │
                         ├── OCR: simulación o Plate Recognizer
                         ├── Documentos: verificación simulada
                         └── WhatsApp: simulación o Twilio
```

El frontend envía solicitudes a la API mediante [frontend/src/api.ts](./frontend/src/api.ts). Los controladores reciben las solicitudes HTTP y los DTO validan los datos de entrada. Los servicios ejecutan las reglas de negocio y usan repositorios para consultar o persistir entidades.

Las integraciones se definen detrás de interfaces. Así, una implementación simulada puede usarse en pruebas o demostraciones sin consumir servicios externos, mientras que los clientes reales permanecen separados.

## 4. Estructura del repositorio

### Frontend

La carpeta [frontend/src/](./frontend/src/) contiene la aplicación React:

- [App.tsx](./frontend/src/App.tsx): inicio de sesión, manejo de sesión JWT y navegación condicionada por el rol.
- [pages.tsx](./frontend/src/pages.tsx): panel, vehículos, usuarios, alertas, auditoría y cuenta del residente.
- [CameraPage.tsx](./frontend/src/CameraPage.tsx): simulador de salida para el administrador.
- [api.ts](./frontend/src/api.ts): cliente HTTP común.
- [types.ts](./frontend/src/types.ts): tipos compartidos entre pantallas.
- Los archivos CSS contienen los estilos de la interfaz.

### Backend

El código Java se encuentra en [src/main/java/safeexit_backend/](./src/main/java/safeexit_backend/):

- `controladores`: rutas REST y traducción de solicitudes/respuestas.
- `servicios`: reglas de negocio, procesamiento de salidas, alertas, usuarios, métricas y auditoría.
- `servicios/integraciones`: interfaces y adaptadores para OCR y WhatsApp.
- `repositorios`: consultas de Spring Data JPA.
- `modelos`: entidades persistidas.
- `dto`: contratos de entrada y salida de la API.
- `seguridad`: filtros JWT, autorización de cámara y carga del usuario autenticado.
- `config`: configuración de Spring, base de datos y creación inicial de datos.

### Configuración y despliegue

- [src/main/resources/application.properties](./src/main/resources/application.properties): propiedades del backend y sus valores por defecto.
- [Dockerfile](./Dockerfile): imagen de varias etapas para compilar y ejecutar el backend.
- [frontend/Dockerfile](./frontend/Dockerfile) y [frontend/nginx.conf](./frontend/nginx.conf): empaquetado local del frontend y proxy hacia la API.
- [docker-compose.yml](./docker-compose.yml): frontend, API y PostgreSQL para desarrollo local.
- [render.yaml](./render.yaml): recursos y variables de configuración de Render.
- [HELP.md](./HELP.md): instrucciones para ejecutar, probar y desplegar el sistema.
- [.env.example](./.env.example): plantilla de variables locales. El archivo `.env` con valores reales no debe subirse a GitHub.

## 5. Modelo de datos

El backend tiene ocho entidades principales:

1. **Conjunto residencial (`ConjuntoResidencial`)**: nombre, dirección y NIT.
2. **Usuario (`Usuario`)**: nombre, correo, contraseña cifrada, teléfono, estado, rol y conjunto.
3. **Vehículo (`Vehiculo`)**: placa, tipo, marca, modelo y propietario.
4. **Vencimiento de documentos (`VencimientoDocumentos`)**: fechas de SOAT y RTM; relación uno a uno con el vehículo.
5. **Lectura de salida (`LecturaSalida`)**: placa procesada, fecha, vehículo relacionado si existe e imagen cuando fue recibida por la API.
6. **Evento de alerta (`EventoAlerta`)**: motivo, criticidad y lectura que lo originó.
7. **Registro de WhatsApp (`RegistroWhatsapp`)**: destinatario, contenido, estado de envío e identificador del proveedor cuando existe.
8. **Parámetros del sistema (`ParametrosSistema`)**: días de anticipación configurados para un conjunto.

Hibernate crea o actualiza el esquema según estas entidades (`spring.jpa.hibernate.ddl-auto=update`). El mapeo representa las ocho tablas de la especificación, aunque algunas restricciones de nulabilidad del código no coinciden exactamente con el diccionario del PDF; por ejemplo, ciertos campos de teléfono, tipo de vehículo y relaciones no están marcados como obligatorios en las anotaciones JPA.

## 6. Roles y funcionalidades

### Administrador — `ROLE_ADMIN`

- Inicia sesión y accede al panel de métricas.
- Consulta usuarios y crea cuentas de residentes o auditores.
- Activa o desactiva cuentas.
- Edita datos del conjunto y configura los días de anticipación.
- Consulta alertas y registros de auditoría.
- Simula una lectura ingresando manualmente una placa en **Cámara**.

La cuenta inicial se configura mediante variables de entorno en Render. El servicio de inicialización la crea si aún no existe. No hay una pantalla administrativa para cambiar la plantilla de WhatsApp; esa configuración se maneja mediante variables del backend.

### Residente — `ROLE_RESIDENT`

- Consulta sus vehículos y el estado de SOAT/RTM.
- Registra vehículos y actualiza las fechas de sus documentos.
- Actualiza su teléfono de WhatsApp.
- Consulta su historial de alertas.
- Puede activar la cámara del dispositivo para intentar leer una placa con OCR local en el navegador y corregir el resultado antes de continuar.

### Auditor — `ROLE_AUDITOR`

- Consulta lecturas de salida, notificaciones y, si existe, la imagen asociada a una lectura.
- La API también ofrece una ruta para consultar el estado de la caché.
- La pantalla de auditoría presenta las lecturas y notificaciones; el porcentaje de entrega se calcula en las métricas del administrador, no en la vista del auditor.

Los roles se validan en [SecurityConfig.java](./src/main/java/safeexit_backend/config/SecurityConfig.java). El usuario autenticado obtiene el rol desde su JWT.

## 7. Flujo de procesamiento de una salida

1. Un residente registra un vehículo y las fechas de SOAT y RTM.
2. Se recibe una placa simulada o una imagen en el endpoint de procesamiento.
3. El sistema elimina espacios, convierte la placa a mayúsculas y verifica formatos como `ABC123` o `ABC12D`.
4. Busca el vehículo y sus documentos. La caché local puede conservar hasta 1.000 entradas por 60 segundos.
5. Compara los vencimientos con la fecha actual y los días configurados; el valor predeterminado es cinco días.
6. Guarda la lectura. Si no hace falta alerta, no crea una notificación; si hay vencimiento o proximidad, registra un evento de alerta y un intento de WhatsApp.
7. Según el modo configurado, el intento se simula o se envía al proveedor.

El servicio central de ese flujo está en [ProcesamientoSalidaService.java](./src/main/java/safeexit_backend/servicios/ProcesamientoSalidaService.java) y la consulta de documentos y persistencia en [SalidaPersistenceService.java](./src/main/java/safeexit_backend/servicios/SalidaPersistenceService.java).

Las imágenes aceptadas por el endpoint deben ser JPEG o PNG y no superar 5 MB. La ruta de cámara está protegida con una clave enviada mediante `X-Camera-Key`.

## 8. Endpoints REST principales

Todas las rutas usan el prefijo `/api/v1`.

| Método y ruta | Función y acceso |
|---|---|
| `POST /auth/login` | Autentica y devuelve un JWT; público |
| `GET /actuator/health` | Comprueba la salud del backend; público |
| `GET /vehiculos/mis-vehiculos` | Lista los vehículos del residente |
| `POST /vehiculos` | Registra un vehículo |
| `PATCH /vehiculos/{id}/documentos` | Actualiza documentos del propietario |
| `PATCH /usuarios/me/telefono-whatsapp` | Actualiza el teléfono del usuario |
| `GET /alertas/historial` | Consulta alertas propias o del conjunto según el rol |
| `POST /notificaciones/whatsapp/prueba` | Registra/envía un mensaje de prueba según el modo |
| `POST /lecturas/procesar-salida` | Recibe una imagen multipart y procesa una salida; requiere clave de cámara |
| `/admin/usuarios`, `/admin/conjuntos`, `/admin/metricas` | Gestión administrativa, parámetros y métricas |
| `POST /admin/demo/lecturas` | Procesa una placa escrita en el simulador |
| `/auditoria/lecturas`, `/auditoria/notificaciones`, `/auditoria/cache` | Funciones de auditoría para administrador y auditor |
| `GET /auditoria/lecturas/{id}/imagen` | Descarga la imagen de una lectura si está guardada |
| `POST /webhooks/twilio/whatsapp` | Recibe actualizaciones del proveedor; se usa con Twilio real |

Los controladores están en [src/main/java/safeexit_backend/controladores/](./src/main/java/safeexit_backend/controladores/). La lista completa de permisos también está documentada en [HELP.md](./HELP.md).

## 9. OCR, verificación documental y WhatsApp

### OCR

- La pantalla del administrador es un simulador que recibe una placa escrita; no toma video ni está conectada a una cámara del edificio.
- La pantalla del residente puede usar la cámara del navegador y Tesseract.js localmente. La fotografía no se envía a la API.
- Existe un cliente para Plate Recognizer que procesa imágenes recibidas por el backend, pero solo se activa con el modo real y el token correspondiente. En la configuración pública actual, el OCR backend sigue en modo mock.

### SOAT y RTM

La aplicación compara las fechas guardadas en su base de datos. El servicio implementado es simulado y no consulta directamente el RUNT o SIMIT. Esto coincide con la decisión de alcance descrita en la especificación: evitar APIs gubernamentales de pago y usar fechas administradas localmente.

### WhatsApp

Hay dos implementaciones detrás de la interfaz `WhatsAppGateway`:

- `MockWhatsAppGateway`: registra el intento en SafeExit, pero no contacta a nadie.
- `TwilioWhatsAppGateway`: puede comunicarse con Twilio cuando se habilita el modo real y se proporcionan credenciales, remitente y `ContentSid`.

Render tiene `SAFEEXIT_WHATSAPP_MODE=mock` y `SAFEEXIT_INTEGRATIONS_MODE=mock`. Por eso, un estado como `ENVIADO` en la demo significa que se procesó el intento simulado; **no confirma una entrega real**.

## 10. Seguridad y configuración

- El login valida correo y contraseña; el backend guarda contraseñas con BCrypt y devuelve un JWT.
- La API es sin estado: el frontend adjunta `Authorization: Bearer ...` a las llamadas autenticadas.
- Los endpoints están restringidos por rol. La cámara utiliza `X-Camera-Key`.
- CORS permite el origen del frontend público.
- Las claves privadas se configuran mediante variables de entorno; no se deben incluir en el repositorio ni en este documento.
- JWT tiene vencimiento configurable, de 24 horas por defecto.

La configuración está en [application.properties](./src/main/resources/application.properties) y [render.yaml](./render.yaml).

## 11. Ejecución y despliegue

### Local

El flujo Docker local levanta PostgreSQL, API y frontend mediante [docker-compose.yml](./docker-compose.yml). Se completa una copia local de `.env.example` en `.env` y se inicia con Docker Compose. Alternativamente, el frontend se puede ejecutar con Vite y usar su proxy local hacia la API.

El frontend Docker compila los archivos de React y los sirve con Nginx; Nginx reenvía `/api/` y `/actuator/` al backend. [frontend/Dockerfile](./frontend/Dockerfile) · [frontend/nginx.conf](./frontend/nginx.conf)

### Render

El [Blueprint](./render.yaml) declara tres recursos gratuitos: frontend estático, backend Docker y PostgreSQL. El backend expone el health check `/actuator/health`. El frontend y el backend están en internet, así que cerrar el computador no detiene la aplicación.

El plan gratuito puede suspender el backend tras un periodo sin tráfico, causando demora en la primera solicitud. La base gratuita también tiene límites y una fecha de expiración indicada en [HELP.md](./HELP.md); conviene revisar ese panel y hacer una copia de seguridad antes del vencimiento.

## 12. Pruebas y estado verificado

- Se ejecutaron **11 pruebas del backend** con Maven y Java 25; todas pasaron.
- Se comprobó que la API pública responde con estado de salud `UP`.
- Se comprobó que el frontend está publicado y que el formulario presenta un error al enviar credenciales ficticias.
- La usuaria confirmó que pudo entrar con la cuenta de administrador.

Estas pruebas verifican partes del backend y el despliegue, pero no equivalen a una prueba completa con cámara física ni a una entrega real de WhatsApp.

## 13. Límites conocidos frente a la especificación académica

1. **Cámara automática:** todavía no hay una cámara física conectada; la cámara administrativa es un simulador.
2. **WhatsApp:** el despliegue actual no hace envíos reales; usa el gateway mock.
3. **OCR de servidor:** el código de Plate Recognizer existe, pero no está habilitado en Render.
4. **Gestión de usuarios:** se crean y activan/desactivan cuentas; no se ofrece edición completa de todos sus datos.
5. **Auditoría:** la API expone el estado de la caché, pero la interfaz de auditoría no lo presenta; la métrica de porcentaje de entrega aparece en el panel administrativo.
6. **Configuración de plantilla WhatsApp:** no se cambia desde la interfaz; se configura externamente.
7. **Datos oficiales:** SOAT/RTM se evalúan con las fechas guardadas, no mediante una consulta oficial a RUNT/SIMIT.

Por lo tanto, es preciso presentar SafeExit como un **prototipo web funcional con integraciones simuladas en la publicación actual**, no como una instalación que ya opera una cámara de portería y envía WhatsApps reales.

## 14. Guion corto para demostrarlo

1. Iniciar sesión como administrador.
2. Crear una cuenta residente.
3. Iniciar sesión con esa cuenta y registrar un vehículo con sus fechas.
4. Volver al administrador, abrir **Cámara**, ingresar la placa y pulsar **Simular salida del vehículo**.
5. Mostrar la lectura, el estado de los documentos, la alerta y el historial.
6. Aclarar que el mensaje de WhatsApp de esta demo es simulado.

Para una demostración guiada paso a paso, consulta [HELP.md](./HELP.md).
