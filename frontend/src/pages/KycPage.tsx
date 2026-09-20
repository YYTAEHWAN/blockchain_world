import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { KycStatus } from '../api/types'
import { useWallet } from '../wallet/useWallet'

const STATUS_LABEL: Record<string, string> = {
  PENDING: '심사중',
  APPROVED: '승인',
  REJECTED: '반려',
}

export function KycPage() {
  const { account, connect, hasWallet } = useWallet()
  const [status, setStatus] = useState<KycStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [reapplying, setReapplying] = useState(false) // 반려 후 재신청 폼 표시

  const [name, setName] = useState('')
  const [idNumber, setIdNumber] = useState('')
  const [wallet, setWallet] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const loadStatus = () => {
    setLoading(true)
    api.get<KycStatus>('/kyc/me')
      .then((res) => setStatus(res.data))
      .catch(() => setStatus(null)) // 신청 내역 없음(404)
      .finally(() => setLoading(false))
  }

  useEffect(loadStatus, [])

  const onConnect = async () => {
    const addr = await connect()
    if (addr) setWallet(addr)
  }

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.post('/kyc', { name, idNumber, walletAddress: wallet })
      setReapplying(false)
      loadStatus()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <p>불러오는 중...</p>

  return (
    <div className="card form-card">
      <h2>KYC 인증</h2>

      {status && !reapplying ? (
        <div>
          <p>현재 상태: <strong className={`status-${status.status.toLowerCase()}`}>
            {STATUS_LABEL[status.status] ?? status.status}
          </strong></p>
          <dl className="kv">
            <div><dt>신청 번호</dt><dd>#{status.id}</dd></div>
            <div><dt>지갑 주소</dt><dd className="addr">{status.walletAddress}</dd></div>
            <div><dt>신청 일시</dt><dd>{status.createdAt ? new Date(status.createdAt).toLocaleString() : '-'}</dd></div>
            {status.status !== 'PENDING' && (
              <>
                <div><dt>심사 일시</dt><dd>{status.reviewedAt ? new Date(status.reviewedAt).toLocaleString() : '-'}</dd></div>
                <div><dt>심사자</dt><dd>{status.reviewedBy ?? '-'}</dd></div>
              </>
            )}
            {status.status === 'REJECTED' && status.rejectReason && (
              <div><dt>반려 사유</dt><dd>{status.rejectReason}</dd></div>
            )}
            {status.status === 'APPROVED' && status.whitelistTxHash && (
              <div><dt>화이트리스트 tx</dt><dd className="addr">{status.whitelistTxHash}</dd></div>
            )}
          </dl>
          {status.status === 'PENDING' && (
            <p className="hint">심사가 진행 중입니다. 관리자 승인 후 매수가 가능합니다.</p>
          )}
          {status.status === 'APPROVED' && (
            <p className="success">KYC가 승인되어 부동산 토큰을 매수할 수 있습니다.</p>
          )}
          {status.status === 'REJECTED' && (
            <>
              <p className="error">KYC가 반려되었습니다. 사유를 확인 후 다시 신청할 수 있습니다.</p>
              <button
                type="button"
                onClick={() => {
                  // 기존 지갑 주소를 채워두고 재신청 폼 표시 (개인정보는 재입력)
                  setName('')
                  setIdNumber('')
                  setWallet(status.walletAddress ?? '')
                  setError(null)
                  setReapplying(true)
                }}
              >
                다시 신청하기
              </button>
            </>
          )}
        </div>
      ) : (
        <form onSubmit={onSubmit}>
          <p className="hint">개인식별정보는 암호화되어 오프체인에만 저장되며, 온체인에는 기록되지 않습니다.</p>
          <label>이름
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          <label>식별번호 (주민등록번호 등)
            <input value={idNumber} onChange={(e) => setIdNumber(e.target.value)} required />
          </label>
          <label>지갑 주소
            <input value={wallet} onChange={(e) => setWallet(e.target.value)} placeholder="0x..." required />
          </label>
          {hasWallet && (
            <button type="button" className="secondary" onClick={onConnect}>
              {account ? `연결됨: ${account.slice(0, 10)}...` : 'MetaMask 로 주소 채우기'}
            </button>
          )}
          {error && <p className="error">{error}</p>}
          <div className="btn-row">
            <button type="submit" disabled={submitting}>
              {submitting ? '신청 중...' : (reapplying ? '다시 신청' : 'KYC 신청')}
            </button>
            {reapplying && (
              <button type="button" className="secondary" onClick={() => { setReapplying(false); setError(null) }}>
                취소
              </button>
            )}
          </div>
        </form>
      )}
    </div>
  )
}
