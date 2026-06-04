import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalyticsData } from '../models/analytics.interface';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/admin/analytics`;

  getAnalytics(): Observable<AnalyticsData> {
    return this.http.get<AnalyticsData>(this.API_URL);
  }
}
