package com.shopsphere.shopsphere_web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter // <-- 이 부분이 있어야 getMethod(), getPaymentKey()가 자동 생성됩니다.
@Setter
public class TossPaymentResponseDTO {
    private String version;
    private String paymentKey;
    private String orderId; // 우리가 토스에 보낸 orderId (토스에서 그대로 반환)
    private String orderName;
    private String method; // 결제 수단 (예: 카드, 가상계좌, 간편결제 등)
    private Long totalAmount;
    private String status; // 결제 상태 (예: READY, DONE, CANCELED 등)
    private String requestedAt; // 결제 요청 일시
    private String approvedAt;  // 결제 승인 일시

    @JsonProperty("checkout")
    private Checkout checkout; // 결제창 URL 정보 (requestPayment 응답에만 있음)

    // Checkout 내부 클래스
    @Getter
    @Setter
    public static class Checkout {
        private String url;
    }

    // 카드 결제 상세 정보 (필요시 사용)
    @Getter
    @Setter
    public static class Card {
        private String issuerCode; // 발급사 코드
        private String acquirerCode; // 매입사 코드
        private String number; // 마스킹된 카드 번호 (예: 123456******7890)
        private String cardType; // 카드 종류 (신용, 체크, 기프트)
        private String ownerType; // 개인, 법인
        private String approveNo; // 승인 번호
        private Boolean useCardPoint; // 카드 포인트 사용 여부
        private String interestFreeInstallment; // 무이자 할부 여부
        private String installmentPlanMonths; // 할부 개월 수
        // ... (더 많은 필드는 토스페이먼츠 API 문서 참고)
    }

    // 가상계좌 정보 (필요시 사용)
    @Getter
    @Setter
    public static class VirtualAccount {
        private String accountType; // 계좌 종류 (일반, 고정)
        private String accountNumber; // 가상계좌 번호
        private String bankCode; // 은행 코드
        private String customerName; // 예금주명
        private LocalDateTime dueDate; // 입금 마감 시간
        // ...
    }

}
