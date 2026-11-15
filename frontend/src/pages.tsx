import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { jsonBody, request, requestBlob } from './api'
import { Icon } from './icons'
import type {
  Alert,
  AuditNotification,
  AuditReading,
  Metrics,
  Resident,
  ResidentialComplex,
  Session,
  SystemParameters,
  Vehicle,
} from './types'

type Notice = { kind: 'success' | 'error'; text: string }

function useResource<T>(session: Session, path: string, enabled = true) {
  const [version, setVersion] = useState(0)
  const key = `${path}|${session.token}|${version}`
  const [state, setState] = useState<{
    key: string
    data: T | null
    loading: boolean
    error: string
  }>({ key: '', data: null, loading: true, error: '' })

  useEffect(() => {
    let active = true
    if (!enabled) return () => { active = false }
    request<T>(path, session)
      .then((result) => {
        if (active) setState({ key, data: result, loading: false, error: '' })
      })
      .catch((caught: unknown) => {
        if (active) {
          setState({
            key,
            data: null,
            loading: false,
            error: caught instanceof Error ? caught.message : 'No fue posible cargar la información.',
          })
        }
      })
    return () => {
      active = false
    }
  }, [enabled, key, path, session])

  const isCurrent = state.key === key
  return {
    data: isCurrent ? state.data : null,
    loading: enabled && (!isCurrent || state.loading),
    error: isCurrent ? state.error : '',
    reload: () => setVersion((current) => current + 1),
  }
}

function PageCard({
  title,
  caption,
  action,
  children,
  className = '',
}: {
  title: string
  caption?: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-heading">
        <div>
          <h2>{title}</h2>
          {caption && <p>{caption}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  )
}

function LoadingState({ label = 'Cargando información…' }: { label?: string }) {
  return <div className="loading-state"><span className="spinner spinner-dark" />{label}</div>
}

function ErrorState({ message }: { message: string }) {
  return <div className="error-state" role="alert"><Icon name="alert" size={18} /><span>{message}</span></div>
}

function EmptyState({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="empty-state">
      <span className="empty-icon"><Icon name="search" size={22} /></span>
      <strong>{title}</strong>
      <p>{detail}</p>
    </div>
  )
}

function MetricCard({
  label,
  value,
  hint,
  icon,
  tone,
}: {
  label: string
  value: number | string
  hint: string
  icon: 'car' | 'activity' | 'alert' | 'check'
  tone: 'mint' | 'blue' | 'amber' | 'violet'
}) {
  return (
    <article className="metric-card">
      <div className="metric-top">
        <span className="metric-label">{label}</span>
        <span className={`metric-icon metric-icon-${tone}`}><Icon name={icon} size={19} /></span>
      </div>
      <strong className="metric-value">{value}</strong>
      <span className="metric-hint">{hint}</span>
    </article>
  )
}

export function DashboardPage({
  session,
  onNotify,
}: {
  session: Session
  onNotify: (notice: Notice) => void
}) {
  const metrics = useResource<Metrics>(session, '/admin/metricas')
  const alerts = useResource<Alert[]>(session, '/alertas/historial')

  return (
    <div className="page-stack">
      <section className="welcome-banner">
        <div className="welcome-content">
          <span className="welcome-eyebrow">PANEL DE CONTROL</span>
          <h2>Una salida más segura empieza con información clara.</h2>
          <p>Consulta la actividad reciente y el estado de la documentación de tu comunidad.</p>
        </div>
        <div className="welcome-graphic" aria-hidden="true">
          <div className="graphic-ring ring-back" />
          <div className="graphic-ring ring-front" />
          <span className="graphic-shield"><Icon name="shield" size={38} /></span>
          <span className="graphic-orbit orbit-a" />
          <span className="graphic-orbit orbit-b" />
        </div>
      </section>

      <div className="section-title-row">
        <div>
          <h2>Resumen de actividad</h2>
          <p>Indicadores generales del conjunto residencial</p>
        </div>
        <span className="updated-label"><span className="status-dot status-dot-green" />Datos en vivo</span>
      </div>

      {metrics.error && <ErrorState message={metrics.error} />}
      <div className="metric-grid">
        <MetricCard label="Vehículos" value={metrics.loading ? '—' : metrics.data?.vehiculosRegistrados ?? 0} hint="Registrados en el conjunto" icon="car" tone="mint" />
        <MetricCard label="Lecturas" value={metrics.loading ? '—' : metrics.data?.lecturasProcesadas ?? 0} hint="Procesadas por cámara" icon="activity" tone="blue" />
        <MetricCard label="Alertas" value={metrics.loading ? '—' : metrics.data?.alertasGeneradas ?? 0} hint="Preventivas generadas" icon="alert" tone="amber" />
        <MetricCard label="Entrega WhatsApp" value={metrics.loading ? '—' : `${metrics.data?.porcentajeEntrega ?? 0}%`} hint={`${metrics.data?.mensajesEntregados ?? 0} mensajes entregados`} icon="check" tone="violet" />
      </div>

      <div className="dashboard-columns">
        <PageCard
          title="Alertas recientes"
          caption="Últimos eventos asociados a vehículos del conjunto"
          action={<span className="mini-count">{alerts.data?.length ?? 0}</span>}
        >
          {alerts.loading ? <LoadingState /> : alerts.error ? <ErrorState message={alerts.error} /> :
            alerts.data?.length ? (
              <div className="alert-list">
                {alerts.data.slice(0, 5).map((alert) => <AlertRow key={alert.id} alert={alert} />)}
              </div>
            ) : <EmptyState title="Aún no hay alertas" detail="Cuando se detecte un documento próximo a vencer, aparecerá aquí." />}
        </PageCard>
        <SettingsPanel session={session} onNotify={onNotify} />
      </div>
      <div className="role-note">
        <Icon name="shield" size={17} />
        <span>El reconocimiento de placas y los avisos funcionan en modo de demostración.</span>
      </div>
    </div>
  )
}

function SettingsPanel({
  session,
  onNotify,
}: {
  session: Session
  onNotify: (notice: Notice) => void
}) {
  const complexes = useResource<ResidentialComplex[]>(session, '/admin/conjuntos')
  const [selectedId, setSelectedId] = useState('')
  const [daysByComplex, setDaysByComplex] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [localError, setLocalError] = useState('')
  const complexId = selectedId || String(complexes.data?.[0]?.id ?? '')
  const parameters = useResource<SystemParameters>(
    session,
    `/admin/conjuntos/${complexId}/parametros`,
    Boolean(complexId),
  )
  const days = daysByComplex[complexId] ?? String(parameters.data?.diasAnticipacion ?? 5)

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!complexId) return
    setSaving(true)
    setLocalError('')
    try {
      await request<SystemParameters>(
        `/admin/conjuntos/${complexId}/parametros`,
        session,
        { method: 'PUT', body: jsonBody({ diasAnticipacion: Number(days) }) },
      )
      parameters.reload()
      onNotify({ kind: 'success', text: 'Anticipación de alertas actualizada.' })
    } catch (caught) {
      setLocalError(caught instanceof Error ? caught.message : 'No fue posible guardar los cambios.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <PageCard title="Anticipación de alertas" caption="Define cuándo avisar antes del vencimiento.">
      {complexes.loading ? <LoadingState /> : complexes.error ? <ErrorState message={complexes.error} /> : (
        <form className="settings-form" onSubmit={save}>
          {complexes.data && complexes.data.length > 1 && (
            <label className="field">
              <span>Conjunto residencial</span>
              <select value={complexId} onChange={(event) => setSelectedId(event.target.value)}>
                {complexes.data.map((complex) => <option value={complex.id} key={complex.id}>{complex.nombre}</option>)}
              </select>
            </label>
          )}
          <div className="days-setting">
            <label className="field">
              <span>Avisar con anticipación</span>
              <div className="input-suffix">
                <input
                  type="number"
                  min="0"
                  max="30"
                  value={days}
                  onChange={(event) => setDaysByComplex((current) => ({ ...current, [complexId]: event.target.value }))}
                  required
                />
                <span>días</span>
              </div>
            </label>
            <p>Un aviso se genera cuando SOAT o RTM vence dentro de este período.</p>
          </div>
          {localError && <ErrorState message={localError} />}
          {parameters.error && <ErrorState message={parameters.error} />}
          <button className="button button-secondary" disabled={saving || !complexId}>
            {saving ? <span className="spinner spinner-dark" /> : 'Guardar configuración'}
          </button>
        </form>
      )}
    </PageCard>
  )
}

function AlertRow({ alert }: { alert: Alert }) {
  return (
    <div className="alert-row">
      <span className={`alert-symbol ${alert.critica ? 'alert-symbol-critical' : ''}`}>
        <Icon name="alert" size={17} />
      </span>
      <div className="alert-main">
        <div className="alert-title-line">
          <strong>{alert.placa}</strong>
          <span className={`severity-pill ${alert.critica ? 'severity-critical' : 'severity-warning'}`}>
            {alert.critica ? 'Crítica' : 'Preventiva'}
          </span>
        </div>
        <span>{humanize(alert.motivo)}</span>
      </div>
      <div className="alert-meta">
        <span>{formatDate(alert.fecha)}</span>
        <DeliveryStatus value={alert.estadoEntrega} />
      </div>
    </div>
  )
}

export function VehiclesPage({
  session,
  onNotify,
}: {
  session: Session
  onNotify: (notice: Notice) => void
}) {
  const vehicles = useResource<Vehicle[]>(session, '/vehiculos/mis-vehiculos')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<Vehicle | null>(null)
  const [editSoat, setEditSoat] = useState('')
  const [editRtm, setEditRtm] = useState('')
  const [manualDates, setManualDates] = useState(false)

  async function addVehicle(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSaving(true)
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      await request<Vehicle>('/vehiculos', session, {
        method: 'POST',
        body: jsonBody({
          placa: String(form.get('placa')).toUpperCase().replace(/\s/g, ''),
          tipoVehiculo: form.get('tipoVehiculo'),
          marca: form.get('marca'),
          modelo: form.get('modelo'),
          ...(manualDates ? {
            fechaVencSoat: form.get('fechaVencSoat'),
            fechaVencRtm: form.get('fechaVencRtm'),
          } : {}),
        }),
      })
      formElement.reset()
      setManualDates(false)
      vehicles.reload()
      onNotify({ kind: 'success', text: 'Vehículo registrado; SOAT y RTM se consultaron en modo demo.' })
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible registrar el vehículo.')
    } finally {
      setSaving(false)
    }
  }

  async function saveDocuments(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!editing) return
    setError('')
    setSaving(true)
    try {
      await request<Vehicle>(`/vehiculos/${editing.id}/documentos`, session, {
        method: 'PATCH',
        body: jsonBody({ fechaVencSoat: editSoat, fechaVencRtm: editRtm }),
      })
      setEditing(null)
      vehicles.reload()
      onNotify({ kind: 'success', text: 'Fechas de documentos actualizadas.' })
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible actualizar los documentos.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page-stack">
      <div className="vehicle-summary-strip">
        <span className="summary-icon"><Icon name="car" size={20} /></span>
        <div><strong>{vehicles.data?.length ?? 0} vehículos registrados</strong><span>La demo simula la consulta de SOAT y RTM; no verifica datos oficiales.</span></div>
        <span className="summary-shield"><Icon name="shield" size={21} /></span>
      </div>
      <div className="vehicle-layout">
        <PageCard title="Vehículos registrados" caption="Estado de SOAT y revisión técnico-mecánica">
          {vehicles.loading ? <LoadingState /> : vehicles.error ? <ErrorState message={vehicles.error} /> :
            vehicles.data?.length ? (
              <div className="vehicle-list">
                {vehicles.data.map((vehicle) => (
                  <article className="vehicle-card" key={vehicle.id}>
                    <div className="vehicle-card-top">
                      <span className="vehicle-icon"><Icon name="car" size={21} /></span>
                      <div className="vehicle-identity">
                        <strong>{vehicle.placa}</strong>
                        <span>{[vehicle.marca, vehicle.modelo, vehicle.tipoVehiculo === 'MOTO' ? 'Motocicleta' : 'Automóvil'].filter(Boolean).join(' · ')}</span>
                      </div>
                      <span className={`document-pill ${vehicle.documentosAlDia ? 'document-ok' : 'document-alert'}`}>
                        <span />{vehicle.documentosAlDia ? 'Al día' : 'Revisar'}
                      </span>
                    </div>
                    <div className="document-grid">
                      <DocumentDate label="SOAT" status={vehicle.soat} />
                      <DocumentDate label="Tecnomecánica" status={vehicle.rtm} />
                    </div>
                    {editing?.id === vehicle.id ? (
                      <form className="inline-edit-form" onSubmit={saveDocuments}>
                        <label className="field"><span>Vencimiento SOAT</span><input type="date" value={editSoat} onChange={(event) => setEditSoat(event.target.value)} required /></label>
                        <label className="field"><span>Vencimiento RTM</span><input type="date" value={editRtm} onChange={(event) => setEditRtm(event.target.value)} required /></label>
                        <div className="form-actions">
                          <button className="button button-primary button-small" disabled={saving}>{saving ? 'Guardando…' : 'Guardar fechas'}</button>
                          <button className="button button-quiet button-small" type="button" onClick={() => setEditing(null)}>Cancelar</button>
                        </div>
                      </form>
                    ) : (
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => {
                          setEditing(vehicle)
                          setEditSoat(vehicle.soat.fechaVencimiento)
                          setEditRtm(vehicle.rtm.fechaVencimiento)
                        }}
                      >
                        Actualizar documentos <Icon name="chevron" size={15} />
                      </button>
                    )}
                  </article>
                ))}
              </div>
            ) : <EmptyState title="Aún no registras vehículos" detail="Completa el formulario para añadir el primero." />}
        </PageCard>
        <PageCard title="Registrar vehículo" caption="Las fechas se consultan automáticamente en modo demo.">
          <form className="form-stack form-compact" onSubmit={addVehicle}>
            <label className="field">
              <span>Placa</span>
              <input name="placa" placeholder="ABC123" pattern="([A-Za-z]{3}[0-9]{3}|[A-Za-z]{3}[0-9]{2}[A-Za-z])" maxLength={6} required />
              <small>Formato colombiano: AAA123 o ABC12D</small>
            </label>
            <div className="form-row">
              <label className="field"><span>Tipo</span><select name="tipoVehiculo" defaultValue="CARRO"><option value="CARRO">Automóvil</option><option value="MOTO">Motocicleta</option></select></label>
              <label className="field"><span>Marca</span><input name="marca" maxLength={50} placeholder="Ej. Mazda" /></label>
            </div>
            <label className="field"><span>Modelo</span><input name="modelo" maxLength={50} placeholder="Ej. CX-30" /></label>
            <label className="manual-fallback-toggle">
              <input
                type="checkbox"
                checked={manualDates}
                onChange={(event) => setManualDates(event.target.checked)}
              />
              <span>Ingresar fechas manualmente (respaldo)</span>
            </label>
            {manualDates && (
              <div className="form-row">
                <label className="field"><span>Vence SOAT</span><input name="fechaVencSoat" type="date" required /></label>
                <label className="field"><span>Vence RTM</span><input name="fechaVencRtm" type="date" required /></label>
              </div>
            )}
            <p className="demo-disclaimer">La consulta es simulada y no reemplaza una verificación oficial del RUNT.</p>
            {error && <ErrorState message={error} />}
            <button className="button button-primary button-wide" disabled={saving}>
              {saving ? <span className="spinner" /> : <><Icon name="plus" size={17} /> Registrar vehículo</>}
            </button>
          </form>
        </PageCard>
      </div>
    </div>
  )
}

function DocumentDate({ label, status }: { label: string; status: Vehicle['soat'] }) {
  const tone = status.estado === 'VENCIDO' ? 'date-critical' : status.estado === 'POR_VENCER' ? 'date-warning' : 'date-ok'
  const detail = status.diasRestantes < 0
    ? `Vencido hace ${Math.abs(status.diasRestantes)} días`
    : status.diasRestantes === 0
      ? 'Vence hoy'
      : `Vence en ${status.diasRestantes} días`
  return (
    <div className="document-date">
      <span>{label}</span>
      <strong>{formatDay(status.fechaVencimiento)}</strong>
      <small className={tone}>{detail}</small>
    </div>
  )
}

export function UsersPage({
  session,
  onNotify,
}: {
  session: Session
  onNotify: (notice: Notice) => void
}) {
  const users = useResource<Resident[]>(session, '/admin/usuarios')
  const complexes = useResource<ResidentialComplex[]>(session, '/admin/conjuntos')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [busyUser, setBusyUser] = useState<number | null>(null)

  async function createUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      await request<Resident>('/admin/usuarios', session, {
        method: 'POST',
        body: jsonBody({
          nombreCompleto: form.get('nombreCompleto'),
          correo: form.get('correo'),
          contrasena: form.get('contrasena'),
          telefonoWhatsapp: form.get('telefonoWhatsapp'),
          rol: form.get('rol'),
          conjuntoId: Number(form.get('conjuntoId')),
        }),
      })
      formElement.reset()
      users.reload()
      onNotify({ kind: 'success', text: 'La cuenta quedó creada y ya puede iniciar sesión.' })
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : 'No fue posible crear la cuenta.'
      setError(message)
    } finally {
      setSaving(false)
    }
  }

  async function changeStatus(user: Resident) {
    setBusyUser(user.id)
    try {
      await request<Resident>(`/admin/usuarios/${user.id}/estado`, session, {
        method: 'PATCH',
        body: jsonBody({ activo: !user.activo }),
      })
      users.reload()
      onNotify({ kind: 'success', text: user.activo ? 'Cuenta desactivada.' : 'Cuenta activada.' })
    } catch (caught) {
      onNotify({ kind: 'error', text: caught instanceof Error ? caught.message : 'No fue posible actualizar la cuenta.' })
    } finally {
      setBusyUser(null)
    }
  }

  return (
    <div className="page-stack">
      <div className="users-summary">
        <span className="summary-icon"><Icon name="users" size={20} /></span>
        <div><strong>Acceso para tu comunidad</strong><span>Crea cuentas de residente o auditor. Las cuentas de administrador se configuran de forma segura fuera de la API.</span></div>
      </div>
      <div className="users-layout">
        <PageCard title="Personas registradas" caption="Controla quién puede acceder al sistema.">
          {users.loading ? <LoadingState /> : users.error ? <ErrorState message={users.error} /> :
            users.data?.length ? (
              <div className="table-scroll">
                <table className="data-table">
                  <thead><tr><th>Persona</th><th>Rol</th><th>Estado</th><th /></tr></thead>
                  <tbody>
                    {users.data.map((user) => (
                      <tr key={user.id}>
                        <td><div className="person-cell"><span className="person-avatar">{user.nombreCompleto.slice(0, 1).toUpperCase()}</span><div><strong>{user.nombreCompleto}</strong><span>{user.correo}</span></div></div></td>
                        <td><span className="role-pill">{humanize(user.rol.replace('ROLE_', ''))}</span></td>
                        <td><span className={`status-pill ${user.activo ? 'status-enabled' : 'status-disabled'}`}><span />{user.activo ? 'Activo' : 'Inactivo'}</span></td>
                        <td className="table-action">
                          <button className="button button-quiet button-small" disabled={busyUser === user.id} onClick={() => void changeStatus(user)} type="button">
                            {busyUser === user.id ? '…' : user.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : <EmptyState title="Sin usuarios registrados" detail="Crea la primera cuenta con el formulario." />}
        </PageCard>
        <PageCard title="Crear cuenta" caption="Agrega un residente o auditor.">
          {complexes.loading ? <LoadingState /> : complexes.error ? <ErrorState message={complexes.error} /> : (
            <form className="form-stack form-compact" onSubmit={createUser}>
              <label className="field"><span>Nombre completo</span><input name="nombreCompleto" maxLength={100} placeholder="Nombre y apellido" required /></label>
              <label className="field"><span>Correo electrónico</span><input name="correo" type="email" maxLength={100} placeholder="persona@ejemplo.co" required /></label>
              <label className="field"><span>Contraseña temporal</span><input name="contrasena" type="password" minLength={12} maxLength={72} placeholder="Mínimo 12 caracteres" required /><small>La persona puede cambiarla según la política de tu conjunto.</small></label>
              <label className="field"><span>WhatsApp</span><input name="telefonoWhatsapp" type="tel" pattern="\+57[0-9]{10}" placeholder="+573001234567" required /><small>Formato colombiano: +57 y 10 dígitos.</small></label>
              <div className="form-row">
                <label className="field"><span>Perfil</span><select name="rol" defaultValue="RESIDENT"><option value="RESIDENT">Residente</option><option value="AUDITOR">Auditor</option></select></label>
                <label className="field"><span>Conjunto</span><select name="conjuntoId" required defaultValue=""><option value="" disabled>Selecciona</option>{complexes.data?.map((complex) => <option key={complex.id} value={complex.id}>{complex.nombre}</option>)}</select></label>
              </div>
              {error && <ErrorState message={error} />}
              <button className="button button-primary button-wide" disabled={saving || !complexes.data?.length}>
                {saving ? <span className="spinner" /> : <><Icon name="plus" size={17} /> Crear cuenta</>}
              </button>
            </form>
          )}
        </PageCard>
      </div>
    </div>
  )
}

export function AlertsPage({ session }: { session: Session }) {
  const alerts = useResource<Alert[]>(session, '/alertas/historial')
  return (
    <PageCard title="Alertas registradas" caption="Eventos generados cuando un vehículo tiene documentos vencidos o próximos a vencer.">
      {alerts.loading ? <LoadingState /> : alerts.error ? <ErrorState message={alerts.error} /> :
        alerts.data?.length ? (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Fecha</th><th>Placa</th><th>Motivo</th><th>Prioridad</th><th>Notificación</th></tr></thead>
              <tbody>
                {alerts.data.map((alert) => (
                  <tr key={alert.id}>
                    <td>{formatDate(alert.fecha)}</td>
                    <td><strong className="plate-text">{alert.placa}</strong></td>
                    <td>{humanize(alert.motivo)}</td>
                    <td><span className={`severity-pill ${alert.critica ? 'severity-critical' : 'severity-warning'}`}>{alert.critica ? 'Crítica' : 'Preventiva'}</span></td>
                    <td><DeliveryStatus value={alert.estadoEntrega} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <EmptyState title="No hay alertas para mostrar" detail="Las alertas aparecerán aquí cuando se registren lecturas de salida." />}
    </PageCard>
  )
}

export function AuditPage({ session }: { session: Session }) {
  const readings = useResource<AuditReading[]>(session, '/auditoria/lecturas')
  const notifications = useResource<AuditNotification[]>(session, '/auditoria/notificaciones')
  return (
    <div className="page-stack">
      <div className="audit-stats">
        <div className="audit-stat"><span className="metric-icon metric-icon-blue"><Icon name="activity" size={19} /></span><div><strong>{readings.data?.length ?? 0}</strong><span>Lecturas registradas</span></div></div>
        <div className="audit-stat"><span className="metric-icon metric-icon-violet"><Icon name="alert" size={19} /></span><div><strong>{notifications.data?.length ?? 0}</strong><span>Notificaciones auditables</span></div></div>
      </div>
      <PageCard title="Lecturas de salida" caption="Registro inmutable de placas procesadas por el sistema.">
        {readings.loading ? <LoadingState /> : readings.error ? <ErrorState message={readings.error} /> :
          readings.data?.length ? (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Fecha y hora</th><th>Placa detectada</th><th>Vehículo</th><th>Documentos</th><th>Imagen</th></tr></thead>
                <tbody>
                  {readings.data.map((reading) => (
                    <tr key={reading.id}>
                      <td>{formatDate(reading.fechaHora)}</td>
                      <td><strong className="plate-text">{reading.placaDetectada}</strong></td>
                      <td>{reading.vehiculoId ? `#${reading.vehiculoId}` : 'No registrado'}</td>
                      <td><span className={`status-pill ${reading.documentosAlDia ? 'status-enabled' : 'status-disabled'}`}><span />{reading.documentosAlDia ? 'Al día' : 'Revisar'}</span></td>
                      <td>{reading.imagenDisponible ? <button className="text-button" type="button" onClick={() => void downloadImage(reading.id, session)}>Ver imagen</button> : <span className="muted">No disponible</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState title="Aún no hay lecturas" detail="Las salidas procesadas por la cámara aparecerán aquí." />}
      </PageCard>
      <PageCard title="Notificaciones WhatsApp" caption="Historial y estado de entrega reportado por el proveedor.">
        {notifications.loading ? <LoadingState /> : notifications.error ? <ErrorState message={notifications.error} /> :
          notifications.data?.length ? (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Fecha</th><th>Destino</th><th>Mensaje</th><th>Estado</th></tr></thead>
                <tbody>
                  {notifications.data.map((item) => (
                    <tr key={item.id}>
                      <td>{formatDate(item.fechaEnvio)}</td><td>{item.telefonoDestino}</td><td className="message-cell">{item.mensaje}</td><td><DeliveryStatus value={item.estadoEntrega} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState title="Sin notificaciones" detail="Los envíos registrados se mostrarán en esta tabla." />}
      </PageCard>
    </div>
  )
}

async function downloadImage(id: number, session: Session) {
  try {
    const url = URL.createObjectURL(await requestBlob(`/auditoria/lecturas/${id}/imagen`, session))
    const link = document.createElement('a')
    link.href = url
    link.download = `lectura-${id}`
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (caught) {
    window.alert(caught instanceof Error ? caught.message : 'No fue posible descargar la imagen.')
  }
}

export function AccountPage({
  session,
  onNotify,
}: {
  session: Session
  onNotify: (notice: Notice) => void
}) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function updatePhone(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      await request('/usuarios/me/telefono-whatsapp', session, {
        method: 'PATCH',
        body: jsonBody({ telefonoWhatsapp: form.get('telefonoWhatsapp') }),
      })
      onNotify({ kind: 'success', text: 'Tu número de WhatsApp se actualizó.' })
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible actualizar el número.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="account-layout">
      <PageCard title="Datos de acceso" caption="Información asociada a tu sesión actual.">
        <div className="account-info">
          <div className="account-info-icon"><Icon name="users" size={22} /></div>
          <div><span>Correo electrónico</span><strong>{session.email}</strong></div>
          <div><span>Perfil</span><strong>Residente</strong></div>
        </div>
      </PageCard>
      <PageCard title="Número de WhatsApp" caption="Se usa para enviarte avisos preventivos de documentos.">
        <form className="form-stack account-form" onSubmit={updatePhone}>
          <label className="field"><span>Número colombiano</span><input name="telefonoWhatsapp" type="tel" pattern="\+57[0-9]{10}" placeholder="+573001234567" required /><small>Escribe el indicativo +57 seguido de 10 dígitos.</small></label>
          {error && <ErrorState message={error} />}
          <button className="button button-primary" disabled={saving}>{saving ? 'Guardando…' : 'Actualizar número'}</button>
        </form>
      </PageCard>
    </div>
  )
}

function DeliveryStatus({ value }: { value: string }) {
  const normalized = value.toUpperCase()
  const delivered = normalized === 'ENTREGADO' || normalized === 'DELIVERED' || normalized === 'READ'
  const failed = normalized === 'FALLIDO' || normalized === 'FAILED' || normalized === 'UNDELIVERED'
  return (
    <span className={`delivery-status ${delivered ? 'delivery-ok' : failed ? 'delivery-failed' : 'delivery-pending'}`}>
      <span />{humanize(normalized)}
    </span>
  )
}

function humanize(value: string) {
  return value
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase())
}

function formatDay(value: string) {
  if (!value) return 'Sin fecha'
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`))
}

function formatDate(value: string) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
