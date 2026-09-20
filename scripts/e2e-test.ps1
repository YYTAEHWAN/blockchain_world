# =====================================================================
# RWA 부동산 조각투자 STO — 전체 시나리오 E2E 통합 테스트 (작업 18)
#
# 커버 범위:
#   1) 가입 -> KYC -> 승인(온체인 화이트리스트) -> 발행 부동산 매수 -> 배당 -> 모니터링
#   2) 정합성: 잔여수량 초과 매수 거부 / KYC 미승인 매수 거부
#   3) 접근통제: 투자자 토큰으로 관리자 API 접근 시 403
#   4) 이상거래 탐지(대량거래) + 조치(동결/해제) + 투자자 계정 상태
#   5) KYC 반려 후 재신청
#
# 사전조건: Hardhat 노드(8545) + Factory 배포 + 백엔드(8080) 기동
# 사용법:   cd c:\rwa\forHanwha ; powershell -ExecutionPolicy Bypass -File .\scripts\e2e-test.ps1
#
# 참고: Windows PowerShell 5.1 의 Invoke-RestMethod 는 byte[] 본문/한글 전송이
#       불안정하여, .NET System.Net.Http.HttpClient 로 UTF-8 요청을 보낸다.
# =====================================================================

$adminEmail = $env:ADMIN_EMAIL
$adminPassword = $env:ADMIN_PASSWORD
if ([string]::IsNullOrWhiteSpace($adminEmail) -or [string]::IsNullOrWhiteSpace($adminPassword)) {
    throw "ADMIN_EMAIL and ADMIN_PASSWORD environment variables are required."
}

$fail = 0

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)

function Req($method, $url, $json, $token) {
    $req = New-Object System.Net.Http.HttpRequestMessage($method, "$base$url")
    if ($null -ne $json) {
        $req.Content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, "application/json")
    }
    if ($token) {
        $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)
    }
    $resp = $client.SendAsync($req).Result
    $body = $resp.Content.ReadAsStringAsync().Result
    $obj = $null
    if ($body) { try { $obj = $body | ConvertFrom-Json } catch { $obj = $null } }
    return [pscustomobject]@{ status = [int]$resp.StatusCode; body = $obj; raw = $body }
}
function PostJson($url, $hash, $token) { return Req "POST" $url ($hash | ConvertTo-Json -Compress) $token }
function GetJson($url, $token)        { return Req "GET"  $url $null $token }

function Check($name, $cond) {
    if ($cond) { Write-Host ("  [PASS] " + $name) -ForegroundColor Green; $script:pass++ }
    else       { Write-Host ("  [FAIL] " + $name) -ForegroundColor Red;   $script:fail++ }
}

Write-Host "`n========== E2E 통합 테스트 시작 ==========" -ForegroundColor Cyan

# [1] 관리자 로그인
Write-Host "`n[1] 관리자 로그인" -ForegroundColor Yellow
$admin = PostJson "/api/auth/login" @{ email=$adminEmail; password=$adminPassword }
$aT = $admin.body.token
Check "관리자 로그인 및 ADMIN 역할" ($admin.status -eq 200 -and $admin.body.role -eq "ADMIN")

# [2] 부동산 등록(온체인 발행)
Write-Host "`n[2] 부동산 등록 + 온체인 토큰 발행" -ForegroundColor Yellow
$prop = PostJson "/api/admin/properties" @{ name="강남 프라임빌딩"; address="서울 강남구"; appraisalValue=1000000000; totalSupply=10000; pricePerToken=100000 } $aT
$propId = $prop.body.id
Check "부동산 등록(201) + 한글명 보존 + 토큰 컨트랙트 배포" ($prop.status -eq 201 -and $prop.body.name -eq "강남 프라임빌딩" -and $prop.body.tokenContractAddress -match "^0x")

# [3] 투자자 가입
Write-Host "`n[3] 투자자 회원가입/로그인" -ForegroundColor Yellow
$em = "e2e$(Get-Random -Maximum 99999)@test.com"
$su = PostJson "/api/auth/signup" @{ email=$em; password="pass1234" }
$iT = $su.body.token
Check "투자자 가입(200) 및 INVESTOR 역할" ($su.status -eq 200 -and $su.body.role -eq "INVESTOR")

# [4] 접근통제
Write-Host "`n[4] 접근통제 (투자자 -> 관리자 API)" -ForegroundColor Yellow
$deny = GetJson "/api/admin/kyc" $iT
Check "투자자 토큰의 관리자 API 접근 403 차단" ($deny.status -eq 403)

# [5] KYC 신청 -> 반려 -> 재신청
Write-Host "`n[5] KYC 신청 -> 반려 -> 재신청" -ForegroundColor Yellow
$w1 = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
$k = PostJson "/api/kyc" @{ name="김투자"; idNumber="900101-1234567"; walletAddress=$w1 } $iT
Check "KYC 신청 PENDING" ($k.status -eq 200 -and $k.body.status -eq "PENDING")
$rej = PostJson "/api/admin/kyc/$($k.body.id)/reject" @{ reason="서류 미비" } $aT
Check "KYC 반려 REJECTED" ($rej.body.status -eq "REJECTED")
$re = PostJson "/api/kyc" @{ name="김투자"; idNumber="900101-1234567"; walletAddress=$w1 } $iT
Check "반려 후 재신청 PENDING (재신청 허용)" ($re.status -eq 200 -and $re.body.status -eq "PENDING")

# [6] KYC 미승인 매수 거부 (정합성/접근)
Write-Host "`n[6] 정합성 - KYC 미승인 상태 매수 거부" -ForegroundColor Yellow
$earlyBuy = PostJson "/api/investments" @{ propertyId=$propId; quantity=10 } $iT
Check "KYC 미승인(화이트리스트 미등록) 매수 403 차단" ($earlyBuy.status -eq 403)

# [7] KYC 승인 (온체인 화이트리스트)
Write-Host "`n[7] KYC 승인 (온체인 화이트리스트 등록)" -ForegroundColor Yellow
$ap = PostJson "/api/admin/kyc/$($k.body.id)/approve" @{} $aT
Check "KYC 승인 APPROVED + txHash 존재" ($ap.body.status -eq "APPROVED" -and $ap.body.whitelistTxHash -match "^0x")

# [8] 매수 + 보유현황
Write-Host "`n[8] 매수 (대행 온체인 이전) + 보유현황" -ForegroundColor Yellow
$buy = PostJson "/api/investments" @{ propertyId=$propId; quantity=300 } $iT
Check "매수 300개 성공" ($buy.status -eq 200 -and $buy.body.quantity -eq 300)
$hold = GetJson "/api/investments/me" $iT
Check "보유현황 300개 / 지분율 3%" ($hold.body[0].holdingQuantity -eq 300 -and [double]$hold.body[0].holdingRatio -eq 3)

# [9] 잔여 초과 매수 거부
Write-Host "`n[9] 정합성 - 잔여 수량 초과 매수 거부" -ForegroundColor Yellow
$over = PostJson "/api/investments" @{ propertyId=$propId; quantity=999999 } $iT
Check "잔여초과 매수 400 거부" ($over.status -eq 400)

# [10] 두번째 투자자 (배당 분배용)
Write-Host "`n[10] 두번째 투자자 매수 (100개)" -ForegroundColor Yellow
$em2 = "e2e$(Get-Random -Maximum 99999)@test.com"
$su2 = PostJson "/api/auth/signup" @{ email=$em2; password="pass1234" }
$iT2 = $su2.body.token
$w2 = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"
$k2 = PostJson "/api/kyc" @{ name="이투자"; idNumber="910202-2345678"; walletAddress=$w2 } $iT2
PostJson "/api/admin/kyc/$($k2.body.id)/approve" @{} $aT | Out-Null
$buy2 = PostJson "/api/investments" @{ propertyId=$propId; quantity=100 } $iT2
Check "두번째 투자자 100개 매수" ($buy2.status -eq 200 -and $buy2.body.quantity -eq 100)

# [11] 배당 집행
Write-Host "`n[11] 배당 집행 (100만원)" -ForegroundColor Yellow
$dist = PostJson "/api/admin/distributions" @{ propertyId=$propId; totalAmount=1000000 } $aT
Check "배당 회차=1, 수령자 2명" ($dist.body.round -eq 1 -and $dist.body.details.Count -eq 2)
$my1 = GetJson "/api/distributions/me" $iT
Check "투자자1 배당 30,000원 (지분 3%)" ([double]$my1.body.totalReceived -eq 30000)

# [12] 이상거래 탐지 + 동결/해제
Write-Host "`n[12] 이상거래 탐지(대량거래) + 동결/해제" -ForegroundColor Yellow
PostJson "/api/investments" @{ propertyId=$propId; quantity=1500 } $iT | Out-Null
Start-Sleep -Milliseconds 300
$anoms = GetJson "/api/admin/monitoring/anomalies?unreviewedOnly=true" $aT
$large = $anoms.body | Where-Object { $_.rule -eq "LARGE_TRADE" } | Select-Object -First 1
Check "대량거래(LARGE_TRADE) 탐지 + 이메일/부동산명 표시" ($large -ne $null -and $large.investorEmail -eq $em -and $large.propertyName -eq "강남 프라임빌딩")

$fz = PostJson "/api/admin/monitoring/anomalies/$($large.id)/freeze" @{ note="대량거래 의심" } $aT
Check "동결 조치 FROZEN + 담당자 기록" ($fz.body.action -eq "FROZEN" -and $fz.body.reviewedBy -eq $adminEmail)
$blocked = PostJson "/api/investments" @{ propertyId=$propId; quantity=10 } $iT
Check "동결 후 매수 차단 403" ($blocked.status -eq 403)
$acc = GetJson "/api/investments/me/account" $iT
Check "투자자 계정 frozen=true + 사유 노출" ($acc.body.frozen -eq $true -and $acc.body.reason -eq "대량거래 의심")
$uf = PostJson "/api/admin/monitoring/anomalies/$($large.id)/unfreeze" @{ note="소명 완료" } $aT
Check "동결 해제 UNFROZEN" ($uf.body.action -eq "UNFROZEN")
$acc2 = GetJson "/api/investments/me/account" $iT
Check "해제 후 계정 frozen=false" ($acc2.body.frozen -eq $false)
$reBuy = PostJson "/api/investments" @{ propertyId=$propId; quantity=10 } $iT
Check "해제 후 매수 재개 성공" ($reBuy.status -eq 200 -and $reBuy.body.quantity -eq 10)

# [13] 모니터링 요약
Write-Host "`n[13] 모니터링 요약 지표" -ForegroundColor Yellow
$sum = GetJson "/api/admin/monitoring/summary" $aT
Check "요약: 거래건수>0, 투자자 2명, 부동산 1건" ($sum.body.totalTransactions -gt 0 -and $sum.body.distinctInvestors -eq 2 -and $sum.body.propertyCount -eq 1)

# --- 결과 ---
Write-Host "`n========== 결과 ==========" -ForegroundColor Cyan
Write-Host ("PASS: {0} / FAIL: {1}" -f $pass, $fail) -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
if ($fail -eq 0) { Write-Host "모든 E2E 검증 통과" -ForegroundColor Green } else { exit 1 }
