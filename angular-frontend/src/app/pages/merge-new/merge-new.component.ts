import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MergeService } from '../../core/services/merge.service';
import { MergeSource } from '../../models/merge-source.model';

@Component({
  selector: 'app-merge-new',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="padding:2rem;">
      <h1>Nueva fusión de calendarios</h1>

      <div style="max-width:600px; margin-top:1.5rem; display:flex; flex-direction:column; gap:1rem;">

        <h2>Calendarios fuente</h2>

        <div *ngFor="let s of sources; let i = index"
             style="border:1px solid #ccc; padding:1rem; border-radius:8px; display:flex; flex-direction:column; gap:0.5rem;">
          <label>Proveedor</label>
          <select [(ngModel)]="s.provider">
            <option value="google">Google Calendar</option>
            <option value="caldav">CalDAV</option>
          </select>

          <label>ID de cuenta</label>
          <input [(ngModel)]="s.accountId" placeholder="ej: google-1234567890" />

          <label>ID de calendario</label>
          <input [(ngModel)]="s.calendarId" placeholder="ej: primary" />

          <label>Prefijo en títulos (opcional)</label>
          <input [(ngModel)]="s.prefix" placeholder="ej: [Trabajo]" />

          <button (click)="removeSource(i)" style="color:red; width:fit-content;">
            Eliminar fuente
          </button>
        </div>

        <button (click)="addSource()">+ Añadir calendario fuente</button>

        <h2>Configuración</h2>

        <label>Máximo de eventos por calendario</label>
        <input type="number" [(ngModel)]="maxResults" placeholder="ej: 100" />

        <label>Frecuencia de refresco (minutos)</label>
        <input type="number" [(ngModel)]="refreshInterval" placeholder="ej: 60" />

        <button (click)="create()" [disabled]="loading" style="margin-top:1rem;">
          {{ loading ? 'Creando...' : 'Crear fusión' }}
        </button>

        <p *ngIf="error" style="color:red;">{{ error }}</p>

        <div *ngIf="icsUrl" style="border:1px solid green; padding:1rem; border-radius:8px;">
          <p><strong>Fusión creada.</strong> URL ICS para suscribirte:</p>
          <a [href]="icsUrl" target="_blank">{{ icsUrl }}</a>
          <br/>
          <button (click)="copy()" style="margin-top:0.5rem;">Copiar URL</button>
        </div>

      </div>
    </div>
  `
})
export class MergeNewComponent {
  sources: MergeSource[] = [];
  maxResults = 100;
  refreshInterval = 60;
  loading = false;
  error = '';
  icsUrl = '';

  constructor(private mergeService: MergeService, private router: Router) {}

  addSource() {
    this.sources.push({ provider: 'google', accountId: '', calendarId: '', prefix: '' });
  }

  removeSource(i: number) {
    this.sources.splice(i, 1);
  }

  create() {
    if (this.sources.length === 0) {
      this.error = 'Añade al menos un calendario fuente.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.mergeService.create({
      sources: this.sources,
      maxResultsPerCalendar: this.maxResults,
      refreshIntervalMinutes: this.refreshInterval
    }).subscribe({
      next: (res) => {
        this.loading = false;
        this.icsUrl = this.mergeService.getIcsUrl(res.merge_id);
      },
      error: () => {
        this.loading = false;
        this.error = 'Error al crear la fusión. Inténtalo de nuevo.';
      }
    });
  }

  copy() {
    navigator.clipboard.writeText(this.icsUrl);
  }
}