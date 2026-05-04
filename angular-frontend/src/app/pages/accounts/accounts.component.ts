import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { GoogleCalendarService } from '../../core/services/google-calendar.service';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div style="padding:2rem;">
      <h1>Cuentas conectadas</h1>

      <div style="margin-top:1.5rem; display:flex; flex-direction:column; gap:1rem; max-width:400px;">

        <div style="border:1px solid #ccc; padding:1rem; border-radius:8px;">
          <h2>Google Calendar</h2>
          <p>Conecta una cuenta de Google para acceder a sus calendarios.</p>
          <button (click)="connectGoogle()" [disabled]="loadingGoogle">
            {{ loadingGoogle ? 'Redirigiendo...' : 'Conectar cuenta de Google' }}
          </button>
          <p *ngIf="errorGoogle" style="color:red;">{{ errorGoogle }}</p>
        </div>

        <div style="border:1px solid #ccc; padding:1rem; border-radius:8px;">
          <h2>CalDAV</h2>
          <p>Conecta una cuenta CalDAV (Nextcloud, Apple, etc.).</p>
          <a routerLink="/accounts/caldav">
            <button>Conectar cuenta CalDAV</button>
          </a>
        </div>

      </div>
    </div>
  `
})
export class AccountsComponent {
  loadingGoogle = false;
  errorGoogle = '';

  constructor(private googleService: GoogleCalendarService) {}

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