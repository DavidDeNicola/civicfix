import { MatPaginatorIntl } from '@angular/material/paginator';

/**
 * Etichette italiane per il paginatore di Angular Material, che di suo parla
 * inglese ("Items per page", "of"). Va fornito come provider al posto di
 * MatPaginatorIntl.
 */
export function paginatorItaliano(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();

  intl.itemsPerPageLabel = 'Elementi per pagina';
  intl.nextPageLabel = 'Pagina successiva';
  intl.previousPageLabel = 'Pagina precedente';
  intl.firstPageLabel = 'Prima pagina';
  intl.lastPageLabel = 'Ultima pagina';

  intl.getRangeLabel = (page: number, pageSize: number, length: number) => {
    if (length === 0 || pageSize === 0) {
      return `0 di ${length}`;
    }

    const totale = Math.max(length, 0);
    const inizio = page * pageSize;
    // L'ultima pagina è quasi sempre parziale: senza questo controllo
    // mostrerebbe un intervallo più ampio del numero reale di elementi.
    const fine = Math.min(inizio + pageSize, totale);

    return `${inizio + 1} – ${fine} di ${totale}`;
  };

  return intl;
}
