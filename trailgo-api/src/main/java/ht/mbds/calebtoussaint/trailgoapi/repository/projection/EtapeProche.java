// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/projection/EtapeProche.java
package ht.mbds.calebtoussaint.trailgoapi.repository.projection;

public interface EtapeProche {
    Long getId();
    String getNom();
    String getDescription();
    Double getLatitude();
    Double getLongitude();
    Integer getOrdre();
    String getPhoto();
    Long getParcoursId();
    String getParcoursTitre();
    Double getDistanceM();
}
