import { Component,OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink,ActivatedRoute } from '@angular/router';
import { GoogleCalendarService } from '../../core/services/google-calendar.service';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <h1 class="page-title">🔗 Cuentas conectadas</h1>
      <p class="page-subtitle">Conecta tus fuentes de calendario</p>
        <div *ngIf="connectedAccount" class="alert-success">
        ✅ Cuenta <strong>{{ connectedAccount }}</strong> conectada correctamente.
      </div>
      <div class="card">
        <h2>Google Calendar</h2>
        <p>Conecta una cuenta de Google para acceder a sus calendarios.</p>
        <button class="btn btn-google" (click)="connectGoogle()" [disabled]="loadingGoogle">
          <span>🔵</span> {{ loadingGoogle ? 'Redirigiendo...' : 'Conectar con Google' }}
        </button>
        <div *ngIf="errorGoogle" class="alert-error" style="margin-top:0.75rem;">{{ errorGoogle }}</div>
      </div>

        <div class="card">
        <h2>CalDAV</h2>
        <p>Conecta una cuenta CalDAV (Nextcloud, Apple Calendar, etc.).</p>
        <a routerLink="/accounts/caldav">
          <button class="btn btn-secondary">Conectar CalDAV</button>
        </a>
      </div>

      <a routerLink="/"><button class="btn btn-secondary" style="margin-top:0.5rem;">← Volver al inicio</button></a>
    </div>
  `
})
export class AccountsComponent implements OnInit{
  loadingGoogle = false;
  errorGoogle = '';
  connectedAccount = '';

 
  constructor(private googleService: GoogleCalendarService, private route: ActivatedRoute) {}

   ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['connected']) {
        this.connectedAccount = params['connected'];
      }
    });
  }
  connectGoogle() {
    this.loadingGoogle = true;
    this.errorGoogle = '';
    const accountId = 'google-' + Date.now();
    this.googleService.getAuthUrl(accountId).subscribe({
      next: (url) => {
        window.location.href = url;
      },
      error: () => {
        this.loadingGoogle = false;
        this.errorGoogle = 'Error al obtener la URL de autenticación.';
      }
    });
  }
}