import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CalDavService {
  constructor(private api: ApiService) {}

  login(accountId: string, username: string, password: string, serverUrl: string): Observable<string> {
    return this.api.http.post(
      `${this.api.base}/caldav/auth/login`,
      { account_id: accountId, username, password, serverUrl },
      { responseType: 'text' }
    );
  }

  listCalendars(accountId: string): Observable<any[]> {
    return this.api.http.get<any[]>(
      `${this.api.base}/caldav/${accountId}/calendars`
    );
  }
}