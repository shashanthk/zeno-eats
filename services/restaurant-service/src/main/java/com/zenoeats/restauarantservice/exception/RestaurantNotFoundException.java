package com.zenoeats.restauarantservice.exception;

import lombok.Getter;

@Getter
public class RestaurantNotFoundException extends RuntimeException {

    private final Long id;

    public RestaurantNotFoundException(Long id) {
        super("Restaurant not found with id: " + id);
        this.id = id;
    }
}
