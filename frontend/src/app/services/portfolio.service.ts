import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PortfolioSummary {
  name: string;
  initialCapital: number;
  cashBalance: number;
  totalEquity: number;
  pnlValue: number;
  pnlPercent: number;
}

export interface PortfolioPosition {
  id: number;
  ticker: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  pnlValue: number;
  pnlPercent: number;
}

export interface PortfolioTransaction {
  id: number;
  ticker: string;
  type: string;
  quantity: number;
  price: number;
  totalValue: number;
  reason: string;
  createdAt: string;
}

export interface PortfolioSnapshot {
  id: number;
  snapshotDate: string;
  totalEquity: number;
  cashBalance: number;
  stockValue: number;
}

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {
  private apiUrl = '/api/portfolio'; // Will be proxied config

  constructor(private http: HttpClient) { }

  getSummary(): Observable<PortfolioSummary> {
    return this.http.get<PortfolioSummary>(`${this.apiUrl}/summary`);
  }

  getPositions(): Observable<PortfolioPosition[]> {
    return this.http.get<PortfolioPosition[]>(`${this.apiUrl}/positions`);
  }

  getTransactions(): Observable<PortfolioTransaction[]> {
    return this.http.get<PortfolioTransaction[]>(`${this.apiUrl}/transactions`);
  }

  getChartData(): Observable<PortfolioSnapshot[]> {
    return this.http.get<PortfolioSnapshot[]>(`${this.apiUrl}/chart`);
  }
}
