// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/ZoneGeographiqueRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.ZoneGeographique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneGeographiqueRepository extends JpaRepository<ZoneGeographique, Long> {
    // Les requetes spatiales (ST_Within, ST_Intersects) viendront plus tard.
}
