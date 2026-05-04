import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { MergeConfig } from '../../models/merge-config.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MergeService {
  constructor(private api: ApiService) {}

  list(): Observable<any[]> {
    return this.api.http.get<any[]>(`${this.api.base}/merge`);
  }

  create(config: MergeConfig): Observable<{ merge_id: string; ics_url: string }> {
    return this.api.http.post<any>(`${this.api.base}/merge`, config);
  }

  get(mergeId: string): Observable<any> {
    return this.api.http.get(`${this.api.base}/merge/${mergeId}`);
  }

  delete(mergeId: string): Observable<any> {
    return this.api.http.delete(`${this.api.base}/merge/${mergeId}`);
  }

  getIcsUrl(mergeId: string): string {
    return `${this.api.base}/merge/${mergeId}/ics`;
  }
}