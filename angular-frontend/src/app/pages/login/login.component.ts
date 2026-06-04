import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.services';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page" style="max-width:420px; margin:4rem auto;">
      <h1 class="page-title">📅 CalendarFusion</h1>
      <p class="page-subtitle">Inicia sesión para continuar</p>

      <div *ngIf="error" class="alert-error">{{ error }}</div>

      <div style="display:flex; flex-direction:column; gap:1rem; margin-top:2rem;">
        <input class="input" type="email" placeholder="Email" [(ngModel)]="email" />
        <input class="input" type="password" placeholder="Contraseña" [(ngModel)]="password" />
        <button class="btn btn-primary" (click)="login()" [disabled]="loading">
          {{ loading ? 'Entrando...' : 'Iniciar sesión' }}
        </button>
        <button class="btn btn-secondary" (click)="toggleMode()">
          {{ isRegister ? 'Ya tengo cuenta' : 'Crear cuenta nueva' }}
        </button>
      </div>

      <div *ngIf="isRegister" style="margin-top:1rem;">
        <button class="btn btn-primary" (click)="register()" [disabled]="loading">
          {{ loading ? 'Registrando...' : 'Registrarse' }}
        </button>
        <p *ngIf="registerOk" class="alert-info" style="margin-top:1rem;">
          Cuenta creada. Revisa tu email para confirmarla y luego inicia sesión.
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;
  error = '';
  isRegister = false;
  registerOk = false;

  constructor(private auth: AuthService, private router: Router) {}

  async login() {
    this.loading = true;
    this.error = '';
    try {
      await this.auth.login(this.email, this.password);
      this.router.navigate(['/']);
    } catch (e: any) {
      this.error = e.message || 'Error al iniciar sesión';
    } finally {
      this.loading = false;
    }
  }

  async register() {
    this.loading = true;
    this.error = '';
    try {
      await this.auth.signUp(this.email, this.password);
      this.registerOk = true;
    } catch (e: any) {
      this.error = e.message || 'Error al registrarse';
    } finally {
      this.loading = false;
    }
  }

  toggleMode() {
    this.isRegister = !this.isRegister;
    this.error = '';
    this.registerOk = false;
  }
}