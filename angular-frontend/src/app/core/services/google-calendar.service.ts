import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class GoogleCalendarService {
  constructor(private api: ApiService) {}

  getAuthUrl(accountId: string): Observable<string> {
    return this.api.http.get(
      `${this.api.base}/google-calendar/auth/google?accountId=${accountId}`,
      { responseType: 'text' }
    );
  }

  listCalendars(accountId: string): Observable<any[]> {
    return this.api.http.get<any[]>(
      `${this.api.base}/google-calendar/${accountId}/calendars`
    );
  }
}