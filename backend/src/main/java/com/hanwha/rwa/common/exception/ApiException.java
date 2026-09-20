package com.hanwha.rwa.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 표준 예외 (design.md 7)
 * HTTP 상태와 사용자 메시지를 함께 담아 일관된 오류 응답으로 변환됩니다.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
