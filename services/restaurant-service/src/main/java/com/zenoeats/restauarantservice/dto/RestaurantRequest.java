package com.zenoeats.restauarantservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestaurantRequest {

    @NotBlank
    private String name;
    private String address;
    private String phone;
}
