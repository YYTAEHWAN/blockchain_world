import { useState } from 'react'

type TxHashDisplayProps = {
  hash: string | null | undefined
  visibleLength?: number
}

function shortenHash(hash: string, visibleLength: number) {
  if (hash.length <= visibleLength) return hash
  // 앞 8자리...뒤 6자리 형식으로 표시
  const prefixLength = 8
  const suffixLength = 6
  if (hash.length <= prefixLength + suffixLength) return hash
  return `${hash.slice(0, prefixLength)}...${hash.slice(-suffixLength)}`
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return
    } catch {
      // 보안 설정 등으로 Clipboard API가 실패하면 fallback을 사용합니다.
    }
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  textarea.remove()
  if (!copied) throw new Error('클립보드 복사에 실패했습니다.')
}

export function TxHashDisplay({ hash, visibleLength = 16 }: TxHashDisplayProps) {
  const [copied, setCopied] = useState(false)

  if (!hash) return <span className="muted">-</span>

  const onCopy = async () => {
    try {
      await copyText(hash)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      setCopied(false)
    }
  }

  return (
    <span className="tx-hash-control">
      <span
        className="tx-hash-value addr"
        data-full-hash={hash}
        title={hash}
        tabIndex={0}
        aria-label={`전체 트랜잭션 해시: ${hash}`}
      >
        {shortenHash(hash, visibleLength)}
      </span>
      <button
        type="button"
        className="tx-copy-button"
        onClick={onCopy}
        aria-label="트랜잭션 해시 복사"
      >
        {copied ? '복사됨' : '복사'}
      </button>
    </span>
  )
}
