export type UserRole = 'ROLE_ADMIN' | 'ROLE_RESIDENT' | 'ROLE_AUDITOR'

export interface Session {
  token: string
  email: string
  roles: string[]
}

export interface Metrics {
  vehiculosRegistrados: number
  lecturasProcesadas: number
  alertasGeneradas: number
  mensajesWhatsapp: number
  mensajesEntregados: number
  mensajesFallidos: number
  porcentajeEntrega: number
}

export interface DocumentStatus {
  fechaVencimiento: string
  diasRestantes: number
  estado: 'VIGENTE' | 'POR_VENCER' | 'VENCIDO'
}

export interface Vehicle {
  id: number
  placa: string
  tipoVehiculo: 'CARRO' | 'MOTO'
  marca: string | null
  modelo: string | null
  soat: DocumentStatus
  rtm: DocumentStatus
  documentosAlDia: boolean
}

export interface Alert {
  id: number
  placa: string
  motivo: string
  critica: boolean
  estadoEntrega: string
  fecha: string
}

export interface Resident {
  id: number
  nombreCompleto: string
  correo: string
  telefonoWhatsapp: string
  rol: string
  activo: boolean
  conjuntoId: number
}

export interface ResidentialComplex {
  id: number
  nombre: string
  direccion: string
  nit: string
}

export interface AuditReading {
  id: number
  placaDetectada: string
  vehiculoId: number | null
  documentosAlDia: boolean
  fechaHora: string
  imagenDisponible: boolean
}

export interface AuditNotification {
  id: number
  telefonoDestino: string
  mensaje: string
  estadoEntrega: string
  idProveedor: string
  fechaEnvio: string
}

export interface CacheStatus {
  habilitado: boolean
  entradas: number
  capacidadMaxima: number
  ttlSegundos: number
}

export interface SystemParameters {
  id: number
  conjuntoId: number
  diasAnticipacion: number
}

export interface LoginResponse {
  token: string
  tipo: string
  expiraEnSegundos: number
  roles: string[]
}
