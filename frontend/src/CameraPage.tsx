import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError, request } from './api'
import { Icon } from './icons'
import type { DocumentStatus, Session } from './types'

interface DocumentLookup {
  placa: string
  fuenteConsulta: string
  soat: DocumentStatus
  rtm: DocumentStatus
  documentosAlDia: boolean
}

export function CameraPage({ session }: { session: Session }) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const requestInProgress = useRef(false)
  const [cameraActive, setCameraActive] = useState(false)
  const [scanPaused, setScanPaused] = useState(false)
  const [plate, setPlate] = useState('ABC123')
  const [result, setResult] = useState<DocumentLookup | null>(null)
  const [manualResult, setManualResult] = useState<DocumentLookup | null>(null)
  const [lookupError, setLookupError] = useState('')
  const [cameraError, setCameraError] = useState('')
  const [lastScan, setLastScan] = useState('')
  const [busy, setBusy] = useState(false)
  const [manualSoat, setManualSoat] = useState('')
  const [manualRtm, setManualRtm] = useState('')

  useEffect(() => {
    if (videoRef.current && streamRef.current) {
      videoRef.current.srcObject = streamRef.current
    }
  }, [cameraActive])

  useEffect(() => () => {
    streamRef.current?.getTracks().forEach((track) => track.stop())
  }, [])

  const lookupPlate = useCallback(async () => {
    const normalizedPlate = plate.trim().toUpperCase().replace(/\s/g, '')
    if (!/^[A-Z]{3}[0-9]{3}$|^[A-Z]{3}[0-9]{2}[A-Z]$/.test(normalizedPlate)) {
      setLookupError('Escribe una placa colombiana válida para simular la consulta.')
      return
    }
    if (requestInProgress.current) return

    requestInProgress.current = true
    setBusy(true)
    setLookupError('')
    setManualResult(null)
    try {
      const data = await request<DocumentLookup>(
        `/admin/demo/consulta-documentos/${encodeURIComponent(normalizedPlate)}`,
        session,
      )
      setResult(data)
      setScanPaused(false)
      setLastScan(new Date().toLocaleTimeString())
    } catch (caught) {
      const message = caught instanceof ApiError
        ? caught.status === 404
          ? 'No se encontró la placa en la consulta de demo. Puedes evaluar las fechas manualmente para esta revisión.'
          : caught.message
        : 'No fue posible consultar el servicio de demo. Puedes evaluar las fechas manualmente para esta revisión.'
      setLookupError(message)
      setResult(null)
      setScanPaused(true)
    } finally {
      requestInProgress.current = false
      setBusy(false)
    }
  }, [plate, session])

  useEffect(() => {
    if (!cameraActive || scanPaused) return
    const firstScan = window.setTimeout(() => void lookupPlate(), 0)
    const timer = window.setInterval(() => void lookupPlate(), 8000)
    return () => {
      window.clearTimeout(firstScan)
      window.clearInterval(timer)
    }
  }, [cameraActive, lookupPlate, scanPaused])

  async function startCamera() {
    setCameraError('')
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError('Este navegador no permite acceso a la cámara. Usa “Consultar placa” para continuar con la demo.')
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false,
      })
      streamRef.current = stream
      setScanPaused(false)
      setCameraActive(true)
    } catch {
      setCameraError('No se pudo abrir la cámara. Revisa el permiso del navegador o consulta la placa manualmente.')
    }
  }

  function stopCamera() {
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    if (videoRef.current) videoRef.current.srcObject = null
    setCameraActive(false)
  }

  function evaluateManualFallback(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedPlate = plate.trim().toUpperCase().replace(/\s/g, '')
    const soat = evaluateDate(manualSoat)
    const rtm = evaluateDate(manualRtm)
    setManualResult({
      placa: normalizedPlate,
      fuenteConsulta: 'INGRESO_MANUAL',
      soat,
      rtm,
      documentosAlDia: soat.estado === 'VIGENTE' && rtm.estado === 'VIGENTE',
    })
    setResult(null)
    setLookupError('')
    setLastScan(new Date().toLocaleTimeString())
  }

  const visibleResult = manualResult ?? result

  return (
    <div className="page-stack">
      <div className="camera-demo-notice" role="note">
        <Icon name="shield" size={18} />
        <span>
          <strong>Modo de demostración:</strong> el video es de tu cámara, pero la placa y la consulta de SOAT/RTM son simuladas. No se consultan fuentes oficiales ni se envía el video.
        </span>
      </div>

      <div className="camera-page-grid">
        <section className="panel camera-panel">
          <div className="panel-heading">
            <div>
              <h2>Cámara de salida</h2>
              <p>Vista en vivo con consulta simulada cada 8 segundos.</p>
            </div>
            <span className={`camera-state ${cameraActive ? 'camera-state-active' : ''}`}>
              <span />{cameraActive ? 'En vivo' : 'Detenida'}
            </span>
          </div>

          <div className={`camera-preview ${cameraActive ? 'camera-preview-active' : ''}`}>
            <video ref={videoRef} autoPlay muted playsInline />
            {!cameraActive && (
              <div className="camera-placeholder">
                <span><Icon name="camera" size={30} /></span>
                <strong>Vista previa de la cámara</strong>
                <small>Al iniciar, el navegador solicitará permiso.</small>
              </div>
            )}
            {cameraActive && <span className="camera-live-label"><span />VIDEO LOCAL</span>}
          </div>

          <label className="field camera-plate-field">
            <span>Placa de prueba que simulará la cámara</span>
            <input
              value={plate}
              onChange={(event) => setPlate(event.target.value.toUpperCase().replace(/\s/g, ''))}
              placeholder="ABC123"
              maxLength={6}
              aria-describedby="camera-plate-help"
            />
            <small id="camera-plate-help">El sistema no lee placas reales en esta versión demo.</small>
          </label>

          {cameraError && <p className="camera-inline-error" role="alert">{cameraError}</p>}
          <div className="camera-actions">
            {cameraActive ? (
              <button className="button button-quiet" type="button" onClick={stopCamera}>Detener cámara</button>
            ) : (
              <button className="button button-primary" type="button" onClick={() => void startCamera()}>
                <Icon name="camera" size={17} /> Iniciar cámara
              </button>
            )}
            <button className="button button-secondary" type="button" onClick={() => void lookupPlate()} disabled={busy}>
              {busy ? <span className="spinner spinner-dark" /> : <><Icon name="search" size={16} /> Consultar placa</>}
            </button>
          </div>
        </section>

        <section className="panel camera-result-panel">
          <div className="panel-heading">
            <div>
              <h2>Consulta de documentos</h2>
              <p>Resultado de SOAT y revisión técnico-mecánica.</p>
            </div>
            {visibleResult && (
              <span className={`document-pill ${visibleResult.documentosAlDia ? 'document-ok' : 'document-alert'}`}>
                <span />{visibleResult.documentosAlDia ? 'Al día' : 'Revisar'}
              </span>
            )}
          </div>

          {busy && !visibleResult && <div className="loading-state"><span className="spinner spinner-dark" />Consultando demo…</div>}
          {!busy && !visibleResult && !lookupError && (
            <div className="camera-empty">
              <Icon name="search" size={22} />
              <strong>Esperando una consulta</strong>
              <span>Inicia la cámara o consulta la placa de prueba.</span>
            </div>
          )}
          {lookupError && <div className="camera-inline-error" role="alert">{lookupError}</div>}
          {visibleResult && (
            <div className="camera-result-content">
              <div className="camera-result-plate">
                <span>Placa consultada</span>
                <strong>{visibleResult.placa}</strong>
              </div>
              <div className="document-grid">
                <DocumentResult label="SOAT" status={visibleResult.soat} />
                <DocumentResult label="RTM" status={visibleResult.rtm} />
              </div>
              <p className="camera-source">
                Fuente: {visibleResult.fuenteConsulta === 'SIMULADA' ? 'consulta simulada, no oficial' : 'ingreso manual para esta revisión'}.
                {lastScan && <> Última consulta: {lastScan}.</>}
              </p>
            </div>
          )}

          {lookupError && (
            <form className="camera-fallback-form" onSubmit={evaluateManualFallback}>
              <strong>Respaldo manual para esta revisión</strong>
              <div className="form-row">
                <label className="field"><span>Vence SOAT</span><input type="date" value={manualSoat} onChange={(event) => setManualSoat(event.target.value)} required /></label>
                <label className="field"><span>Vence RTM</span><input type="date" value={manualRtm} onChange={(event) => setManualRtm(event.target.value)} required /></label>
              </div>
              <button className="button button-secondary button-small">Evaluar fechas manuales</button>
              <small>Este cálculo es solo para la demo actual; no guarda ni valida los documentos.</small>
            </form>
          )}
        </section>
      </div>
    </div>
  )
}

function DocumentResult({ label, status }: { label: string; status: DocumentStatus }) {
  const tone = status.estado === 'VENCIDO' ? 'date-critical' : status.estado === 'POR_VENCER' ? 'date-warning' : 'date-ok'
  const remaining = status.diasRestantes < 0
    ? `Vencido hace ${Math.abs(status.diasRestantes)} días`
    : status.diasRestantes === 0
      ? 'Vence hoy'
      : `Vence en ${status.diasRestantes} días`
  return (
    <div className="document-date">
      <span>{label}</span>
      <strong>{formatDay(status.fechaVencimiento)}</strong>
      <small className={tone}>{remaining}</small>
    </div>
  )
}

function evaluateDate(value: string): DocumentStatus {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expiration = new Date(`${value}T00:00:00`)
  const days = Math.round((expiration.getTime() - today.getTime()) / 86_400_000)
  return {
    fechaVencimiento: value,
    diasRestantes: days,
    estado: days < 0 ? 'VENCIDO' : days <= 5 ? 'POR_VENCER' : 'VIGENTE',
  }
}

function formatDay(value: string): string {
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(`${value}T12:00:00`))
}
