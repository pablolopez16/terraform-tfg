import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AccountsComponent } from './pages/accounts/accounts.component';
import { ConnectCaldavComponent } from './pages/connect-caldav/connect-caldav.component';
import { MergeNewComponent } from './pages/merge-new/merge-new.component';
import { LoginComponent } from './pages/login/login.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'accounts', component: AccountsComponent, canActivate: [authGuard] },
  { path: 'accounts/caldav', component: ConnectCaldavComponent, canActivate: [authGuard] },
  { path: 'merge/new', component: MergeNewComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];