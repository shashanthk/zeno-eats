package com.zenoeats.restauarantservice.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RestaurantNotFoundException extends RuntimeException {

    private Long id;

    public RestaurantNotFoundException(Long id) {
        super("Restaurant not found with id: " + id);
    }
}
