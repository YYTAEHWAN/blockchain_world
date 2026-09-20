package com.hanwha.rwa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * RWA 부동산 조각투자 플랫폼 백엔드 진입점.
 *
 * 실행: ./gradlew bootRun
 * 기본 프로파일은 H2 인메모리 DB를 사용하여 별도 DB 설치 없이 구동됩니다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RwaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RwaApplication.class, args);
    }
}
