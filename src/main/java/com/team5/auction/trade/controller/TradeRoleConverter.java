package com.team5.auction.trade.controller;

import com.team5.auction.trade.domain.TradeRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** 쿼리 파라미터 role(buyer|seller)을 대소문자 구분 없이 TradeRole로 변환합니다. */
@Component
public class TradeRoleConverter implements Converter<String, TradeRole> {
    @Override
    public TradeRole convert(String source) {
        return TradeRole.valueOf(source.trim().toUpperCase());
    }
}
