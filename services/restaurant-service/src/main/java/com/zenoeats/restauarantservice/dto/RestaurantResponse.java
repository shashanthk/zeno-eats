package com.zenoeats.restauarantservice.dto;

import lombok.Data;

@Data
public class RestaurantResponse {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private Boolean active;
}
