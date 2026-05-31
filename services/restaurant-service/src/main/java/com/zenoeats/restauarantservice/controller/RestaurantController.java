package com.zenoeats.restauarantservice.controller;

import com.zenoeats.restauarantservice.dto.RestaurantRequest;
import com.zenoeats.restauarantservice.dto.RestaurantResponse;
import com.zenoeats.restauarantservice.service.RestaurantService;
import com.zenoeats.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
        @Valid @RequestBody RestaurantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success("Restaurant created", restaurantService.createRestaurant(request))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getAllRestaurants() {
        return ResponseEntity.ok(
            ApiResponse.success(
                restaurantService.getAllRestaurants()
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success(
                restaurantService.getRestaurantById(id)
            )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
        @PathVariable Long id,
        @Valid @RequestBody RestaurantRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "Restaurant updated",
                restaurantService.updateRestaurant(id, request)
            )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

}
