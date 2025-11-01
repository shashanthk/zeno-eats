package com.zenoeats.restauarantservice.service.impl;

import com.zenoeats.restauarantservice.dto.RestaurantRequest;
import com.zenoeats.restauarantservice.dto.RestaurantResponse;
import com.zenoeats.restauarantservice.entity.Restaurant;
import com.zenoeats.restauarantservice.exception.RestaurantNotFoundException;
import com.zenoeats.restauarantservice.mapper.RestaurantMapper;
import com.zenoeats.restauarantservice.repository.RestaurantRepository;
import com.zenoeats.restauarantservice.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Override
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        Restaurant restaurant = restaurantMapper.toEntity(request);
        Restaurant saved = restaurantRepository.save(restaurant);
        return restaurantMapper.toResponse(saved);
    }

    @Override
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll()
            .stream()
            .map(restaurantMapper::toResponse)
            .toList();
    }

    @Override
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
            .orElseThrow(() -> new RestaurantNotFoundException(id));
        return restaurantMapper.toResponse(restaurant);
    }

    @Override
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request) {
        Restaurant existing = restaurantRepository.findById(id)
            .orElseThrow(() -> new RestaurantNotFoundException(id));

        existing.setName(request.getName());
        existing.setAddress(request.getAddress());
        existing.setPhone(request.getPhone());

        return restaurantMapper.toResponse(restaurantRepository.save(existing));
    }

    @Override
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
            .orElseThrow(() -> new RestaurantNotFoundException(id));
        restaurantRepository.delete(restaurant);
    }
}
