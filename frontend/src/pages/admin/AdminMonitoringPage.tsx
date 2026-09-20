import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../../api/client'
import type { MonitoringSummary, AdminTx, AnomalyFlag } from '../../api/types'

const SEVERITY_CLASS: Record<string, string> = {
  HIGH: 'sev-high',
  MEDIUM: 'sev-medium',
  LOW: 'sev-low',
}

export function AdminMonitoringPage() {
  const navigate = useNavigate()
  const [summary, setSummary] = useState<MonitoringSummary | null>(null)
  const [anomalies, setAnomalies] = useState<AnomalyFlag[]>([])
  const [txs, setTxs] = useState<AdminTx[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [actingId, setActingId] = useState<number | null>(null)

  function loadAll() {
    return Promise.all([
      api.get<MonitoringSummary>('/admin/monitoring/summary'),
      api.get<AnomalyFlag[]>('/admin/monitoring/anomalies'),
      api.get<AdminTx[]>('/admin/monitoring/transactions'),
    ]).then(([s, a, t]) => {
      setSummary(s.data)
      setAnomalies(a.data)
      setTxs(t.data)
    })
  }

  useEffect(() => {
    loadAll()
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  async function handleReview(id: number) {
    const note = window.prompt('검토 메모(선택). 정상 확인/종결 사유를 남기세요.', '')
    if (note === null) return
    setActingId(id)
    setError(null)
    try {
      await api.post(`/admin/monitoring/anomalies/${id}/review`, { note })
      await loadAll()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setActingId(null)
    }
  }

  async function handleFreeze(id: number, who: string) {
    if (!window.confirm(`${who} 지갑을 동결합니다.\n모든 부동산 토큰 화이트리스트에서 제거되어 이후 매수/전송이 온체인에서 차단됩니다.\n진행할까요?`)) return
    const note = window.prompt('동결 사유(선택).', '') ?? ''
    setActingId(id)
    setError(null)
    try {
      await api.post(`/admin/monitoring/anomalies/${id}/freeze`, { note })
      await loadAll()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setActingId(null)
    }
  }



  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error">{error}</p>

  return (
    <div>
      <h2>모니터링 대시보드</h2>

      {summary && (
        <div className="stat-grid">
          <Stat label="총 거래 건수" value={`${summary.totalTransactions}건`} />
          <Stat label="총 거래 수량" value={summary.totalQuantity.toLocaleString()} />
          <Stat label="총 거래 금액" value={`${summary.totalAmount.toLocaleString()}원`} />
          <Stat label="투자자 수" value={`${summary.distinctInvestors}명`} />
          <Stat label="부동산 수" value={`${summary.propertyCount}건`} />
          <Stat label="이상거래(미검토)" value={`${summary.anomalyCount} (${summary.unreviewedAnomalyCount})`} highlight={summary.unreviewedAnomalyCount > 0} />
        </div>
      )}

      <h3 style={{ marginTop: 28 }}>이상거래 후보</h3>
      {anomalies.length === 0 ? (
        <p className="hint">탐지된 이상거래가 없습니다.</p>
      ) : (
        <table className="table">
          <thead><tr><th>탐지시각</th><th>규칙</th><th>심각도</th><th>투자자</th><th>부동산</th><th>사유</th><th>상태</th><th>조치</th></tr></thead>
          <tbody>
            {anomalies.map((a) => {
              const who = a.investorEmail ?? `#${a.investorId}`
              return (
                <tr key={a.id}>
                  <td>{new Date(a.detectedAt).toLocaleString()}</td>
                  <td>{a.rule}</td>
                  <td><span className={SEVERITY_CLASS[a.severity]}>{a.severity}</span></td>
                  <td>{who}</td>
                  <td>{a.propertyName ?? `#${a.propertyId}`}</td>
                  <td>{a.detail}</td>
                  <td>{renderStatus(a)}</td>
                  <td>
                    {a.action === 'FROZEN' ? (
                      <button
                        className="sm secondary"
                        onClick={() => {
                          // 해제요청이 있으면 투자자 필터도 넣어서 조치이력 페이지로 이동
                          const params = new URLSearchParams()
                          params.set('action', 'FROZEN')
                          if (a.unfreezeRequested) {
                            params.set('investor', who)
                          }
                          navigate(`/admin/actions?${params.toString()}`)
                        }}
                      >
                        동결해제
                      </button>
                    ) : a.reviewed ? (
                      <span className="hint">처리됨</span>
                    ) : (
                      <span style={{ display: 'inline-flex', gap: 6 }}>
                        <button
                          className="sm secondary"
                          disabled={actingId === a.id}
                          onClick={() => handleReview(a.id)}
                        >
                          검토완료
                        </button>
                        <button
                          className="sm danger"
                          disabled={actingId === a.id}
                          onClick={() => handleFreeze(a.id, who)}
                        >
                          지갑동결
                        </button>
                      </span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}

      <h3 style={{ marginTop: 28 }}>전체 거래 이력</h3>
      {txs.length === 0 ? (
        <p className="hint">거래 이력이 없습니다.</p>
      ) : (
        <table className="table">
          <thead><tr><th>일시</th><th>투자자</th><th>부동산</th><th>유형</th><th>수량</th><th>금액</th></tr></thead>
          <tbody>
            {txs.map((t) => (
              <tr key={t.txId}>
                <td>{new Date(t.createdAt).toLocaleString()}</td>
                <td>{t.investorEmail}</td>
                <td>{t.propertyName}</td>
                <td>{t.type}</td>
                <td>{t.quantity.toLocaleString()}</td>
                <td>{t.amount.toLocaleString()}원</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

function renderStatus(a: AnomalyFlag) {
  if (!a.reviewed) return <span className="sev-medium">미검토</span>
  let label: string
  let cls: string
  let who: string
  if (a.action === 'FROZEN') {
    label = a.unfreezeRequested ? '동결됨(해제요청)' : '동결됨'
    cls = a.unfreezeRequested ? 'sev-high unfreeze-requested' : 'sev-high'
    who = '동결권자'
  } else if (a.action === 'UNFROZEN') {
    label = '동결해제됨'; cls = 'sev-low'; who = '해제권자'
  } else {
    label = '검토완료'; cls = 'sev-low'; who = '검토자'
  }
  const when = a.reviewedAt ? new Date(a.reviewedAt).toLocaleString() : ''
  const note = a.unfreezeRequested ? `투자자 소명: ${a.unfreezeRequestNote ?? '(내용 없음)'}\n${a.reviewNote ?? ''}` : (a.reviewNote ?? '')
  return (
    <span title={`${a.reviewedBy ?? ''} ${when}\n${note}`}>
      <span className={cls}>{label}</span>
      {a.reviewedBy ? (
        <div className="hint" style={{ marginTop: 2 }}>{who} : {a.reviewedBy}</div>
      ) : null}
    </span>
  )
}

function Stat({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`stat-card${highlight ? ' stat-alert' : ''}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
    </div>
  )
}
