package org.civicfix.app.repository.specification;

import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.ReportStatus;
import org.springframework.data.jpa.domain.Specification;

public class ReportSpecifications {

    public static Specification<Report> hasCategory(ReportCategory category){
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Report> hasStatus(ReportStatus status){
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Report> nearLocation(Double lat, Double lng, Double radiusKm){

        //se i parametri non sono presenti, non si applica il filtro (null)
        if (lat == null || lng == null | radiusKm == null){
            return (root, query, criteriaBuilder) -> null;
        }

        //formula di Haversine approssimata, poi rifinita in service
        double delta = radiusKm / 111.0;
        return (root, query, cb) -> cb.and(
                cb.between(root.get("latitude"), lat - delta, lat + delta),
                cb.between(root.get("longitude"), lng - delta, lng + delta)
        );
    }
}
