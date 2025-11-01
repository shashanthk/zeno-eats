package com.zenoeats.restauarantservice.repository;

import com.zenoeats.restauarantservice.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
