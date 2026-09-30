import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { KycStatus, AccountStatus } from '../api/types'
import { useWallet } from '../wallet/useWallet'
import { TxHashDisplay } from '../components/TxHashDisplay'

const STATUS_LABEL: Record<string, string> = {
  PENDING: '심사중',
  APPROVED: '승인',
  REJECTED: '반려',
}

export function KycPage() {
  const { account, connect, hasWallet } = useWallet()
  const [status, setStatus] = useState<KycStatus | null>(null)
  const [history, setHistory] = useState<KycStatus[]>([])
  const [accountStatus, setAccountStatus] = useState<AccountStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [historyLoading, setHistoryLoading] = useState(false)
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

  const loadHistory = () => {
    setHistoryLoading(true)
    api.get<KycStatus[]>('/kyc/history')
      .then((res) => setHistory(res.data))
      .catch(() => setHistory([])) // 에러 시 빈 배열
      .finally(() => setHistoryLoading(false))
  }

  // 계좌(지갑) 동결 상태 조회 - KYC 승인이어도 동결되면 매수 불가하므로 함께 표시
  const loadAccountStatus = () => {
    api.get<AccountStatus>('/investments/me/account')
      .then((res) => setAccountStatus(res.data))
      .catch(() => setAccountStatus(null)) // 조회 실패 시 무시
  }

  useEffect(() => {
    loadStatus()
    loadHistory()
    loadAccountStatus()
  }, [])

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
      loadHistory() // 내역도 갱신
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  // 지갑 주소 복사 함수
  const copyWalletAddress = (address: string) => {
    navigator.clipboard.writeText(address)
      .then(() => alert('지갑 주소가 복사되었습니다.'))
      .catch(() => alert('복사에 실패했습니다.'))
  }

  // Check if user is changing wallet address on reapplication
  const isChangingWallet = status && status.status === 'APPROVED' && 
    wallet && status.walletAddress && 
    wallet.toLowerCase() !== status.walletAddress.toLowerCase();

  if (loading) return <p>불러오는 중...</p>

  return (
    <div className="kyc-page-container">
      <h2>KYC 인증</h2>
      
      <div className="kyc-layout">
        {/* 왼쪽: KYC 인증 상태/신청 */}
        <div className="kyc-left-section">
          <div className="card form-card">
            {status && !reapplying ? (
              <div>
                <p>현재 상태: <strong className={`status-${status.status.toLowerCase()}`}>
                  {STATUS_LABEL[status.status] ?? status.status}
                </strong>
                {status.status === 'APPROVED' && accountStatus?.frozen && (
                  <strong className="status-rejected" style={{ marginLeft: 8 }}>· 계좌 동결됨</strong>
                )}
                </p>
                <dl className="kv">
                  <div><dt>신청 번호</dt><dd>#{status.id}</dd></div>
                  <div>
                    <dt>지갑 주소</dt>
                    <dd className="addr">
                      <span 
                        className="wallet-address-hoverable"
                        title={status.walletAddress}
                      >
                        {status.walletAddress}
                      </span>
                      <button 
                        type="button" 
                        className="small secondary copy-btn"
                        onClick={() => copyWalletAddress(status.walletAddress)}
                      >
                        복사
                      </button>
                    </dd>
                  </div>
                  <div><dt>지갑 순번</dt><dd>{status.walletSequence ? `${status.walletSequence}번째 지갑` : '1번째 지갑'}</dd></div>
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
                    <div><dt>화이트리스트 tx</dt><dd><TxHashDisplay hash={status.whitelistTxHash} /></dd></div>
                  )}
                </dl>
                {status.status === 'PENDING' && (
                  <p className="hint">심사가 진행 중입니다. 관리자 승인 후 매수가 가능합니다.</p>
                )}
                {status.status === 'APPROVED' && (
                  <div>
                    {accountStatus?.frozen ? (
                      <div className="warning-card">
                        <h4>⚠️ 계좌(지갑)가 동결되었습니다</h4>
                        <p>
                          KYC는 승인되었으나 이상거래 심사 결과에 따라 현재 지갑의 매수·전송이 제한됩니다.
                        </p>
                        <p className="hint">
                          • 사유 : {accountStatus.reason?.trim() ? accountStatus.reason : '(별도 사유 미기재)'}<br/>
                          {accountStatus.frozenAt && (
                            <>• 처리일시 : {new Date(accountStatus.frozenAt).toLocaleString()}<br/></>
                          )}
                          {accountStatus.unfreezeRequested && '• 동결 해제 요청이 접수되어 관리자 검토 중입니다.'}
                        </p>
                        <p className="hint">동결 해제 요청은 내 포트폴리오 화면에서 진행할 수 있습니다.</p>
                      </div>
                    ) : (
                      <p className="success">KYC가 승인되어 부동산 토큰을 매수할 수 있습니다.</p>
                    )}
                    <button
                      type="button"
                      onClick={() => {
                        // 새로운 KYC 신청을 위한 폼 표시
                        setName('')
                        setIdNumber('')
                        setWallet('')
                        setError(null)
                        setReapplying(true)
                      }}
                    >
                      새로운 KYC 신청하기 (다른 지갑)
                    </button>
                  </div>
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
                
                {/* Warning when changing wallet address on reapplication */}
                {isChangingWallet && (
                  <div className="warning-card">
                    <h4>⚠️ 지갑 주소 변경 주의</h4>
                    <p>
                      현재 승인된 KYC의 지갑 주소와 다른 주소를 입력했습니다. 
                      새로운 지갑 주소는 KYC 승인 후에만 적용됩니다.
                    </p>
                    <p className="hint">
                      • 기존 지갑: {status?.walletAddress}<br/>
                      • 새 지갑: {wallet}
                    </p>
                    <p className="hint">
                      KYC가 승인되기 전까지는 <strong>부동산 매수가 불가능</strong>합니다.
                      승인 후에는 새 지갑 주소로 매수할 수 있습니다.
                    </p>
                  </div>
                )}
                
                <label>이름
                  <input value={name} onChange={(e) => setName(e.target.value)} required />
                </label>
                <label>식별번호 (주민등록번호 등)
                  <input value={idNumber} onChange={(e) => setIdNumber(e.target.value)} required />
                </label>
                <label>지갑 주소
                  <input 
                    value={wallet} 
                    onChange={(e) => setWallet(e.target.value)} 
                    placeholder="0x..." 
                    required 
                  />
                </label>
                {hasWallet && (
                  <button type="button" className="secondary" onClick={onConnect}>
                    {account ? `연결됨: ${account.slice(0, 8)}...${account.slice(-6)}` : 'MetaMask 로 주소 채우기'}
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
        </div>

        {/* 오른쪽: KYC 신청 내역 */}
        <div className="kyc-right-section">
          <div className="card">
            <div className="history-header">
              <h3>KYC 신청 내역</h3>
              <button 
                type="button" 
                className="small secondary"
                onClick={() => {
                  if (history.length === 0) {
                    loadHistory()
                  }
                }}
              >
                {historyLoading ? '새로고침 중...' : '새로고침'}
              </button>
            </div>
            
            {historyLoading ? (
              <p className="hint">내역 불러오는 중...</p>
            ) : history.length === 0 ? (
              <p className="hint">KYC 신청 내역이 없습니다.</p>
            ) : (
              <div className="history-table compact">
                <table>
                  <thead>
                    <tr>
                      <th>번호</th>
                      <th>지갑</th>
                      <th>순번</th>
                      <th>상태</th>
                      <th>신청일</th>
                      <th>복사</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((item) => (
                      <tr key={item.id} className={item.id === status?.id ? 'current' : ''}>
                        <td>#{item.id}</td>
                        <td>
                          <span 
                            className="wallet-address-hoverable"
                            title={item.walletAddress}
                          >
                            {item.walletAddress.slice(0, 8)}...{item.walletAddress.slice(-6)}
                          </span>
                        </td>
                        <td>{item.walletSequence ? `${item.walletSequence}번째` : '-'}</td>
                        <td>
                          <span className={`status-badge status-${item.status.toLowerCase()}`}>
                            {STATUS_LABEL[item.status] ?? item.status}
                          </span>
                        </td>
                        <td>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '-'}</td>
                        <td>
                          <button 
                            type="button" 
                            className="small secondary"
                            onClick={() => copyWalletAddress(item.walletAddress)}
                            title="지갑 주소 복사"
                          >
                            📋
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
