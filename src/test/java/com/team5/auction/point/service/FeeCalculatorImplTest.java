package com.team5.auction.point.service;

import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.config.AuctionPolicyProperties.FeeTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** FeeCalculatorImpl (계약서 3.2). application.yml과 같은 구간(99,999원까지 5%, 그 위는 3%)으로 계산한다 */
class FeeCalculatorImplTest {

    private final FeeCalculatorImpl feeCalculator = new FeeCalculatorImpl(properties());

    @Test
    void 금액_99999원은_5퍼센트이고_원_단위_미만은_버린다() {
        Fee fee = feeCalculator.calculate(99_999); // 4,999.95원 → 4,999원

        assertFee(fee, "0.05", 4_999, 95_000);
    }

    @Test
    void 금액_100000원은_3퍼센트이고_수수료는_정확히_3000원이다() {
        Fee fee = feeCalculator.calculate(100_000); // 3% 구간이 시작되는 금액 (경계값 테스트)

        assertFee(fee, "0.03", 3_000, 97_000);
    }

    @Test
    void 계약서_예시_15000원은_수수료_750원_정산금_14250원이다() {
        Fee fee = feeCalculator.calculate(15_000);

        assertFee(fee, "0.05", 750, 14_250);
    }

    /** application.yml과 같은 수수료 구간. 이 테스트는 수수료 설정만 써서 나머지는 null */
    private static AuctionPolicyProperties properties() {
        AuctionPolicyProperties.Fee fee = new AuctionPolicyProperties.Fee(List.of(
                new FeeTier(99_999L, new BigDecimal("0.05")),
                new FeeTier(null, new BigDecimal("0.03"))));
        return new AuctionPolicyProperties(fee, null, null, null, null);
    }

    private static void assertFee(Fee fee, String rate, long feeAmount, long settleAmount) {
        assertThat(fee.rate()).isEqualByComparingTo(new BigDecimal(rate));
        assertThat(fee.feeAmount()).isEqualTo(feeAmount);
        assertThat(fee.settleAmount()).isEqualTo(settleAmount);
    }
}
