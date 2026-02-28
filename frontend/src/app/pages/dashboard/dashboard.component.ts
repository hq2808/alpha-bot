import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsPanelComponent } from '../../components/news-panel/news-panel.component';
import { SignalService, NewsArticle } from '../../services/signal.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NewsPanelComponent],
  template: `
    <div class="dashboard">

      <!-- Stats Row -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-label">Articles Today</div>
          <div class="stat-value">{{ articles.length }}</div>
        </div>
        <div class="stat-card bullish">
          <div class="stat-label">Bullish Signals</div>
          <div class="stat-value">{{ bullishCount }}</div>
        </div>
        <div class="stat-card bearish">
          <div class="stat-label">Bearish Signals</div>
          <div class="stat-value">{{ bearishCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Avg. Sentiment</div>
          <div class="stat-value" [class.bullish-text]="avgSentiment > 0" [class.bearish-text]="avgSentiment < 0">
            {{ avgSentiment | number:'1.2-2' }}
          </div>
        </div>
      </div>

      <!-- Main Grid -->
      <div class="main-grid">
        <!-- Left: Live News Feed -->
        <section class="panel">
          <app-news-panel />
        </section>

        <!-- Right: Sidebar Signals -->
        <aside class="sidebar">
          <div class="panel">
            <h3 class="panel-title">🔥 Top Bullish</h3>
            @for (a of topBullish; track a.id) {
              <div class="signal-item">
                <span class="score bullish">+{{ a.sentimentScore | number:'1.2-2' }}</span>
                <div class="signal-meta">
                  <span class="signal-source">{{ a.source }}</span>
                  <a [href]="a.url" target="_blank" class="signal-title">{{ a.title }}</a>
                  @if (a.mentionedTickers) {
                    <span class="tickers">{{ a.mentionedTickers }}</span>
                  }
                </div>
              </div>
            } @empty {
              <p class="empty-state">Chưa có tín hiệu. Crawler đang chạy...</p>
            }
          </div>
        </aside>
      </div>

    </div>
  `,
  styles: [`
    .stats-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 16px 20px;
    }

    .stat-card.bullish { border-left: 3px solid #3fb950; }
    .stat-card.bearish { border-left: 3px solid #f85149; }

    .stat-label { font-size: 0.8rem; color: #8b949e; margin-bottom: 6px; }
    .stat-value { font-size: 1.6rem; font-weight: 700; color: #e6edf3; }
    .bullish-text { color: #3fb950 !important; }
    .bearish-text { color: #f85149 !important; }

    .main-grid {
      display: grid;
      grid-template-columns: 1fr 360px;
      gap: 16px;
    }

    .panel {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 16px;
      height: fit-content;
    }

    .panel-title { color: #e6edf3; margin: 0 0 16px; font-size: 0.95rem; font-weight: 600; }

    .signal-item {
      display: flex;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid #21262d;
    }

    .score {
      font-size: 0.85rem;
      font-weight: 700;
      min-width: 48px;
      padding-top: 2px;
    }

    .score.bullish { color: #3fb950; }

    .signal-meta { display: flex; flex-direction: column; gap: 3px; }
    .signal-source { font-size: 0.75rem; color: #8b949e; }
    .signal-title {
      font-size: 0.85rem;
      color: #e6edf3;
      text-decoration: none;
      line-height: 1.4;
    }
    .signal-title:hover { color: #58a6ff; }
    .tickers { font-size: 0.75rem; color: #3fb950; font-weight: 600; }

    .empty-state { color: #8b949e; font-size: 0.85rem; padding: 16px 0; text-align: center; }

    @media (max-width: 900px) {
      .stats-row { grid-template-columns: repeat(2, 1fr); }
      .main-grid { grid-template-columns: 1fr; }
      .sidebar { order: -1; }
    }
    @media (max-width: 500px) {
      .stats-row { grid-template-columns: 1fr; }
    }
  `]
})
export class DashboardComponent implements OnInit {
  articles: NewsArticle[] = [];

  constructor(private signal: SignalService) { }

  ngOnInit(): void {
    this.signal.getLatestNews().subscribe(news => this.articles = news);
  }

  get bullishCount(): number {
    return this.articles.filter(a => a.sentimentScore >= 0.5).length;
  }

  get bearishCount(): number {
    return this.articles.filter(a => a.sentimentScore <= -0.5).length;
  }

  get avgSentiment(): number {
    if (!this.articles.length) return 0;
    return this.articles.reduce((sum, a) => sum + (a.sentimentScore ?? 0), 0) / this.articles.length;
  }

  get topBullish(): NewsArticle[] {
    return [...this.articles]
      .filter(a => a.sentimentScore >= 0.5)
      .sort((a, b) => b.sentimentScore - a.sentimentScore)
      .slice(0, 5);
  }
}
