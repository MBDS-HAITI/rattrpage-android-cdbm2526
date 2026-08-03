// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/projection/PoiProche.java
package ht.mbds.calebtoussaint.trailgoapi.repository.projection;

public interface PoiProche {
    Long getId();
    String getTitre();
    String getCategorie();
    String getAdresse();
    Double getLatitude();
    Double getLongitude();
    Integer getRayonProximiteM();
    Double getDistanceM();
}
