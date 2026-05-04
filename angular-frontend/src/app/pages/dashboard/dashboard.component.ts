import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div style="padding:2rem;">
      <h1>Mis calendarios fusionados</h1>
      <a routerLink="/merge/new">+ Nueva fusión</a>
      <p style="margin-top:1rem;">No tienes ninguna fusión creada todavía.</p>
    </div>
  `
})
export class DashboardComponent {}