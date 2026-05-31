package com.zenoeats.restauarantservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "menuItems")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "restaurant")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String address;
    private String phone;
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuItem> menuItems;
}
