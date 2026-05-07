import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MergeService } from '../../core/services/merge.service';
import { MergeSource } from '../../models/merge-source.model';
import { CalDavService } from '../../core/services/caldav.service';
import { GoogleCalendarService } from '../../core/services/google-calendar.service';

@Component({
  selector: 'app-merge-new',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h1 class="page-title">➕ Nueva fusión</h1>
      <p class="page-subtitle">Combina varios calendarios en uno</p>

      <div class="card">
        <h2>Calendarios fuente</h2>

        <div *ngFor="let s of sources; let i = index" class="source-card">
          <div class="form-group">
            <label>Proveedor</label>
            <select [(ngModel)]="s.provider" (ngModelChange)="onProviderChange(i)">
              <option value="google">🔵 Google Calendar</option>
              <option value="caldav">📆 CalDAV</option>
            </select>
          </div>

          <div class="form-group">
            <label>ID de cuenta</label>
            <input [(ngModel)]="s.accountId" placeholder="ej: google-1234567890" (blur)="loadCalendars(i)" />
          </div>

          <label>Calendario</label>
              <div style="display:flex; gap:0.5rem; align-items:center;">
                <select *ngIf="calendarOptions[i]?.length; else caldavManualInput" [(ngModel)]="s.calendarId" style="flex:1;">
                  <option value="">-- Selecciona un calendario --</option>
                  <option *ngFor="let c of calendarOptions[i]" [value]="c.id">{{ c.summary }}</option>
                </select>
                <ng-template #caldavManualInput>
                  <input [(ngModel)]="s.calendarId" placeholder="ej: /dav/principal/calendars/home/" style="flex:1;" />
                </ng-template>
                <button class="btn btn-secondary btn-sm" (click)="loadCalendars(i)" [disabled]="loadingCalendars[i]">
                  {{ loadingCalendars[i] ? '⏳' : '🔄 Cargar' }}
                </button>
              </div>

          <ng-container *ngIf="s.provider === 'google'">
            <div class="form-group">
              <label>Calendario</label>
              <div style="display:flex; gap:0.5rem; align-items:center;">
                <select *ngIf="calendarOptions[i]?.length; else manualInput" [(ngModel)]="s.calendarId" style="flex:1;">
                  <option value="">-- Selecciona un calendario --</option>
                  <option *ngFor="let c of calendarOptions[i]" [value]="c.id">{{ c.summary }}</option>
                </select>
                <ng-template #manualInput>
                  <input [(ngModel)]="s.calendarId" placeholder="ej: primary" style="flex:1;" />
                </ng-template>
                <button class="btn btn-secondary btn-sm" (click)="loadCalendars(i)" [disabled]="loadingCalendars[i]">
                  {{ loadingCalendars[i] ? '⏳' : '🔄 Cargar' }}
                </button>
              </div>
            </div>
            <div *ngIf="calendarErrors[i]" class="alert-error">{{ calendarErrors[i] }} 
            </div>          
          </ng-container>

          <ng-container *ngIf="s.provider === 'caldav'">
           <div class="form-group">
              <label>ID de calendario</label>
              <input [(ngModel)]="s.calendarId" placeholder="ej: /calendars/personal" />
            </div>
          </ng-container>

           <button class="btn btn-danger btn-sm" (click)="removeSource(i)">Eliminar fuente</button>
        </div>

        <button class="btn btn-secondary" (click)="addSource()" style="margin-top:0.5rem;">+ Añadir calendario fuente</button>
      </div>

      <div class="card">
        <h2>Configuración</h2>
        <div class="form-group">
          <label>Máximo de eventos por calendario</label>
          <input type="number" [(ngModel)]="maxResults" placeholder="ej: 100" />
        </div>
        <div class="form-group">
          <label>Frecuencia de refresco (minutos)</label>
          <input type="number" [(ngModel)]="refreshInterval" placeholder="ej: 60" />
        </div>
      </div>


        <div *ngIf="error" class="alert-error">{{ error }}</div>

        <button class="btn btn-primary" (click)="create()" [disabled]="loading" style="width:100%; padding:0.85rem; font-size:1rem;">
        {{ loading ? 'Creando...' : '🚀 Crear fusión' }}
      </button>

        <div *ngIf="icsUrl" class="ics-result" style="margin-top:1rem;">
        <p>✅ Fusión creada. Suscríbete con esta URL ICS:</p>
        <a [href]="icsUrl" target="_blank">{{ icsUrl }}</a>
        <br/>
        <button class="btn btn-secondary btn-sm" (click)="copy()" style="margin-top:0.75rem;">Copiar URL</button>
      </div>

      <a routerLink="/"><button class="btn btn-secondary" style="margin-top:1rem;">← Volver</button></a>
    </div>
  `
})
export class MergeNewComponent {
  sources: MergeSource[] = [];
  maxResults = 100;
  refreshInterval = 60;
  calendarOptions: { id: string; summary: string }[][] = [];
  loadingCalendars: boolean[] = [];
  calendarErrors: string[] = [];
  loading = false;
  error = '';
  icsUrl = '';

  constructor(private mergeService: MergeService, private googleService: GoogleCalendarService, private caldavService: CalDavService, private router: Router) {}
  addSource() {
    this.sources.push({ provider: 'google', accountId: '', calendarId: '', prefix: '' });
     this.calendarOptions.push([]);
     this.loadingCalendars.push(false);
     this.calendarErrors.push('');
  }

  removeSource(i: number) {
    this.sources.splice(i, 1);
    this.calendarOptions.splice(i, 1);
    this.loadingCalendars.splice(i, 1);
    this.calendarErrors.splice(i, 1);
  }
  onProviderChange(i: number) {
    this.calendarOptions[i] = [];
    this.calendarErrors[i] = '';
    this.sources[i].calendarId = '';
  }

  loadCalendars(i: number) {
    const s = this.sources[i];
    if (s.provider !== 'google' || !s.accountId.trim()) return;
    this.loadingCalendars[i] = true;
    this.calendarErrors[i] = '';
    this.googleService.listCalendars(s.accountId).subscribe({
      next: (items: any[]) => {
        this.calendarOptions[i] = items.map(c => ({ id: c.id, summary: c.summary || c.id }));
        this.loadingCalendars[i] = false;
      },
      error: () => {
        this.calendarErrors[i] = 'No se pudieron cargar los calendarios. Comprueba el ID de cuenta.';
        this.loadingCalendars[i] = false;
      }
    });
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