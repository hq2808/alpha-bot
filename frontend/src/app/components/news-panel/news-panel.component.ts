import { Component, OnInit, OnDestroy, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { SignalService, NewsArticle } from '../../services/signal.service';

@Component({
  selector: 'app-news-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="news-panel">
      <h2 class="panel-title">📡 Live Intelligence Feed
        <span class="badge" [class.pulse]="liveCount > 0">{{ liveCount }} NEW</span>
      </h2>

      <div class="news-list">
        @for (article of displayArticles; track article.id) {
          <div class="news-card" [class]="signal.getSentimentClass(article.sentimentScore)">
            <div class="card-header">
              <span class="source">{{ article.source }}</span>
              <span class="sentiment" [class]="signal.getSentimentClass(article.sentimentScore)">
                {{ signal.formatSentiment(article.sentimentScore) }}
              </span>
            </div>

            <a [href]="article.url" target="_blank" class="article-title">
              {{ article.title }}
            </a>

            @if (article.aiSummary) {
              <p class="summary">🤖 {{ article.aiSummary }}</p>
            }

            @if (article.mentionedTickers) {
              <div class="tickers">
                @for (ticker of article.mentionedTickers.split(','); track ticker) {
                  <span class="ticker-chip">{{ ticker.trim() }}</span>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .news-panel { padding: 16px; }
    .panel-title { display: flex; align-items: center; gap: 12px; font-size: 1.2rem; }
    .badge { background: #e53935; color: white; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; }
    .badge.pulse { animation: pulse 1.5s infinite; }
    @keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
    .news-list { display: flex; flex-direction: column; gap: 12px; }
    .news-card { padding: 12px 16px; border-radius: 8px; border-left: 4px solid #aaa; background: rgba(255,255,255,0.05); }
    .news-card.bullish { border-left-color: #4caf50; }
    .news-card.bearish { border-left-color: #f44336; }
    .card-header { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 0.8rem; opacity: 0.7; }
    .article-title { display: block; font-weight: 600; color: inherit; text-decoration: none; margin-bottom: 6px; }
    .article-title:hover { text-decoration: underline; }
    .summary { font-size: 0.85rem; opacity: 0.8; margin: 4px 0; }
    .tickers { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 6px; }
    .ticker-chip { background: rgba(76,175,80,0.2); color: #4caf50; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 700; }
    .sentiment.bullish { color: #4caf50; }
    .sentiment.bearish { color: #f44336; }
    .sentiment.neutral { color: #aaa; }
  `]
})
export class NewsPanelComponent implements OnInit, OnDestroy, OnChanges {
  /** If provided by parent (Dashboard), avoids a duplicate API call. */
  @Input() articles: NewsArticle[] | null = null;

  private _articles: NewsArticle[] = [];
  liveCount = 0;
  private subs: Subscription[] = [];

  constructor(public signal: SignalService) { }

  get displayArticles(): NewsArticle[] {
    return this._articles;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['articles'] && this.articles) {
      this._articles = [...this.articles];
    }
  }

  ngOnInit(): void {
    // Only fetch independently if parent didn't provide articles
    if (!this.articles) {
      this.subs.push(
        this.signal.getLatestNews().subscribe(news => this._articles = news)
      );
    }

    // Real-time updates via WebSocket — always active
    this.subs.push(
      this.signal.getLiveNews().subscribe((article: NewsArticle) => {
        this._articles = [article, ...this._articles.slice(0, 19)];
        this.liveCount++;
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }
}
