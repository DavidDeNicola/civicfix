import * as L from 'leaflet';

// Leaflet's default marker resolves its icons as bundled image assets
// (marker-icon-2x.png / marker-shadow.png), which the Angular build does not
// emit — on retina screens that 404s and the browser paints a broken-image
// placeholder instead of a pin. A divIcon with inline SVG has no asset
// dependency at all and can be tinted per category.
const PIN_SVG = (colore: string) => `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 32" width="26" height="35" aria-hidden="true">
  <path d="M12 0C5.373 0 0 5.373 0 12c0 9 12 20 12 20s12-11 12-20c0-6.627-5.373-12-12-12z"
        fill="${colore}" stroke="#ffffff" stroke-width="1.5"/>
  <circle cx="12" cy="12" r="4.5" fill="#ffffff"/>
</svg>`;

/** Map pin anchored at its tip, tinted with the given colour. */
export function creaSegnaposto(colore: string = '#0369a1'): L.DivIcon {
  return L.divIcon({
    html: PIN_SVG(colore),
    className: 'civicfix-marker',
    iconSize: [26, 35],
    iconAnchor: [13, 35],
    popupAnchor: [0, -35]
  });
}

/** Marker colour per report category, matching the card accent colours. */
export const COLORI_CATEGORIA: Record<string, string> = {
  VIABILITY: '#d97706',
  LIGHTING: '#a16207',
  WASTE: '#78716c',
  GREEN_AREAS: '#15803d',
  WATER: '#0369a1',
  OTHER: '#7c3aed'
};

export function segnapostoPerCategoria(categoria: string): L.DivIcon {
  return creaSegnaposto(COLORI_CATEGORIA[categoria] ?? '#0369a1');
}
