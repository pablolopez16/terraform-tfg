import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MergeService } from '../../core/services/merge.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <h1 class="page-title">📅 CalendarFusion</h1>
      <p class="page-subtitle">Tus calendarios fusionados en un solo lugar</p>

      <div class="action-bar">
        <a routerLink="/merge/new"><button class="btn btn-primary">+ Nueva fusión</button></a>
        <a routerLink="/accounts"><button class="btn btn-secondary">Gestionar cuentas</button></a>
      </div>

     <div *ngIf="loading" class="alert-info">Cargando fusiones...</div>
     <div *ngIf="error" class="alert-error">{{ error }}</div>

      <div *ngIf="!loading && merges.length === 0" class="empty-state">
        No tienes ninguna fusión creada todavía.<br>
        <a routerLink="/merge/new"><button class="btn btn-primary" style="margin-top:1rem;">+ Crear primera fusión</button></a>
      </div>

      <div *ngFor="let m of merges" class="merge-item">
        <strong>{{ m.merge_id }}</strong>

        <div class="ics-link">
          <a [href]="getIcsUrl(m.merge_id)" target="_blank">{{ getIcsUrl(m.merge_id) }}</a>
          <button (click)="copy(m.merge_id)" class="btn btn-secondary btn-sm">Copiar</button>
          <button (click)="delete(m.merge_id)" class="btn btn-danger btn-sm">Eliminar</button>
        </div>

        
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  merges: any[] = [];
  loading = false;
  error = '';

  constructor(private mergeService: MergeService) {}

  ngOnInit() {
    this.loading = true;
    this.mergeService.list().subscribe({
      next: (data) => {
        this.merges = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar las fusiones.';
        this.loading = false;
      }
    });
  }

  delete(mergeId: string) {
    this.mergeService.delete(mergeId).subscribe({
      next: () => {
        this.merges = this.merges.filter(m => m.merge_id !== mergeId);
      },
      error: () => {
        this.error = 'Error al eliminar la fusión.';
      }
    });
  }

  getIcsUrl(mergeId: string): string {
    return this.mergeService.getIcsUrl(mergeId);
  }

  copy(mergeId: string) {
    navigator.clipboard.writeText(this.getIcsUrl(mergeId));
  }
}