import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AccountsComponent } from './pages/accounts/accounts.component';
import { ConnectCaldavComponent } from './pages/connect-caldav/connect-caldav.component';
import { MergeNewComponent } from './pages/merge-new/merge-new.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'accounts', component: AccountsComponent },
  { path: 'accounts/caldav', component: ConnectCaldavComponent },
  { path: 'merge/new', component: MergeNewComponent },
  { path: '**', redirectTo: '' }
];