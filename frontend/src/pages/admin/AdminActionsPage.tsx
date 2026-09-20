import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api, errorMessage } from '../../api/client'
import type { AnomalyFlag } from '../../api/types'

// 조치 유형 라벨/스타일
const ACTION_META: Record<string, { label: string; cls: string; who: string }> = {
  FROZEN:   { label: '동결 중',   cls: 'sev-high',   who: '동결권자' },
  UNFROZEN: { label: '동결 해제', cls: 'sev-low',    who: '해제권자' },
  REVIEWED: { label: '검토 완료', cls: 'sev-medium', who: '검토자' },
}

type FilterKey = 'ALL' | 'FROZEN' | 'UNFROZEN' | 'REVIEWED'

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'FROZEN', label: '동결 중' },
  { key: 'UNFROZEN', label: '동결 해제' },
  { key: 'REVIEWED', label: '검토 완료' },
]

export function AdminActionsPage() {
  const location = useLocation()
  const [items, setItems] = useState<AnomalyFlag[]>([])
  const [filter, setFilter] = useState<FilterKey>('ALL')
  const [targetInvestor, setTargetInvestor] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [actingId, setActingId] = useState<number | null>(null)

  function load(f: FilterKey = filter) {
    setLoading(true)
    const url = f === 'ALL' ? '/admin/monitoring/actions' : `/admin/monitoring/actions?action=${f}`
    return api
      .get<AnomalyFlag[]>(url)
      .then((res) => setItems(res.data))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    // 쿼리 파라미터 파싱: /admin/actions?action=FROZEN&investor=email@test.com
    const params = new URLSearchParams(location.search)
    const action = params.get('action')?.toUpperCase() as FilterKey | undefined
    const investor = params.get('investor')
    if (action && ['ALL','FROZEN','UNFROZEN','REVIEWED'].includes(action)) {
      setFilter(action)
    }
    if (investor) {
      setTargetInvestor(investor)
    }
  }, [location.search])

  useEffect(() => {
    load(filter)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter])

  async function handleUnfreeze(id: number, who: string) {
    if (!window.confirm(`${who} 지갑 동결을 해제합니다.\n화이트리스트에 다시 등록되어 매수/전송이 재허용됩니다.\n진행할까요?`)) return
    const note = window.prompt('해제 사유(선택).', '') ?? ''
    setActingId(id)
    setError(null)
    try {
      await api.post(`/admin/monitoring/anomalies/${id}/unfreeze`, { note })
      await load(filter)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setActingId(null)
    }
  }

  // 상태별 카운트 (전체 목록 기준이 아니라 현재 로드된 목록 기준이므로, 요약은 전체 조회로 계산)
  const frozenCount = items.filter((a) => a.action === 'FROZEN').length
  const unfrozenCount = items.filter((a) => a.action === 'UNFROZEN').length
  const reviewedCount = items.filter((a) => a.action === 'REVIEWED').length

  if (loading && items.length === 0) return <p>불러오는 중...</p>

  return (
    <div>
      <h2>조치 이력</h2>
      <p className="hint">이상거래 후보에 대해 취해진 조치(검토 완료 · 지갑 동결 · 동결 해제)를 최신순으로 보여줍니다.</p>

      {error && <p className="error">{error}</p>}

      {filter === 'ALL' && (
        <div className="stat-grid">
          <Stat label="동결 중" value={`${frozenCount}건`} highlight={frozenCount > 0} />
          <Stat label="동결 해제" value={`${unfrozenCount}건`} />
          <Stat label="검토 완료" value={`${reviewedCount}건`} />
        </div>
      )}

      <div className="btn-row" style={{ marginTop: 16 }}>
        {FILTERS.map((f) => (
          <button
            key={f.key}
            className={`sm${filter === f.key ? '' : ' secondary'}`}
            onClick={() => setFilter(f.key)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {items.length === 0 ? (
        <p className="hint" style={{ marginTop: 16 }}>해당하는 조치 이력이 없습니다.</p>
      ) : (
        <table className="table" style={{ marginTop: 12 }}>
          <thead>
            <tr>
              <th>조치일시</th>
              <th>상태</th>
              <th>대상 투자자</th>
              <th>부동산</th>
              <th>탐지 규칙</th>
              <th>담당자</th>
              <th>사유</th>
              <th>조치</th>
            </tr>
          </thead>
          <tbody>
            {items.map((a) => {
              const meta = ACTION_META[a.action ?? ''] ?? { label: a.action ?? '-', cls: '', who: '담당자' }
              const who = a.investorEmail ?? `#${a.investorId}`
              const pendingRequest = a.action === 'FROZEN' && a.unfreezeRequested
              const isTarget = targetInvestor && (a.investorEmail === targetInvestor || `#${a.investorId}` === targetInvestor)
              const rowStyle = isTarget
                ? { background: '#fff5e6', borderLeft: '4px solid #f39c12' }
                : pendingRequest
                ? { background: '#fff5e6' }
                : undefined
              return (
                <tr key={a.id} style={rowStyle}>
                  <td>{a.reviewedAt ? new Date(a.reviewedAt).toLocaleString() : '-'}</td>
                  <td>
                    <span className={meta.cls}>{meta.label}</span>
                    {pendingRequest && <div className="sev-medium" style={{ fontSize: 12, marginTop: 2 }}>● 해제요청</div>}
                  </td>
                  <td>{who}</td>
                  <td>{a.propertyName ?? `#${a.propertyId}`}</td>
                  <td>{a.rule}</td>
                  <td>{meta.who} : {a.reviewedBy ?? '-'}</td>
                  <td>
                    {a.reviewNote?.trim() ? a.reviewNote : '-'}
                    {pendingRequest && (
                      <div style={{ marginTop: 4, fontSize: 12 }}>
                        <span className="muted">투자자 소명</span> : {a.unfreezeRequestNote?.trim() ? a.unfreezeRequestNote : '(내용 없음)'}
                      </div>
                    )}
                  </td>
                  <td>
                    {a.action === 'FROZEN' ? (
                      <button
                        className={pendingRequest ? 'sm unfreeze-request-btn' : 'sm secondary'}
                        disabled={actingId === a.id}
                        onClick={() => handleUnfreeze(a.id, who)}
                      >
                        동결해제
                      </button>
                    ) : (
                      <span className="hint">-</span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </div>
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
