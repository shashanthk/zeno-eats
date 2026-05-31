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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Override
    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        Restaurant restaurant = restaurantMapper.toEntity(request);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
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
        return restaurantMapper.toResponse(
            restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id))
        );
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request) {
        Restaurant existing = restaurantRepository.findById(id)
            .orElseThrow(() -> new RestaurantNotFoundException(id));
        restaurantMapper.updateEntity(request, existing);
        // no save() needed — Hibernate dirty-checks the managed entity at commit
        return restaurantMapper.toResponse(existing);
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
            .orElseThrow(() -> new RestaurantNotFoundException(id));
        restaurantRepository.delete(restaurant);
    }
}
