import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MergeService } from '../../core/services/merge.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div style="padding:2rem;">
      <h1>Mis calendarios fusionados</h1>

      <div style="margin-top:1rem; display:flex; gap:1rem;">
        <a routerLink="/merge/new"><button>+ Nueva fusión</button></a>
        <a routerLink="/accounts"><button>Gestionar cuentas</button></a>
      </div>

      <p *ngIf="loading" style="margin-top:1rem;">Cargando...</p>
      <p *ngIf="error" style="margin-top:1rem; color:red;">{{ error }}</p>

      <div *ngIf="!loading && merges.length === 0" style="margin-top:1.5rem;">
        No tienes ninguna fusión creada todavía.
      </div>

      <div *ngFor="let m of merges"
           style="margin-top:1rem; border:1px solid #ccc; padding:1rem; border-radius:8px;">
        <strong>{{ m.merge_id }}</strong>

        <div style="margin-top:0.5rem;">
          <span>URL ICS: </span>
          <a [href]="getIcsUrl(m.merge_id)" target="_blank">{{ getIcsUrl(m.merge_id) }}</a>
          <button (click)="copy(m.merge_id)" style="margin-left:0.5rem;">Copiar</button>
        </div>

        <button (click)="delete(m.merge_id)" style="margin-top:0.5rem; color:red;">
          Eliminar
        </button>
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