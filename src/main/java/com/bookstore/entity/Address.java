package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_street", nullable = false, length = 255)
    private String street;

    @Column(name = "address_number", nullable = false, length = 20)
    private String number;

    @Column(name = "address_complement", length = 100)
    private String complement;

    @Column(name = "address_neighborhood", nullable = false, length = 100)
    private String neighborhood;

    @Column(name = "address_city", nullable = false, length = 100)
    private String city;

    @Column(name = "address_state", nullable = false, length = 2)
    private String state;

    @Column(name = "address_zip_code", nullable = false, length = 9)
    private String zipCode;
}
