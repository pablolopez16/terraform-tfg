import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CalDavService } from '../../core/services/caldav.service';

@Component({
  selector: 'app-connect-caldav',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="padding:2rem;">
      <h1>Conectar cuenta CalDAV</h1>

      <div style="display:flex; flex-direction:column; gap:0.75rem; max-width:400px; margin-top:1.5rem;">
        <input [(ngModel)]="accountId" placeholder="Nombre de cuenta (ej: mi-nextcloud)" />
        <input [(ngModel)]="serverUrl" placeholder="URL del servidor (ej: https://nextcloud.example.com)" />
        <input [(ngModel)]="username" placeholder="Usuario" />
        <input [(ngModel)]="password" type="password" placeholder="Contraseña" />

        <button (click)="connect()" [disabled]="loading">
          {{ loading ? 'Conectando...' : 'Conectar' }}
        </button>

        <p *ngIf="error" style="color:red;">{{ error }}</p>
        <p *ngIf="success" style="color:green;">Cuenta conectada correctamente.</p>
      </div>
    </div>
  `
})
export class ConnectCaldavComponent {
  accountId = '';
  serverUrl = '';
  username = '';
  password = '';
  loading = false;
  error = '';
  success = false;

  constructor(private caldavService: CalDavService, private router: Router) {}

  connect() {
    this.loading = true;
    this.error = '';
    this.caldavService.login(this.accountId, this.username, this.password, this.serverUrl)
      .subscribe({
        next: () => {
          this.loading = false;
          this.success = true;
          setTimeout(() => this.router.navigate(['/accounts']), 1500);
        },
        error: (err) => {
          this.loading = false;
          this.error = 'Error al conectar. Revisa los datos.';
        }
      });
  }
}