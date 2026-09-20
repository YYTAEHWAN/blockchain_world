import { useCallback, useEffect, useState } from 'react'
import { BrowserProvider } from 'ethers'

/**
 * MetaMask 지갑 연결 훅 (design.md 5.2)
 *
 * - connect(): 지갑 연결 요청 → 첫 계정 주소 반환
 * - account: 현재 연결된 주소 (없으면 null)
 * - 매수 서명 등 실제 트랜잭션은 provider.getSigner() 로 수행한다.
 *
 * window.ethereum 이 없으면(=MetaMask 미설치) hasWallet=false.
 */

// window.ethereum 타입 (간이)
declare global {
  interface Window {
    ethereum?: {
      request: (args: { method: string; params?: unknown[] }) => Promise<unknown>
      on?: (event: string, handler: (...args: unknown[]) => void) => void
      removeListener?: (event: string, handler: (...args: unknown[]) => void) => void
    }
  }
}

export function useWallet() {
  const [account, setAccount] = useState<string | null>(null)
  const [connecting, setConnecting] = useState(false)
  const hasWallet = typeof window !== 'undefined' && !!window.ethereum

  const connect = useCallback(async (): Promise<string | null> => {
    if (!window.ethereum) {
      alert('MetaMask 가 설치되어 있지 않습니다.')
      return null
    }
    setConnecting(true)
    try {
      const accounts = (await window.ethereum.request({
        method: 'eth_requestAccounts',
      })) as string[]
      const addr = accounts?.[0] ?? null
      setAccount(addr)
      return addr
    } finally {
      setConnecting(false)
    }
  }, [])

  /** 매수 등 서명이 필요한 트랜잭션에 쓸 signer 를 반환 */
  const getSigner = useCallback(async () => {
    if (!window.ethereum) throw new Error('지갑이 없습니다')
    const provider = new BrowserProvider(window.ethereum)
    return provider.getSigner()
  }, [])

  // 계정 변경 반영
  useEffect(() => {
    if (!window.ethereum?.on) return
    const handler = (...args: unknown[]) => {
      const accounts = args[0] as string[]
      setAccount(accounts?.[0] ?? null)
    }
    window.ethereum.on('accountsChanged', handler)
    return () => window.ethereum?.removeListener?.('accountsChanged', handler)
  }, [])

  return { account, connect, connecting, hasWallet, getSigner }
}
