package com.zenoeats.restauarantservice.service;

import com.zenoeats.restauarantservice.dto.RestaurantRequest;
import com.zenoeats.restauarantservice.dto.RestaurantResponse;

import java.util.List;

public interface RestaurantService {

    RestaurantResponse createRestaurant(RestaurantRequest request);
    List<RestaurantResponse> getAllRestaurants();
    RestaurantResponse getRestaurantById(Long id);
    RestaurantResponse updateRestaurant(Long id, RestaurantRequest request);
    void deleteRestaurant(Long id);
}
