package org.civicfix.app.repository.specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.ReportStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class ReportSpecifications {

    /** Lunghezza di un grado di latitudine, praticamente costante. */
    private static final double KM_PER_GRADO_LAT = 110.57;

    /** Lunghezza di un grado di longitudine all'equatore; si accorcia salendo di latitudine. */
    private static final double KM_PER_GRADO_LNG_EQUATORE = 111.32;

    public static Specification<Report> hasCategory(ReportCategory category){
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Report> hasStatus(ReportStatus status){
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Nessun valore significa "nessun vincolo", non "nessun risultato". */
    public static Specification<Report> hasCategoryIn(List<ReportCategory> categories){
        return (root, query, cb) ->
                (categories == null || categories.isEmpty()) ? null : root.get("category").in(categories);
    }

    public static Specification<Report> hasStatusIn(List<ReportStatus> statuses){
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    public static Specification<Report> titleContains(String testo){
        if (testo == null || testo.isBlank()) {
            return (root, query, cb) -> null;
        }
        // Il confronto è reso insensibile alle maiuscole; i caratteri jolly di
        // LIKE vengono neutralizzati perché il testo arriva dall'utente.
        String termine = "%" + testo.trim().toLowerCase()
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), termine, '!');
    }

    /** Estremi inclusivi: la data "a" copre l'intera giornata. */
    public static Specification<Report> createdBetween(LocalDate da, LocalDate a){
        return (root, query, cb) -> {
            if (da == null && a == null) return null;
            if (da == null) return cb.lessThan(root.get("createdAt"), a.plusDays(1).atStartOfDay());
            if (a == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), da.atStartOfDay());
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), da.atStartOfDay()),
                    cb.lessThan(root.get("createdAt"), a.plusDays(1).atStartOfDay())
            );
        };
    }

    /**
     * Segnalazioni entro un raggio dal punto indicato.
     *
     * <p>Il filtro è composto da due parti: un riquadro, che scarta in fretta
     * la maggior parte delle righe con semplici confronti, e la distanza vera
     * calcolata sul piano locale. Senza la seconda, gli angoli del riquadro
     * resterebbero inclusi pur trovandosi fino al 41% più lontani del raggio
     * chiesto. L'approssimazione piana è accurata entro qualche decina di
     * chilometri, ben oltre l'uso previsto di "vicino a me".
     */
    public static Specification<Report> nearLocation(Double lat, Double lng, Double radiusKm){

        // Senza tutti e tre i parametri il filtro non si applica.
        if (lat == null || lng == null || radiusKm == null) {
            return (root, query, criteriaBuilder) -> null;
        }

        double kmPerGradoLng = KM_PER_GRADO_LNG_EQUATORE * Math.cos(Math.toRadians(lat));
        // Vicino ai poli il valore tende a zero: si evita la divisione per zero.
        kmPerGradoLng = Math.max(kmPerGradoLng, 0.001);

        double deltaLat = radiusKm / KM_PER_GRADO_LAT;
        double deltaLng = radiusKm / kmPerGradoLng;
        double kmPerGradoLngFinale = kmPerGradoLng;

        return (root, query, cb) -> {
            Expression<Double> latitudine = root.get("latitude");
            Expression<Double> longitudine = root.get("longitude");

            Predicate riquadro = cb.and(
                    cb.between(latitudine, lat - deltaLat, lat + deltaLat),
                    cb.between(longitudine, lng - deltaLng, lng + deltaLng)
            );

            // Distanza in chilometri scomposta nei due assi, confrontata al
            // quadrato per non calcolare una radice inutile.
            Expression<Double> distanzaNord = cb.prod(
                    cb.diff(latitudine, cb.literal(lat)), cb.literal(KM_PER_GRADO_LAT));
            Expression<Double> distanzaEst = cb.prod(
                    cb.diff(longitudine, cb.literal(lng)), cb.literal(kmPerGradoLngFinale));

            Predicate dentroIlCerchio = cb.lessThanOrEqualTo(
                    cb.sum(cb.prod(distanzaNord, distanzaNord), cb.prod(distanzaEst, distanzaEst)),
                    radiusKm * radiusKm
            );

            return cb.and(riquadro, dentroIlCerchio);
        };
    }
}
