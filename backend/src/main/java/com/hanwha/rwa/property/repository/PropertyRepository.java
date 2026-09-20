package com.hanwha.rwa.property.repository;

import com.hanwha.rwa.property.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByTokenContractAddress(String tokenContractAddress);
}
