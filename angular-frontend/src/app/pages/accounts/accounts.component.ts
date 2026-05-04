import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div style="padding:2rem;">
      <h1>Cuentas conectadas</h1>
      <a routerLink="/accounts/caldav">Conectar CalDAV</a>
    </div>
  `
})
export class AccountsComponent {}