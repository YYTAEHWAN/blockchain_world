package com.hanwha.rwa.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이상거래 탐지 임계치 설정 (application.yml 의 app.monitoring.*) (design.md 4.5)
 *
 * - largeTradeRatioPercent: 1회 거래 수량이 부동산 총량의 이 비율(%)을 초과하면 대량 거래(HIGH)
 * - rapidRepeatWindowSeconds: 단기 반복 판정 시간창(초)
 * - rapidRepeatThreshold: 시간창 내 거래 횟수가 이 값을 초과하면 단기 반복(MEDIUM)
 */
@ConfigurationProperties(prefix = "app.monitoring")
public class MonitoringProperties {

    /** 대량 거래 임계 비율(%). 기본 10% */
    private double largeTradeRatioPercent = 10.0;

    /** 단기 반복 시간창(초). 기본 60초 */
    private long rapidRepeatWindowSeconds = 60;

    /** 단기 반복 횟수 임계치. 기본 3회 초과 */
    private int rapidRepeatThreshold = 3;

    public double getLargeTradeRatioPercent() { return largeTradeRatioPercent; }
    public void setLargeTradeRatioPercent(double largeTradeRatioPercent) {
        this.largeTradeRatioPercent = largeTradeRatioPercent;
    }

    public long getRapidRepeatWindowSeconds() { return rapidRepeatWindowSeconds; }
    public void setRapidRepeatWindowSeconds(long rapidRepeatWindowSeconds) {
        this.rapidRepeatWindowSeconds = rapidRepeatWindowSeconds;
    }

    public int getRapidRepeatThreshold() { return rapidRepeatThreshold; }
    public void setRapidRepeatThreshold(int rapidRepeatThreshold) {
        this.rapidRepeatThreshold = rapidRepeatThreshold;
    }
}
