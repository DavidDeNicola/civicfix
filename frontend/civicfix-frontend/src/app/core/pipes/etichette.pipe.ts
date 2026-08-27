import { Pipe, PipeTransform } from '@angular/core';

// Gli enum arrivano dal backend in inglese e restano tali nelle chiamate API:
// qui si traduce solo ciò che l'utente legge, senza toccare il contratto REST.

export const ETICHETTE_STATO: Record<string, string> = {
  PENDING: 'In attesa',
  IN_PROGRESS: 'In corso',
  RESOLVED: 'Risolta',
  REJECTED: 'Respinta'
};

export const ETICHETTE_PRIORITA: Record<string, string> = {
  LOW: 'Bassa',
  NORMAL: 'Normale',
  HIGH: 'Alta',
  URGENT: 'Urgente'
};

export const ETICHETTE_CATEGORIA: Record<string, string> = {
  VIABILITY: 'Viabilità',
  LIGHTING: 'Illuminazione',
  WASTE: 'Rifiuti',
  GREEN_AREAS: 'Aree verdi',
  WATER: 'Acqua',
  OTHER: 'Altro'
};

export const ETICHETTE_RUOLO: Record<string, string> = {
  CITIZEN: 'Cittadino',
  OPERATOR: 'Operatore',
  ADMIN: 'Amministratore'
};

/** Plurale usato nei titoli di sezione dell'area di gestione. */
export const ETICHETTE_RUOLO_PLURALE: Record<string, string> = {
  CITIZEN: 'Cittadini',
  OPERATOR: 'Operatori',
  ADMIN: 'Amministratori'
};

function traduci(mappa: Record<string, string>, valore: string | null | undefined): string {
  if (!valore) return '';
  // Se compare un valore non previsto si mostra l'originale invece di una
  // stringa vuota: meglio un'etichetta in inglese che un'informazione persa.
  return mappa[valore] ?? valore;
}

@Pipe({ name: 'stato', standalone: true })
export class StatoPipe implements PipeTransform {
  transform(valore: string | null | undefined): string {
    return traduci(ETICHETTE_STATO, valore);
  }
}

@Pipe({ name: 'priorita', standalone: true })
export class PrioritaPipe implements PipeTransform {
  transform(valore: string | null | undefined): string {
    return traduci(ETICHETTE_PRIORITA, valore);
  }
}

@Pipe({ name: 'categoria', standalone: true })
export class CategoriaPipe implements PipeTransform {
  transform(valore: string | null | undefined): string {
    return traduci(ETICHETTE_CATEGORIA, valore);
  }
}

@Pipe({ name: 'ruolo', standalone: true })
export class RuoloPipe implements PipeTransform {
  transform(valore: string | null | undefined): string {
    return traduci(ETICHETTE_RUOLO, valore);
  }
}
