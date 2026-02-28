import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalService } from '../../services/signal.service';

@Component({
    selector: 'app-watchlist',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="watchlist-container">
      <div class="header">
        <h1>Global Watchlist</h1>
        <p class="subtitle">Only tickers in this list will trigger Telegram alerts. If empty, all bullish signals trigger alerts.</p>
      </div>

      <div class="add-form">
        <input type="text" [(ngModel)]="newTicker" (keyup.enter)="addTicker()" placeholder="Enter Ticker (e.g., FPT)" class="tech-input">
        <button (click)="addTicker()" class="btn-primary" [disabled]="loading || !newTicker">Add</button>
      </div>

      <div class="list-grid">
        <div class="ticker-card" *ngFor="let item of watchlist">
          <span class="ticker-name">{{ item.ticker }}</span>
          <button class="btn-delete" (click)="removeTicker(item.ticker)" title="Remove">✕</button>
        </div>
      </div>
      
      <div class="empty-state" *ngIf="watchlist.length === 0 && !loading">
        <p>Watchlist is empty. You receive all alerts.</p>
      </div>
    </div>
  `,
    styles: [`
    .watchlist-container { max-width: 800px; margin: 0 auto; color: #e6edf3; }
    .header h1 { font-size: 1.5rem; margin-bottom: 8px; border-bottom: 1px solid #30363d; padding-bottom: 12px; }
    .subtitle { color: #8b949e; font-size: 0.9rem; margin-bottom: 24px; }
    .add-form { display: flex; gap: 12px; margin-bottom: 24px; }
    .tech-input { padding: 8px 12px; background: #0d1117; border: 1px solid #30363d; color: #e6edf3; border-radius: 6px; flex: 1; max-width: 300px; }
    .tech-input:focus { border-color: #58a6ff; outline: none; }
    .btn-primary { background: #238636; color: #fff; border: 1px solid rgba(240,246,252,0.1); padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: 500; }
    .btn-primary:hover:not([disabled]) { background: #2ea043; }
    .btn-primary[disabled] { opacity: 0.6; cursor: not-allowed; }
    
    .list-grid { display: flex; flex-wrap: wrap; gap: 12px; }
    .ticker-card { display: flex; align-items: center; justify-content: space-between; background: #161b22; border: 1px solid #30363d; padding: 10px 16px; border-radius: 6px; min-width: 120px; }
    .ticker-name { font-weight: 700; color: #58a6ff; }
    .btn-delete { background: none; border: none; color: #8b949e; cursor: pointer; padding: 4px; font-size: 1rem; margin-left: auto; }
    .btn-delete:hover { color: #f85149; }
    .empty-state { text-align: center; color: #8b949e; padding: 40px; font-style: italic; }
  `]
})
export class WatchlistComponent implements OnInit {
    watchlist: any[] = [];
    newTicker: string = '';
    loading = false;

    constructor(private signalService: SignalService) { }

    ngOnInit() {
        this.loadWatchlist();
    }

    loadWatchlist() {
        this.loading = true;
        this.signalService.getWatchlist().subscribe({
            next: (data) => { this.watchlist = data; this.loading = false; },
            error: () => this.loading = false
        });
    }

    addTicker() {
        const t = this.newTicker.trim();
        if (!t) return;
        this.loading = true;
        this.signalService.addToWatchlist(t).subscribe({
            next: () => {
                this.newTicker = '';
                this.loadWatchlist();
            },
            error: () => this.loading = false
        });
    }

    removeTicker(ticker: string) {
        this.signalService.removeFromWatchlist(ticker).subscribe({
            next: () => this.loadWatchlist()
        });
    }
}
