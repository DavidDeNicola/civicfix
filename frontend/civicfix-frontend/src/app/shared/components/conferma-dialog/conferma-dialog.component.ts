import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfermaDialogData {
  titolo: string;
  messaggio: string;
  /** Testo del pulsante di conferma; per default "Conferma". */
  conferma?: string;
  /** Colora di rosso l'azione quando è distruttiva. */
  pericolosa?: boolean;
}

@Component({
  selector: 'app-conferma-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.titolo }}</h2>
    <mat-dialog-content>
      <p class="messaggio">{{ data.messaggio }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="ref.close(false)">Annulla</button>
      <button mat-flat-button
              [color]="data.pericolosa ? 'warn' : 'primary'"
              (click)="ref.close(true)">
        {{ data.conferma ?? 'Conferma' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .messaggio {
      margin: 0;
      max-width: min(420px, 76vw);
      line-height: 1.55;
      color: var(--color-muted-foreground);
    }
  `]
})
export class ConfermaDialogComponent {
  constructor(
    public ref: MatDialogRef<ConfermaDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: ConfermaDialogData
  ) {}
}
