package com.team5.auction.user.dto;

import com.team5.auction.common.entity.Address;
import com.team5.auction.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressDto(

        @NotBlank(message = "받는 사람 이름을 입력해 주세요.")
        @Size(max = 50, message = "받는 사람 이름은 50자 이하여야 합니다.")
        String recipientName,

        @NotBlank(message = "받는 사람 전화번호를 입력해 주세요.")
        @Pattern(regexp = ValidationPatterns.PHONE, message = "전화번호는 010-1234-5678 형식이어야 합니다.")
        String recipientPhone,

        @NotBlank(message = "우편 번호를 입력해 주세요.")
        @Pattern(regexp = ValidationPatterns.ZIP_CODE, message = "우편번호는 5자리 숫자여야 합니다.")
        String zipCode,

        @NotBlank(message = "주소를 입력해 주세요.")
        @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
        String address1,

        @Size(max = 200, message = "상세주소는 200자 이하여야 합니다.")
        String address2
) {
    public static AddressDto from(Address address){
        if(address==null){
            return null;
        }
        return new AddressDto(
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getZipCode(),
                address.getAddress1(),
                address.getAddress2()
        );
    }

    public Address toAddress(){
        return new Address(recipientName, recipientPhone, zipCode, address1, address2);
    }
}
