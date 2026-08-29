import { Component, ElementRef, AfterViewInit, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import * as L from 'leaflet';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { ReportService } from '../../../core/services/report.service';
import { GeocodingService, RisultatoGeocoding } from '../../../core/services/geocoding.service';
import { CreateReportRequest, ReportCategory } from '../../../core/models/report.model';
import { segnapostoPerCategoria } from '../../../core/constants/map-marker';
import { CategoriaPipe } from '../../../core/pipes/etichette.pipe';

/**
 * Form di creazione/modifica di una segnalazione, con mappa Leaflet per
 * scegliere la posizione e geocoding per convertire testo ↔ coordinate.
 * Lo stesso componente serve sia "nuova segnalazione" sia "modifica":
 * a distinguerli è idInModifica, valorizzato solo se la rotta contiene un id.
 */

@Component({
  selector: 'app-create-report',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatCardModule, MatIconModule, CategoriaPipe
  ],
  templateUrl: './create-report.component.html',
  styleUrl: './create-report.component.scss'
})
export class CreateReportComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private resizeObserver: ResizeObserver | null = null;

  categorieDisponibili = Object.values(ReportCategory);

  /** Valorizzato solo sulla rotta di modifica: lo stesso form serve entrambi i casi. */
  idInModifica: number | null = null;

  titolo: string = '';
  descrizione: string = '';
  categoria: ReportCategory = ReportCategory.VIABILITY;
  indirizzo: string = '';
  latitudine: number | null = null;
  longitudine: number | null = null;

  fileSelezionati: File[] = [];
  salvataggioInCorso: boolean = false;
  caricamento: boolean = false;
  errore: string | null = null;

  risultatiIndirizzo: RisultatoGeocoding[] = [];
  ricercaIndirizzoInCorso: boolean = false;
  erroreRicercaIndirizzo: string | null = null;
  ricercaInversaInCorso: boolean = false;

  constructor(
    private reportService: ReportService,
    private geocodingService: GeocodingService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  /** Vero solo sulla rotta di modifica (idInModifica valorizzato in ngOnInit). */
  get inModifica(): boolean {
    return this.idInModifica !== null;
  }

  /**
   * Se la rotta contiene un id (rotta di modifica), lo legge dai parametri
   * e precarica la segnalazione esistente per riempire il form. Se manca,
   * il form resta vuoto: siamo nel caso "nuova segnalazione".
   */
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.idInModifica = Number(id);
    this.caricamento = true;

    this.reportService.findById(this.idInModifica).subscribe({
      next: (report) => {
        this.titolo = report.title;
        this.descrizione = report.description;
        this.categoria = report.category;
        this.indirizzo = report.address ?? '';
        this.caricamento = false;
        // La mappa può essere già pronta (ngAfterViewInit precede la
        // risposta HTTP): il segnaposto va messo appena si hanno le coordinate.
        this.impostaPosizione(report.latitude, report.longitude);
        this.map?.setView([report.latitude, report.longitude], 16);
      },
      error: () => {
        this.errore = 'Impossibile caricare la segnalazione da modificare.';
        this.caricamento = false;
      }
    });
  }

  /**
   * Il contenitore della mappa esiste solo dopo che Angular ha renderizzato
   * il template: per questo Leaflet si inizializza qui e non in ngOnInit.
   */
  ngAfterViewInit(): void {
    this.inizializzaMappa();
  }

  /** Libera mappa e observer alla distruzione del componente, per non lasciare listener orfani. */
  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.map?.remove();
  }

  /**
   * Crea la mappa centrata su Lecce, aggiunge le tile OpenStreetMap e
   * collega il click sulla mappa sia a impostaPosizione (segnaposto) sia
   * alla geocodifica inversa (riempie l'indirizzo in automatico).
   */
  private inizializzaMappa(): void {
    this.map = L.map(this.mapContainer.nativeElement).setView([40.3515, 18.1750], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      this.impostaPosizione(event.latlng.lat, event.latlng.lng);
      this.geocodificaInversa(event.latlng.lat, event.latlng.lng);
    });

// Mantiene sincronizzati gli offset in pixel di Leaflet con i cambi di layout responsive.    this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    const resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    resizeObserver.observe(this.mapContainer.nativeElement);
    this.resizeObserver = resizeObserver;
  }

  /**
   * Aggiorna le coordinate del form e il segnaposto sulla mappa: se esiste
   * già lo sposta, altrimenti lo crea la prima volta. Usata sia dal click
   * sulla mappa sia dalla scelta di un risultato di ricerca indirizzo.
   */
  private impostaPosizione(lat: number, lng: number): void {
    this.latitudine = lat;
    this.longitudine = lng;

    if (this.marker) {
      this.marker.setLatLng([lat, lng]);
    } else {
      this.marker = L.marker([lat, lng], {
        icon: segnapostoPerCategoria(this.categoria)
      }).addTo(this.map!);
    }
  }

  /**
   * Cerca l'indirizzo digitato dall'utente e popola risultatiIndirizzo con
   * i candidati: la scelta finale avviene con scegliRisultato.
   */
  cercaIndirizzo(): void {
    const testo = this.indirizzo.trim();
    if (!testo || this.ricercaIndirizzoInCorso) return;

    this.ricercaIndirizzoInCorso = true;
    this.erroreRicercaIndirizzo = null;
    this.risultatiIndirizzo = [];

    this.geocodingService.cerca(testo).subscribe(risultati => {
      this.ricercaIndirizzoInCorso = false;
      if (risultati.length === 0) {
        this.erroreRicercaIndirizzo = 'Nessun indirizzo trovato.';
        return;
      }
      this.risultatiIndirizzo = risultati;
    });
  }


  /**
   * L'utente conferma uno dei risultati proposti: riempie l'indirizzo,
   * svuota l'elenco dei candidati e sposta mappa e segnaposto sulla
   * posizione scelta.
   */
  scegliRisultato(risultato: RisultatoGeocoding): void {
    this.indirizzo = risultato.indirizzo;
    this.risultatiIndirizzo = [];
    this.impostaPosizione(risultato.lat, risultato.lng);
    this.map?.setView([risultato.lat, risultato.lng], 16);
  }

  /** Riempie il campo indirizzo dopo un click sulla mappa, senza sovrascrivere una ricerca in corso. */
  private geocodificaInversa(lat: number, lng: number): void {
    this.ricercaInversaInCorso = true;
    this.geocodingService.inversa(lat, lng).subscribe(indirizzo => {
      this.ricercaInversaInCorso = false;
      if (indirizzo) {
        this.indirizzo = indirizzo;
      }
    });
  }

  /** Salva i file scelti dall'input; l'upload vero avviene solo dopo il salvataggio della segnalazione. */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.fileSelezionati = Array.from(input.files);
    }
  }

  /**
   * Valida i campi obbligatori (posizione inclusa: senza un click sulla
   * mappa latitudine/longitudine restano null) e poi crea o aggiorna la
   * segnalazione a seconda di inModifica. In entrambi i casi il passo
   * successivo è lo stesso: caricare le foto e reindirizzare.
   */
  invia(): void {
    this.errore = null;

    if (!this.titolo || !this.descrizione || this.latitudine === null || this.longitudine === null) {
      this.errore = 'Compila tutti i campi obbligatori e seleziona una posizione sulla mappa.';
      return;
    }

    const dto: CreateReportRequest = {
      title: this.titolo,
      description: this.descrizione,
      category: this.categoria,
      latitude: this.latitudine,
      longitude: this.longitudine,
      address: this.indirizzo
    };

    this.salvataggioInCorso = true;

    if (this.inModifica) {
      this.reportService.update(this.idInModifica!, dto).subscribe({
        next: (report) => this.caricaFotoESpostati(report.id),
        error: (err) => {
          // Il backend rifiuta la modifica se la segnalazione è già stata
          // presa in carico: quel messaggio dice all'utente cos'è successo.
          this.errore = err.error?.message ?? 'Impossibile aggiornare la segnalazione.';
          this.salvataggioInCorso = false;
        }
      });
      return;
    }

    this.reportService.createReport(dto).subscribe({
      next: (report) => this.caricaFotoESpostati(report.id),
      error: () => {
        this.errore = 'Impossibile creare la segnalazione.';
        this.salvataggioInCorso = false;
      }
    });
  }

  /**
   * Carica ogni foto selezionata in parallelo e aspetta che tutte finiscano
   * — con successo o con errore, non importa quale — prima di andare al
   * dettaglio della segnalazione: un errore su una singola foto non deve
   * bloccare l'utente sulla pagina del form.
   */
  private caricaFotoESpostati(reportId: number): void {
    if (this.fileSelezionati.length === 0) {
      this.router.navigate(['/reports', reportId]);
      return;
    }

    let completati = 0;
    const totale = this.fileSelezionati.length;

    this.fileSelezionati.forEach(file => {
      this.reportService.uploadPhoto(reportId, file).subscribe({
        next: () => { completati++; if (completati === totale) this.router.navigate(['/reports', reportId]); },
        error: () => { completati++; if (completati === totale) this.router.navigate(['/reports', reportId]); }
      });
    });
  }
}
