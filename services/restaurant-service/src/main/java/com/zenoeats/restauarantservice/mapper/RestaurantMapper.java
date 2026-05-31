package com.zenoeats.restauarantservice.mapper;

import com.zenoeats.restauarantservice.dto.RestaurantRequest;
import com.zenoeats.restauarantservice.dto.RestaurantResponse;
import com.zenoeats.restauarantservice.entity.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {

    Restaurant toEntity(RestaurantRequest request);
    RestaurantResponse toResponse(Restaurant restaurant);

    void updateEntity(RestaurantRequest request, @MappingTarget Restaurant restaurant);
}
