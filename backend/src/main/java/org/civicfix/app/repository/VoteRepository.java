package org.civicfix.app.repository;

import org.civicfix.app.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByReportIdAndUserId(Long reportId, Long userId);
    long countByReportId(Long reportId);
    long deleteByReportIdAndUserId(Long reportId, Long userId);

    /** Usata quando la segnalazione viene eliminata: i voti non sono in cascata. */
    void deleteByReportId(Long reportId);

    /**
     * Conteggio dei voti per un blocco di segnalazioni in una sola query:
     * contarli uno per uno mentre si costruisce l'elenco significherebbe
     * un'interrogazione per riga.
     */
    @Query("select v.report.id, count(v) from Vote v where v.report.id in :reportIds group by v.report.id")
    List<Object[]> contaPerSegnalazioni(@Param("reportIds") Collection<Long> reportIds);

    /** Segnalazioni, fra quelle indicate, già votate dall'utente corrente. */
    @Query("select v.report.id from Vote v where v.user.id = :userId and v.report.id in :reportIds")
    List<Long> segnalazioniVotateDa(@Param("userId") Long userId,
                                    @Param("reportIds") Collection<Long> reportIds);
}
