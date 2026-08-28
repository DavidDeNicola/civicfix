package org.civicfix.app.doc;

/**
 * Nomi e descrizioni dei gruppi in cui Swagger raccoglie gli endpoint.
 *
 * I gruppi seguono il destinatario (chi può chiamare quella rotta), non il
 * controller che la espone: ReportController serve tutti e quattro i pubblici,
 * e vederli mescolati in un unico blocco renderebbe la pagina illeggibile.
 */
public final class SwaggerTags {

    private SwaggerTags() {
    }

    public static final String PUBBLICO = "Pubblico";
    public static final String PUBBLICO_DESC =
            "Rotte accessibili senza autenticazione: registrazione, accesso, recupero password e consultazione delle segnalazioni.";

    public static final String CITTADINO = "Cittadino";
    public static final String CITTADINO_DESC =
            "Rotte per l'utente autenticato: apertura, modifica ed eliminazione delle proprie segnalazioni, sostegni, commenti e foto.";

    public static final String OPERATORE = "Operatore";
    public static final String OPERATORE_DESC =
            "Rotte per l'operatore assegnato alla segnalazione: avanzamento dello stato di lavorazione.";

    public static final String ADMIN = "Amministrazione";
    public static final String ADMIN_DESC =
            "Rotte riservate agli amministratori: utenti, team, assegnazioni, priorità e statistiche.";

    public static final String ERRORE_DESC = "Dettaglio dell'errore restituito dal server.";
}
