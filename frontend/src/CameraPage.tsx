import { useState, type FormEvent } from 'react'
import { jsonBody, request } from './api'
import { Icon } from './icons'
import type { DocumentStatus, Session } from './types'

interface SimulatedReading {
  lecturaId: number
  placaDetectada: string
  vehiculoRegistrado: boolean
  documentosAlDia: boolean
  soat: DocumentStatus | null
  rtm: DocumentStatus | null
  motivosAlerta: string[]
  estadoNotificacion: string
}

export function CameraPage({
  session,
  onNotify,
}: {
  session: Session
  onNotify: (notice: { kind: 'success' | 'error'; text: string }) => void
}) {
  const [plate, setPlate] = useState('ABC123')
  const [result, setResult] = useState<SimulatedReading | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function simulateReading(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedPlate = plate.trim().toUpperCase().replace(/\s/g, '')
    setError('')
    setResult(null)
    setBusy(true)
    try {
      const reading = await request<SimulatedReading>('/admin/demo/lecturas', session, {
        method: 'POST',
        body: jsonBody({ placa: normalizedPlate }),
      })
      setResult(reading)
      onNotify({
        kind: 'success',
        text: reading.motivosAlerta.length
          ? 'Lectura procesada; se generó una alerta de prueba para el residente.'
          : 'Lectura procesada; no se requiere una alerta.',
      })
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : 'No fue posible simular la lectura.'
      setError(message)
      onNotify({ kind: 'error', text: message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page-stack">
      <div className="camera-demo-notice" role="note">
        <Icon name="shield" size={18} />
        <span>
          <strong>Cámara de edificio simulada:</strong> no hay una cámara conectada. Al simular una salida, SafeExit guarda la lectura, revisa los documentos registrados y crea una alerta para el residente si corresponde. WhatsApp solo se envía de verdad cuando Twilio está configurado; de lo contrario, se simula.
        </span>
      </div>

      <div className="camera-page-grid">
        <section className="panel camera-panel">
          <div className="panel-heading">
            <div>
              <h2>Cámara del sótano</h2>
              <p>Simula la llegada de una placa a la salida del conjunto.</p>
            </div>
            <span className="camera-state camera-state-active"><span />Simulador listo</span>
          </div>

          <div className="camera-preview camera-preview-simulated">
            <div className="camera-placeholder">
              <span><Icon name="camera" size={30} /></span>
              <strong>Entrada de cámara simulada</strong>
              <small>La lectura se iniciará al pulsar el botón.</small>
            </div>
            <span className="camera-live-label"><span />DEMO</span>
            <span className="camera-scan-line" />
          </div>

          <form className="camera-simulation-form" onSubmit={simulateReading}>
            <label className="field camera-plate-field">
              <span>Placa que detecta la cámara simulada</span>
              <input
                value={plate}
                onChange={(event) => setPlate(event.target.value.toUpperCase().replace(/\s/g, ''))}
                placeholder="ABC123"
                maxLength={6}
                pattern="([A-Za-z]{3}[0-9]{3}|[A-Za-z]{3}[0-9]{2}[A-Za-z])"
                required
              />
              <small>Usa la placa de un vehículo registrado en SafeExit.</small>
            </label>
            {error && <p className="camera-inline-error" role="alert">{error}</p>}
            <button className="button button-primary button-wide" disabled={busy}>
              {busy ? <span className="spinner" /> : <><Icon name="activity" size={17} /> Simular salida del vehículo</>}
            </button>
          </form>

          <p className="camera-test-hint">
            Prueba sugerida: registra <strong>ABC123</strong> en una cuenta residente y simula su salida. En la demo esa placa tiene el SOAT vencido.
          </p>
        </section>

        <section className="panel camera-result-panel">
          <div className="panel-heading">
            <div>
              <h2>Resultado del análisis</h2>
              <p>Lectura de salida y aviso asociado al vehículo.</p>
            </div>
            {result && result.vehiculoRegistrado && (
              <span className={`document-pill ${result.motivosAlerta.length ? 'document-alert' : 'document-ok'}`}>
                <span />{result.motivosAlerta.length ? 'Alerta' : 'Sin alerta'}
              </span>
            )}
          </div>

          {!result && !busy && (
            <div className="camera-empty">
              <Icon name="search" size={22} />
              <strong>Esperando una lectura</strong>
              <span>Simula la salida de una placa para revisar el flujo completo.</span>
            </div>
          )}
          {busy && <div className="loading-state"><span className="spinner spinner-dark" />Analizando lectura…</div>}
          {result && (
            <div className="camera-result-content">
              <div className="camera-result-plate">
                <span>Placa detectada</span>
                <strong>{result.placaDetectada}</strong>
              </div>
              <p className={`camera-registration-state ${result.vehiculoRegistrado ? 'camera-registration-found' : ''}`}>
                {result.vehiculoRegistrado
                  ? `Vehículo registrado · lectura #${result.lecturaId}`
                  : 'Vehículo no registrado · no se envió alerta'}
              </p>
              {result.vehiculoRegistrado && result.soat && result.rtm && (
                <div className="document-grid">
                  <DocumentResult label="SOAT" status={result.soat} />
                  <DocumentResult label="RTM" status={result.rtm} />
                </div>
              )}
              {result.motivosAlerta.length > 0 ? (
                <div className="camera-alert-result">
                  <strong>Alerta creada para el residente</strong>
                  <span>{result.motivosAlerta.map(humanize).join(' · ')}</span>
                  <small>Notificación WhatsApp simulada: {result.estadoNotificacion}</small>
                </div>
              ) : result.vehiculoRegistrado ? (
                <p className="camera-source">Documentos sin vencimientos que requieran alerta.</p>
              ) : null}
            </div>
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

function humanize(value: string): string {
  return value.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase())
}

function formatDay(value: string): string {
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(`${value}T12:00:00`))
}
