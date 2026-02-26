import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalService, NewsArticle } from '../../services/signal.service';

@Component({
    selector: 'app-signals',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="signals-page">
      <div class="page-header">
        <h1>📈 Bullish Signals</h1>
        <div class="filter-row">
          <label>Sentiment threshold:</label>
          <input type="range" min="0" max="1" step="0.05"
                 [(ngModel)]="threshold"
                 (change)="load()" />
          <span class="threshold-val">≥ {{ threshold | number:'1.2-2' }}</span>
        </div>
      </div>

      <div class="signals-grid">
        @for (article of articles; track article.id) {
          <div class="signal-card">
            <div class="card-top">
              <span class="source-badge">{{ article.source }}</span>
              <span class="score">{{ article.sentimentScore | number:'1.2-2' }}</span>
            </div>
            <a [href]="article.url" target="_blank" class="title">{{ article.title }}</a>
            @if (article.aiSummary) {
              <p class="summary">🤖 {{ article.aiSummary }}</p>
            }
            @if (article.mentionedTickers) {
              <div class="tickers">
                @for (t of article.mentionedTickers.split(','); track t) {
                  <span class="chip">{{ t.trim() }}</span>
                }
              </div>
            }
            <span class="time">{{ article.crawledAt | date:'HH:mm dd/MM' }}</span>
          </div>
        } @empty {
          <div class="empty">
            <p>Không có tín hiệu nào trên ngưỡng {{ threshold | number:'1.2-2' }}</p>
            <small>Crawler đang thu thập dữ liệu hoặc hãy giảm ngưỡng xuống.</small>
          </div>
        }
      </div>
    </div>
  `,
    styles: [`
    .page-header { margin-bottom: 24px; }
    h1 { color: #e6edf3; font-size: 1.4rem; margin-bottom: 12px; }

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 0.9rem;
      color: #8b949e;
    }

    input[type=range] { width: 200px; accent-color: #3fb950; }
    .threshold-val { color: #3fb950; font-weight: 700; font-size: 1rem; }

    .signals-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }

    .signal-card {
      background: #161b22;
      border: 1px solid #30363d;
      border-left: 3px solid #3fb950;
      border-radius: 8px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .card-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .source-badge {
      background: #21262d;
      color: #8b949e;
      font-size: 0.75rem;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .score {
      font-size: 1.2rem;
      font-weight: 700;
      color: #3fb950;
    }

    .title {
      font-size: 0.9rem;
      font-weight: 600;
      color: #e6edf3;
      text-decoration: none;
      line-height: 1.4;
    }
    .title:hover { color: #58a6ff; text-decoration: underline; }

    .summary { font-size: 0.82rem; color: #8b949e; line-height: 1.4; margin: 0; }

    .tickers { display: flex; gap: 6px; flex-wrap: wrap; }
    .chip {
      background: rgba(63,185,80,0.15);
      color: #3fb950;
      font-size: 0.75rem;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .time { font-size: 0.75rem; color: #6e7681; margin-top: auto; }

    .empty {
      grid-column: 1/-1;
      text-align: center;
      padding: 48px;
      color: #8b949e;
    }
    .empty small { display: block; margin-top: 8px; font-size: 0.8rem; opacity: 0.7; }
  `]
})
export class SignalsComponent implements OnInit {
    articles: NewsArticle[] = [];
    threshold = 0.5;

    constructor(private signal: SignalService) { }

    ngOnInit(): void { this.load(); }

    load(): void {
        this.signal.getBullishNews(this.threshold).subscribe(news => this.articles = news);
    }
}
