package com.team5.auction.user.dto;

import com.team5.auction.common.entity.Address;

public record AddressDto(
        String recipientName,
        String recipientPhone,
        String zipCode,
        String address1,
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
