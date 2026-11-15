# SafeExit CO - Backend y frontend

Aplicación web y API REST para administrar residentes y vehículos, revisar fechas de SOAT/RTM y generar alertas preventivas en la salida de un conjunto residencial.

## Requisitos

- Java 25 JDK
- Docker Desktop con Docker Compose, para ejecutar la API y PostgreSQL localmente
- Node.js 22.12 o superior y npm, solo para ejecutar/desarrollar el frontend fuera de Docker
- Maven wrapper incluido (`mvnw.cmd`)

El proyecto usa Spring Boot 3.5 y PostgreSQL. Las integraciones externas se separan por interfaces: por defecto el reconocimiento de placas, la consulta de SOAT/RTM y WhatsApp usan simulaciones, así que el flujo de demostración no consume APIs ni necesita credenciales de terceros. Los vencimientos simulados no son datos oficiales ni reemplazan una consulta autorizada al RUNT.

## Ejecutar localmente con Docker

1. Copia el ejemplo de variables y ábrelo para editar:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

2. Completa todos los valores vacíos. `JWT_SECRET` y `CAMERA_API_KEY` deben ser valores aleatorios; no reutilices contraseñas ni subas `.env` a Git. La contraseña del administrador debe tener entre 12 y 72 bytes UTF-8. El teléfono debe usar el formato `+57` seguido de 10 dígitos.

   Para generar un secreto aleatorio en PowerShell:

   ```powershell
   $bytes = New-Object byte[] 32
   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $rng.GetBytes($bytes)
   [Convert]::ToBase64String($bytes)
   ```

3. Inicia la base de datos, la API y el frontend:

   ```powershell
   docker compose --env-file .env up --build -d
   docker compose logs -f safeexit-api
   ```

   Abre el frontend en `http://localhost:5173`. La API queda en `http://localhost:8081`; su comprobación de salud es `http://localhost:8081/actuator/health`. El frontend reenvía las llamadas a la API por una conexión interna de Docker.

El puerto de PostgreSQL solo es accesible entre contenedores; no se publica en el computador. El frontend y la API se enlazan únicamente a `localhost`.

PostgreSQL conserva datos en el volumen `postgres_data`. Si cambias `DB_PASSWORD` después de crear ese volumen, también debes cambiar la contraseña del rol existente en PostgreSQL; cambiar el archivo `.env` por sí solo no modifica el volumen.

## Demostración rápida

Entra al frontend con `SAFEEXIT_ADMIN_EMAIL` y `SAFEEXIT_ADMIN_PASSWORD` definidos en `.env`. Desde el panel puedes ver métricas, cambiar el período de anticipación de alertas, crear cuentas, revisar auditorías y abrir **Cámara**. Como no hay una cámara física conectada, esa pantalla simula una lectura: escribe la placa registrada y pulsa **Simular salida del vehículo**. SafeExit guarda la lectura, analiza SOAT/RTM y crea una alerta para el residente si corresponde. En modo predeterminado (`SAFEEXIT_WHATSAPP_MODE=mock`) no sale ningún mensaje real. El panel actualiza las métricas cada 10 segundos; las lecturas, alertas e intentos de WhatsApp se cuentan por separado, y la entrega real se confirma mediante el callback de Twilio. Los residentes pueden registrar vehículos sin escribir fechas: el mock les asigna vencimientos sintéticos. En **Mis vehículos** pueden simular un escaneo propio escribiendo su placa para consultar el estado guardado. No hay OCR real ni consulta oficial al RUNT en esta demo.

Para comprobar el flujo de alertas, registra un vehículo `ABC123` en la cuenta residente y luego, como administrador, abre **Cámara**, escribe `ABC123` y pulsa **Simular salida del vehículo**. En el mock, esa placa tiene el SOAT vencido: debe aparecer una alerta en la pantalla y en el historial del residente. El residente puede ingresar de nuevo y verla en **Mis alertas**.

También puedes usar la API directamente. El siguiente ejemplo crea un residente y registra un vehículo con el SOAT vencido para generar una alerta:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/v1/auth/login `
  -ContentType 'application/json' `
  -Body (@{ correo = 'admin@tu-dominio.co'; contrasena = 'tu-contraseña' } | ConvertTo-Json)
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }

$conjuntos = Invoke-RestMethod `
  -Uri http://localhost:8081/api/v1/admin/conjuntos `
  -Headers $headers
$conjuntoId = $conjuntos[0].id

$residente = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/v1/admin/usuarios `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body (@{
    nombreCompleto = 'Residente de prueba'
    correo = 'residente@ejemplo.co'
    contrasena = 'Residente-demo-2026!'
    telefonoWhatsapp = '+573001234567'
    rol = 'RESIDENT'
    conjuntoId = $conjuntoId
  } | ConvertTo-Json)

$loginResidente = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/v1/auth/login `
  -ContentType 'application/json' `
  -Body (@{ correo = 'residente@ejemplo.co'; contrasena = 'Residente-demo-2026!' } | ConvertTo-Json)
$headersResidente = @{ Authorization = "Bearer $($loginResidente.token)" }
$fechaVencida = (Get-Date).AddDays(-1).ToString('yyyy-MM-dd')
$fechaVigente = (Get-Date).AddDays(30).ToString('yyyy-MM-dd')

Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/v1/vehiculos `
  -Headers $headersResidente `
  -ContentType 'application/json' `
  -Body (@{
    placa = 'ABC123'
    tipoVehiculo = 'CARRO'
    marca = 'Mazda'
    modelo = 'CX-30'
    fechaVencSoat = $fechaVencida
    fechaVencRtm = $fechaVigente
  } | ConvertTo-Json)
```

Para probar la salida del backend con OCR simulado, envía una imagen JPEG o PNG de hasta 5 MB. El mock usa la placa configurada en `MOCK_PLATE` (por defecto `ABC123`):

```powershell
curl.exe -X POST http://localhost:8081/api/v1/lecturas/procesar-salida `
  -H "X-Camera-Key: valor-de-CAMERA_API_KEY" `
  -F "imagen=@C:\ruta\a\captura.jpg"
```

El mock simula una lectura OCR; no reconoce placas en la imagen. Con el vehículo del ejemplo, consulta el historial con:

```powershell
Invoke-RestMethod -Uri http://localhost:8081/api/v1/alertas/historial -Headers $headersResidente
```

La API registra la lectura, el evento y el intento de notificación. Esta llamada es independiente de la pantalla **Cámara**, que permite simular la placa y muestra el resultado del procesamiento.

## Frontend

El frontend es una SPA de React y TypeScript. Muestra pantallas según el rol del JWT: administrador (resumen, cámara de demo, usuarios, alertas y auditoría), residente (vehículos, alertas y teléfono) y auditor (solo auditoría). La pantalla Cámara simula una cámara del edificio y la placa; no captura video ni ejecuta OCR. No necesita CORS en Docker porque Nginx sirve la aplicación y reenvía las llamadas a la API en el mismo origen.

Para desarrollo sin Docker, inicia primero el backend y después ejecuta:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Vite queda en `http://localhost:5173` y usa un proxy local a `http://localhost:8081`.

## API y permisos

Todos los endpoints usan el prefijo `/api/v1`.

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/auth/login` | Público; entrega JWT |
| `POST` | `/admin/usuarios` | ADMIN; crea residentes o auditores |
| `GET` | `/admin/usuarios` | ADMIN |
| `PATCH` | `/admin/usuarios/{id}/estado` | ADMIN; habilita/deshabilita una cuenta |
| `GET` | `/admin/metricas` | ADMIN |
| `GET` | `/admin/conjuntos` | ADMIN; lista conjuntos y sus identificadores |
| `GET` | `/admin/conjuntos/{id}/parametros` | ADMIN; consulta anticipación configurada |
| `PUT` | `/admin/conjuntos/{id}` | ADMIN; actualiza nombre, dirección y NIT |
| `PUT` | `/admin/conjuntos/{id}/parametros` | ADMIN; configura días de anticipación (0-30) |
| `GET` | `/vehiculos/mis-vehiculos` | RESIDENT |
| `POST` | `/vehiculos` | RESIDENT |
| `PATCH` | `/vehiculos/{id}/documentos` | Propietario RESIDENT |
| `PATCH` | `/usuarios/me/telefono-whatsapp` | ADMIN o RESIDENT; actualiza su teléfono |
| `POST` | `/lecturas/procesar-salida` | Cámara mediante `X-Camera-Key`, multipart `imagen` |
| `GET` | `/alertas/historial` | RESIDENT (propias) o ADMIN |
| `POST` | `/notificaciones/whatsapp/prueba` | ADMIN o RESIDENT |
| `GET` | `/auditoria/lecturas` | ADMIN o AUDITOR |
| `GET` | `/auditoria/lecturas/{id}/imagen` | ADMIN o AUDITOR |
| `GET` | `/auditoria/notificaciones` | ADMIN o AUDITOR |
| `GET` | `/auditoria/cache` | ADMIN o AUDITOR |

Las respuestas de vehículos incluyen vencimiento, días restantes y estado (`VIGENTE`, `POR_VENCER` o `VENCIDO`). Las placas aceptadas siguen los formatos `AAA123` y `ABC12D`. Las contraseñas se guardan con BCrypt. Los endpoints de negocio no aceptan acceso anónimo.

## Datos y configuración

El esquema contiene ocho entidades: conjunto residencial, usuario, vehículo, vencimiento de documentos, lectura de salida, evento de alerta, registro WhatsApp y parámetros del sistema. Hibernate actualiza el esquema sin borrar tablas; al iniciar, el backend completa campos nuevos para registros del esquema anterior y copia las fechas antiguas del vehículo a `vencimiento_documentos`.

Configura las variables mediante el panel del proveedor o un archivo `.env` local:

| Variable | Propósito |
|---|---|
| `SPRING_DATASOURCE_URL`, `DB_USER`, `DB_PASSWORD` | Conexión PostgreSQL |
| `JWT_SECRET` | Firma JWT; mínimo 32 bytes |
| `CAMERA_API_KEY` | Autenticación de la cámara |
| `SAFEEXIT_ADMIN_EMAIL`, `SAFEEXIT_ADMIN_PASSWORD`, `SAFEEXIT_ADMIN_PHONE` | Crear el administrador inicial si aún no existe |
| `SAFEEXIT_CORS_ALLOWED_ORIGINS` | Orígenes web permitidos en el despliegue público |
| `RESIDENTIAL_NAME`, `RESIDENTIAL_ADDRESS`, `RESIDENTIAL_NIT` | Conjunto inicial |
| `ALERT_ADVANCE_DAYS` | Anticipación predeterminada; 5 días |
| `SAFEEXIT_ALERT_TEMPLATE` | Plantilla; admite `{nivel}`, `{placa}` y `{motivos}` |
| `SAFEEXIT_WHATSAPP_MODE` | `mock` (predeterminado) o `real`, independiente del OCR |
| `SAFEEXIT_PLATE_OCR_MODE` | `mock` (predeterminado) o `real`, independiente de WhatsApp |
| `SAFEEXIT_INTEGRATIONS_MODE` | Fallback compatible con configuraciones anteriores; prefiere las dos variables específicas |
| `MOCK_PLATE` | Placa devuelta por el OCR simulado |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` | Credenciales privadas de la cuenta Twilio |
| `TWILIO_WHATSAPP_SENDER` | Remitente de WhatsApp de Twilio, por ejemplo `whatsapp:+14155238886` para Sandbox |
| `TWILIO_WHATSAPP_CONTENT_SID` | `ContentSid` de la plantilla aprobada para el remitente |
| `SAFEEXIT_PUBLIC_URL` | URL pública base del backend para recibir callbacks de estado |
| `SAFEEXIT_CACHE_ENABLED` | Caché local con TTL de 60 segundos |
| `PORT` | Puerto asignado por la plataforma |

### Activar WhatsApp real con Twilio

1. Crea una cuenta Twilio y activa WhatsApp Sandbox para pruebas o configura un remitente aprobado para producción. En cuenta Trial, verifica el teléfono destinatario en Twilio y únelo al Sandbox; solo los números verificados pueden recibir mensajes.
2. En `.env`, establece `SAFEEXIT_WHATSAPP_MODE=real`, mantén `SAFEEXIT_PLATE_OCR_MODE=mock` y completa `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_SENDER` y `TWILIO_WHATSAPP_CONTENT_SID`. No publiques ni compartas el archivo `.env`.
3. Configura `SAFEEXIT_PUBLIC_URL` con una URL HTTPS pública que llegue al backend. Para probar localmente puedes exponer el puerto `8081` con un túnel HTTPS; en producción usa la URL pública del backend. Twilio la necesita para reportar si el mensaje fue entregado o falló.
4. Recrea la API con `docker compose up -d --build --force-recreate safeexit-api` y prueba una lectura que genere alerta con un residente que haya autorizado su teléfono.

El envío real depende de la cuenta, el remitente, la plantilla y las condiciones/costos de Twilio. Cuando `TWILIO_WHATSAPP_CONTENT_SID` está configurado, Twilio envía el contenido aprobado de esa plantilla, no el texto libre del registro interno. Las plantillas del Sandbox pueden ser fijas; para incluir dinámicamente placa y documentos se necesita una plantilla aprobada con variables y configurar `ContentVariables`. Con `SAFEEXIT_WHATSAPP_MODE=mock`, la demo solo registra intentos; no contacta a nadie. `PLATE_RECOGNIZER_TOKEN` solo es necesario si también activas `SAFEEXIT_PLATE_OCR_MODE=real`.

## Despliegue gratuito para demostración

El repositorio incluye `render.yaml` con dos servicios gratuitos en Render: el backend Docker y el frontend estático. Usa PostgreSQL de Neon Free como base externa: en Neon crea una base y configura en Render la URL JDBC, usuario y contraseña. La URL debe tener el formato `jdbc:postgresql://HOST/BASE?sslmode=require`. Completa también las variables del administrador, el conjunto y la cámara en Render. El frontend usa `https://safeexit-backend.onrender.com/api/v1` y el backend permite el origen `https://safeexit-frontend.onrender.com`; si cambias los nombres, actualiza ambos valores.

Los niveles gratuitos tienen límites importantes. El backend de Render Free se suspende tras 15 minutos sin tráfico y puede tardar cerca de un minuto en reactivarse; el sitio estático del frontend permanece publicado. Neon Free suspende el cómputo inactivo y limita el almacenamiento. Los frames se guardan en PostgreSQL, no en el filesystem del contenedor. Esto sirve para una entrega académica/demo, **no garantiza operación de cámara en tiempo real ni disponibilidad de producción**. Para una operación continua se requiere alojamiento siempre activo y almacenamiento/retención de imágenes apropiados.

No uses la base de datos gratuita de Render para datos de largo plazo: sus bases Free expiran a los 30 días. Revisa las condiciones y cuotas de los proveedores antes de desplegar.

## Pruebas y compilación

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
Set-Location frontend
npm ci
npm run build
npm run lint
```

Las pruebas usan H2 y mocks; no requieren PostgreSQL, Twilio ni Plate Recognizer.
