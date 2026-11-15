import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { ApiError, jsonBody, request } from './api'
import { Icon, type IconName } from './icons'
import { CameraPage } from './CameraPage'
import type { LoginResponse, Session, UserRole } from './types'
import {
  AccountPage,
  AlertsPage,
  AuditPage,
  DashboardPage,
  UsersPage,
  VehiclesPage,
} from './pages'
import './App.css'

type View = 'overview' | 'camera' | 'vehicles' | 'users' | 'alerts' | 'audit' | 'account'
type Notice = { kind: 'success' | 'error'; text: string }

const pageDetails: Record<View, { title: string; subtitle: string }> = {
  overview: {
    title: 'Resumen',
    subtitle: 'El estado de la movilidad y la documentación de tu conjunto.',
  },
  camera: {
    title: 'Cámara de salida',
    subtitle: 'Simula una lectura de cámara y el análisis de documentos.',
  },
  vehicles: {
    title: 'Mis vehículos',
    subtitle: 'Registra tus vehículos y mantén sus documentos al día.',
  },
  users: {
    title: 'Residentes y auditores',
    subtitle: 'Administra el acceso de las personas de tu conjunto.',
  },
  alerts: {
    title: 'Historial de alertas',
    subtitle: 'Consulta las alertas preventivas generadas en las salidas.',
  },
  audit: {
    title: 'Auditoría',
    subtitle: 'Revisa lecturas de cámara y notificaciones registradas.',
  },
  account: {
    title: 'Mi cuenta',
    subtitle: 'Mantén actualizados tus datos de contacto.',
  },
}

function readStoredSession(): Session | null {
  const stored = sessionStorage.getItem('safeexit.session')
  if (!stored) return null
  try {
    const parsed = JSON.parse(stored) as Session
    if (parsed.token && parsed.email && Array.isArray(parsed.roles)) return parsed
  } catch {
    sessionStorage.removeItem('safeexit.session')
  }
  return null
}

function roleOf(session: Session): UserRole | null {
  if (session.roles.includes('ROLE_ADMIN')) return 'ROLE_ADMIN'
  if (session.roles.includes('ROLE_RESIDENT')) return 'ROLE_RESIDENT'
  if (session.roles.includes('ROLE_AUDITOR')) return 'ROLE_AUDITOR'
  return null
}

function App() {
  const [session, setSession] = useState<Session | null>(readStoredSession)
  const [notice, setNotice] = useState<Notice | null>(null)

  useEffect(() => {
    if (!notice) return
    const timeout = window.setTimeout(() => setNotice(null), 4200)
    return () => window.clearTimeout(timeout)
  }, [notice])

  function signIn(nextSession: Session) {
    sessionStorage.setItem('safeexit.session', JSON.stringify(nextSession))
    setSession(nextSession)
  }

  function signOut() {
    sessionStorage.removeItem('safeexit.session')
    setSession(null)
  }

  if (!session) {
    return (
      <>
        <LoginPage onLogin={signIn} />
        {notice && <Toast notice={notice} onDismiss={() => setNotice(null)} />}
      </>
    )
  }

  const role = roleOf(session)
  if (!role) {
    sessionStorage.removeItem('safeexit.session')
    return <LoginPage onLogin={signIn} />
  }

  return (
    <ApplicationShell
      session={session}
      role={role}
      onSignOut={signOut}
      onNotify={setNotice}
    >
      {notice && <Toast notice={notice} onDismiss={() => setNotice(null)} />}
    </ApplicationShell>
  )
}

function LoginPage({ onLogin }: { onLogin: (session: Session) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = await request<LoginResponse>('/auth/login', null, {
        method: 'POST',
        body: jsonBody({ correo: email.trim(), contrasena: password }),
      })
      onLogin({ token: response.token, email: email.trim(), roles: response.roles })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'No fue posible conectar con SafeExit.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-screen">
      <div className="login-art" aria-hidden="true">
        <div className="art-glow art-glow-one" />
        <div className="art-glow art-glow-two" />
        <div className="art-grid" />
        <div className="art-copy">
          <div className="brand-lockup brand-lockup-light">
            <BrandMark />
            <span>SafeExit<span className="brand-country"> CO</span></span>
          </div>
          <p className="art-eyebrow">MOVILIDAD SEGURA, COMUNIDAD TRANQUILA</p>
          <h1>Tu conjunto,<br />siempre en control.</h1>
          <p className="art-description">
            Información clara para cuidar a quienes entran y salen todos los días.
          </p>
          <div className="art-status">
            <span className="status-dot" />
            Plataforma de gestión residencial
          </div>
        </div>
        <div className="art-orbit orbit-one" />
        <div className="art-orbit orbit-two" />
      </div>

      <section className="login-panel">
        <div className="brand-lockup brand-lockup-dark login-mobile-brand">
          <BrandMark />
          <span>SafeExit<span className="brand-country"> CO</span></span>
        </div>
        <div className="login-card">
          <span className="section-kicker">BIENVENIDO DE NUEVO</span>
          <h2>Inicia sesión</h2>
          <p className="muted login-intro">Ingresa con la cuenta de tu conjunto residencial.</p>
          <form onSubmit={submit} className="form-stack">
            <label className="field">
              <span>Correo electrónico</span>
              <input
                type="email"
                autoComplete="username"
                placeholder="nombre@conjunto.co"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </label>
            <label className="field">
              <span>Contraseña</span>
              <input
                type="password"
                autoComplete="current-password"
                placeholder="Tu contraseña"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </label>
            {error && <div className="inline-error" role="alert">{error}</div>}
            <button className="button button-primary button-wide login-submit" disabled={loading}>
              {loading ? <span className="spinner" /> : 'Entrar a SafeExit'}
              {!loading && <Icon name="chevron" size={17} />}
            </button>
          </form>
          <div className="login-note">
            <Icon name="shield" size={17} />
            <span>Acceso protegido para tu comunidad</span>
          </div>
        </div>
        <p className="login-footer">SAFEEXIT CO · CONTROL VEHICULAR RESIDENCIAL</p>
      </section>
    </main>
  )
}

function ApplicationShell({
  session,
  role,
  onSignOut,
  onNotify,
  children,
}: {
  session: Session
  role: UserRole
  onSignOut: () => void
  onNotify: (notice: Notice) => void
  children: ReactNode
}) {
  const [view, setView] = useState<View>(role === 'ROLE_RESIDENT' ? 'vehicles' : role === 'ROLE_AUDITOR' ? 'audit' : 'overview')
  const navItems: { id: View; label: string; icon: IconName }[] =
    role === 'ROLE_ADMIN'
      ? [
          { id: 'overview', label: 'Resumen', icon: 'grid' },
          { id: 'camera', label: 'Cámara', icon: 'camera' },
          { id: 'users', label: 'Usuarios', icon: 'users' },
          { id: 'alerts', label: 'Alertas', icon: 'alert' },
          { id: 'audit', label: 'Auditoría', icon: 'audit' },
        ]
      : role === 'ROLE_RESIDENT'
        ? [
            { id: 'vehicles', label: 'Mis vehículos', icon: 'car' },
            { id: 'alerts', label: 'Mis alertas', icon: 'alert' },
            { id: 'account', label: 'Mi cuenta', icon: 'users' },
          ]
        : [{ id: 'audit', label: 'Auditoría', icon: 'audit' }]

  const details = pageDetails[view]
  const roleLabel = role === 'ROLE_ADMIN' ? 'Administrador' : role === 'ROLE_RESIDENT' ? 'Residente' : 'Auditor'
  const firstName = session.email.split('@')[0].split(/[._-]/)[0]
  const initials = session.email.slice(0, 1).toUpperCase()

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-lockup brand-lockup-light">
            <BrandMark />
            <span>SafeExit<span className="brand-country"> CO</span></span>
          </div>
          <span className="sidebar-caption">GESTIÓN RESIDENCIAL</span>
        </div>
        <div className="sidebar-label">MENÚ PRINCIPAL</div>
        <nav className="side-nav" aria-label="Navegación principal">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${view === item.id ? 'nav-item-active' : ''}`}
              onClick={() => setView(item.id)}
              type="button"
            >
              <Icon name={item.icon} size={19} />
              <span>{item.label}</span>
              {view === item.id && <span className="nav-active-mark" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <div className="sidebar-security">
          <span className="security-icon"><Icon name="shield" size={18} /></span>
          <div>
            <strong>Acceso protegido</strong>
            <span>Sesión privada y segura</span>
          </div>
        </div>
        <button className="signout-button" onClick={onSignOut} type="button">
          <Icon name="logout" size={18} />
          <span>Cerrar sesión</span>
        </button>
        <div className="sidebar-version">SAFEEXIT CO <span>·</span> v1.0</div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>SafeExit</span><Icon name="chevron" size={14} /><strong>{details.title}</strong></div>
          <div className="topbar-user">
            <div className="topbar-user-copy">
              <strong>{roleLabel}</strong>
              <span>{session.email}</span>
            </div>
            <div className="avatar">{initials}</div>
          </div>
        </header>
        <div className="page-content">
          <div className="page-heading">
            <div>
              <span className="section-kicker">{roleLabel.toUpperCase()}</span>
              <h1>{details.title}</h1>
              <p>{details.subtitle}</p>
            </div>
            <div className="welcome-chip">
              <span className="welcome-dot" />
              Hola, {firstName}
            </div>
          </div>
          {view === 'overview' && role === 'ROLE_ADMIN' && (
            <DashboardPage session={session} onNotify={onNotify} />
          )}
          {view === 'camera' && role === 'ROLE_ADMIN' && (
            <CameraPage session={session} onNotify={onNotify} />
          )}
          {view === 'vehicles' && role === 'ROLE_RESIDENT' && (
            <VehiclesPage session={session} onNotify={onNotify} />
          )}
          {view === 'users' && role === 'ROLE_ADMIN' && (
            <UsersPage session={session} onNotify={onNotify} />
          )}
          {view === 'alerts' && (role === 'ROLE_ADMIN' || role === 'ROLE_RESIDENT') && (
            <AlertsPage session={session} />
          )}
          {view === 'audit' && (role === 'ROLE_ADMIN' || role === 'ROLE_AUDITOR') && (
            <AuditPage session={session} />
          )}
          {view === 'account' && role === 'ROLE_RESIDENT' && (
            <AccountPage session={session} onNotify={onNotify} />
          )}
        </div>
        <footer className="main-footer">
          <span>SafeExit CO · Movilidad segura para tu comunidad</span>
          <span>Los datos se actualizan desde tu conjunto residencial</span>
        </footer>
      </main>
      {children}
    </div>
  )
}

function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <Icon name="shield" size={19} />
    </span>
  )
}

function Toast({ notice, onDismiss }: { notice: Notice; onDismiss: () => void }) {
  return (
    <div className={`toast toast-${notice.kind}`} role="status">
      <span className="toast-mark"><Icon name={notice.kind === 'success' ? 'check' : 'alert'} size={17} /></span>
      <span>{notice.text}</span>
      <button onClick={onDismiss} type="button" aria-label="Cerrar aviso"><Icon name="x" size={16} /></button>
    </div>
  )
}

export default App
