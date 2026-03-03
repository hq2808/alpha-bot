import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { SignalService, NewsArticle, NewsPage, SENTIMENT_THRESHOLDS } from '../../services/signal.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="dashboard">

      <!-- Stats Row -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">📰</div>
          <div class="stat-info">
            <div class="stat-value">{{ totalArticles }}</div>
            <div class="stat-label">Tổng bài báo</div>
          </div>
        </div>
        <div class="stat-card pos selectable" [class.active-filter]="activeFilter === 'bull'" (click)="toggleFilter('bull')">
          <div class="stat-icon">📈</div>
          <div class="stat-info">
            <div class="stat-value bullish-text">{{ bullishCount }}</div>
            <div class="stat-label">Tin tích cực</div>
          </div>
        </div>
        <div class="stat-card neg selectable" [class.active-filter]="activeFilter === 'bear'" (click)="toggleFilter('bear')">
          <div class="stat-icon">📉</div>
          <div class="stat-info">
            <div class="stat-value bearish-text">{{ bearishCount }}</div>
            <div class="stat-label">Tin tiêu cực</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🎯</div>
          <div class="stat-info">
            <div class="stat-value" [class]="avgSentimentClass">{{ avgSentiment | number:'1.2-2' }}</div>
            <div class="stat-label">Avg Sentiment</div>
          </div>
        </div>
      </div>

      <!-- Hot Tickers Row -->
      @if (hotTickers.length > 0) {
        <div class="panel hot-row">
          <span class="hot-label">🔥 Đang hot ·</span>
          @for (t of hotTickers; track t.ticker) {
            <span class="ticker-chip" [class]="sentimentClass(t.avgScore)"
                  (click)="filterByTicker(t.ticker)">
              {{ t.ticker }}
              <span class="chip-score">{{ t.avgScore > 0 ? '+' : '' }}{{ t.avgScore | number:'1.2-2' }}</span>
              <span class="chip-count">({{ t.count }})</span>
            </span>
          }
        </div>
      }

      <!-- Search toolbar -->
      <div class="toolbar">
        <div class="search-wrap">
          <input type="text" class="search-input" [(ngModel)]="searchQuery"
                 placeholder="🔍 Tìm tin tức, mã cổ phiếu..."
                 (ngModelChange)="onSearchChange($event)" />
          @if (searchQuery) {
            <button class="clear-btn" (click)="clearSearch()">✕</button>
          }
        </div>
      </div>

      <!-- News Stream -->
      <div class="news-stream">
        <div class="stream-header">
          <h3>📡 Dòng tin mới nhất</h3>
          @if (page) {
            <span class="stream-sub">{{ page.totalElements }} bài · Trang {{ page.number + 1 }}/{{ page.totalPages }}</span>
          }
        </div>

        @if (loading) {
          <div class="loading-state">⏳ Đang tải...</div>
        }

        <div class="news-list">
          @for (a of articles; track a.id) {
            <div class="news-card" [class]="cardClass(a.sentimentScore)">
              <div class="card-bar" [class]="barClass(a.sentimentScore)"></div>
              <div class="card-body">
                <div class="card-meta">
                  <span class="card-source">{{ a.source }}</span>
                  <span class="card-time">{{ (a.publishedAt || a.crawledAt) | date:'dd/MM HH:mm' }}</span>
                  @if (a.mentionedTickers) {
                    <span class="card-tickers" (click)="filterByTicker(a.mentionedTickers.split(',')[0].trim()); $event.stopPropagation()">
                      {{ a.mentionedTickers }}
                    </span>
                  }
                </div>
                <a [href]="a.url" target="_blank" class="card-title">{{ a.title }}</a>
                @if (a.aiSummary) {
                  <p class="card-summary">{{ a.aiSummary }}</p>
                }
              </div>
              <div class="card-score" [class]="scoreClass(a.sentimentScore)">
                {{ a.sentimentScore > 0 ? '+' : '' }}{{ a.sentimentScore | number:'1.2-2' }}
              </div>
            </div>
          } @empty {
            @if (!loading) {
              <div class="empty-state">Không tìm thấy bài báo nào.</div>
            }
          }
        </div>

        <!-- Pagination -->
        @if (page && page.totalPages > 1) {
          <div class="pagination">
            <button class="page-btn" [disabled]="currentPage === 0" (click)="goPage(0)">«</button>
            <button class="page-btn" [disabled]="currentPage === 0" (click)="goPage(currentPage - 1)">‹</button>
            @for (p of pageNumbers; track p) {
              <button class="page-btn" [class.active]="p === currentPage" (click)="goPage(p)">{{ p + 1 }}</button>
            }
            <button class="page-btn" [disabled]="currentPage >= page.totalPages - 1" (click)="goPage(currentPage + 1)">›</button>
            <button class="page-btn" [disabled]="currentPage >= page.totalPages - 1" (click)="goPage(page.totalPages - 1)">»</button>
          </div>
        }
      </div>

    </div>
  `,
  styles: [`
    .dashboard { display: flex; flex-direction: column; gap: 18px; }

    /* Stats */
    .stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
    .stat-card {
      background: #161b22; border: 1px solid #30363d; border-radius: 10px;
      padding: 16px 18px; display: flex; align-items: center; gap: 14px;
    }
    .stat-card.pos { border-left: 3px solid #3fb950; }
    .stat-card.neg { border-left: 3px solid #f85149; }
    .stat-card.selectable { cursor: pointer; transition: all 0.2s; }
    .stat-card.selectable:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
    .stat-card.selectable.active-filter { background: #1c2128; border-color: #58a6ff; }
    .stat-icon { font-size: 1.6rem; }
    .stat-value { font-size: 1.8rem; font-weight: 800; color: #e6edf3; line-height: 1; }
    .stat-label { font-size: 0.75rem; color: #8b949e; margin-top: 3px; }
    .bullish-text { color: #3fb950 !important; }
    .bearish-text { color: #f85149 !important; }

    /* Hot Tickers */
    .hot-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding: 12px 16px !important; }
    .hot-label { font-size: 0.8rem; color: #8b949e; white-space: nowrap; }
    .ticker-chip {
      display: inline-flex; align-items: center; gap: 4px;
      background: #21262d; border: 1px solid #30363d; border-radius: 20px;
      padding: 4px 12px; font-weight: 700; font-size: 0.82rem; cursor: pointer;
      transition: all 0.15s;
    }
    .ticker-chip:hover { border-color: #58a6ff; transform: translateY(-1px); }
    .ticker-chip.bull { color: #3fb950; border-color: #3fb95044; }
    .ticker-chip.bear { color: #f85149; border-color: #f8514944; }
    .ticker-chip.neut { color: #8b949e; }
    .chip-score { font-size: 0.72rem; opacity: 0.8; }
    .chip-count { font-size: 0.68rem; color: #6e7681; }

    /* Toolbar */
    .toolbar { display: flex; gap: 10px; align-items: center; }
    .search-wrap { flex: 1; position: relative; }
    .search-input {
      width: 100%; padding: 9px 36px 9px 14px; background: #161b22;
      border: 1px solid #30363d; border-radius: 8px; color: #e6edf3;
      font-size: 0.9rem; outline: none; transition: border-color 0.15s;
      box-sizing: border-box;
    }
    .search-input:focus { border-color: #58a6ff; }
    .clear-btn {
      position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
      background: none; border: none; color: #6e7681; cursor: pointer; font-size: 0.8rem;
    }
    .sort-select {
      background: #161b22; border: 1px solid #30363d; border-radius: 8px;
      color: #e6edf3; padding: 9px 12px; font-size: 0.85rem; outline: none; cursor: pointer;
    }

    /* News Stream */
    .stream-header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
    .stream-header h3 { margin: 0; font-size: 0.95rem; color: #e6edf3; font-weight: 700; }
    .stream-sub { font-size: 0.75rem; color: #6e7681; }
    .news-list { display: flex; flex-direction: column; gap: 6px; }
    .loading-state { text-align: center; padding: 20px; color: #8b949e; font-size: 0.9rem; }

    .news-card {
      display: flex; align-items: stretch; background: #161b22;
      border: 1px solid #30363d; border-radius: 8px; overflow: hidden;
      transition: border-color 0.15s, background 0.15s;
    }
    .news-card:hover { border-color: #58a6ff44; background: #1c2128; }
    .card-bar { width: 3px; flex-shrink: 0; }
    .card-bar.bull { background: #3fb950; }
    .card-bar.bear { background: #f85149; }
    .card-bar.neut { background: #30363d; }
    .card-body { flex: 1; padding: 10px 14px; display: flex; flex-direction: column; gap: 4px; }
    .card-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .card-source { font-size: 0.72rem; color: #8b949e; }
    .card-time { font-size: 0.7rem; color: #6e7681; font-family: monospace; }
    .card-tickers {
      font-size: 0.7rem; font-weight: 700; color: #3fb950; cursor: pointer;
      background: rgba(63,185,80,.1); padding: 1px 6px; border-radius: 4px;
    }
    .card-tickers:hover { background: rgba(63,185,80,.2); }
    .card-title { font-size: 0.88rem; color: #e6edf3; text-decoration: none; line-height: 1.4; }
    .card-title:hover { color: #58a6ff; }
    .card-summary { font-size: 0.78rem; color: #8b949e; line-height: 1.5; margin: 0; }
    .card-score {
      padding: 0 14px; font-size: 0.82rem; font-weight: 700; font-family: monospace;
      display: flex; align-items: center; min-width: 56px; justify-content: flex-end; flex-shrink: 0;
    }
    .card-score.bull { color: #3fb950; }
    .card-score.bear { color: #f85149; }
    .card-score.neut { color: #6e7681; }

    /* Pagination */
    .pagination { display: flex; justify-content: center; gap: 4px; margin-top: 16px; flex-wrap: wrap; }
    .page-btn {
      background: #21262d; border: 1px solid #30363d; color: #e6edf3;
      border-radius: 6px; padding: 6px 12px; font-size: 0.82rem; cursor: pointer;
      transition: all 0.15s;
    }
    .page-btn:hover:not([disabled]):not(.active) { border-color: #58a6ff; color: #58a6ff; }
    .page-btn.active { background: #1f6feb; border-color: #1f6feb; color: #fff; font-weight: 600; }
    .page-btn[disabled] { opacity: 0.35; cursor: default; }

    .panel { background: #161b22; border: 1px solid #30363d; border-radius: 10px; padding: 16px; }
    .empty-state { color: #8b949e; text-align: center; padding: 32px; font-size: 0.9rem; }

    @media (max-width: 900px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  articles: NewsArticle[] = [];
  page?: NewsPage;
  loading = false;
  searchQuery = '';
  currentPage = 0;
  activeFilter: 'all' | 'bull' | 'bear' = 'all';
  readonly PAGE_SIZE = 20;

  // Stats are based on first page results only
  bullishCount = 0;
  bearishCount = 0;
  avgSentiment = 0;
  hotTickers: { ticker: string; count: number; avgScore: number }[] = [];

  private search$ = new Subject<string>();
  private sub?: Subscription;

  constructor(private signal: SignalService, private router: Router) { }

  ngOnInit(): void {
    this.sub = this.search$.pipe(debounceTime(350)).subscribe(() => {
      this.currentPage = 0;
      this.loadNews();
    });
    this.loadNews();
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }

  loadNews(): void {
    this.loading = true;

    // Call API with filterType directly
    this.signal.searchNews(this.searchQuery, this.currentPage, this.PAGE_SIZE, this.activeFilter).subscribe({
      next: (p: NewsPage) => {
        this.page = p;
        // The API already sorts correctly via SQL ORDER BY.
        let results = p.content;

        this.computeStats(results);
        this.articles = results;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  computeStats(all: NewsArticle[]): void {
    this.bullishCount = all.filter(a => a.sentimentScore >= 0.2).length;
    this.bearishCount = all.filter(a => a.sentimentScore <= -0.2).length;
    this.avgSentiment = all.length
      ? all.reduce((s, a) => s + (a.sentimentScore ?? 0), 0) / all.length
      : 0;

    const map: Record<string, { count: number; total: number }> = {};
    for (const a of all) {
      if (!a.mentionedTickers) continue;
      for (const t of a.mentionedTickers.split(',').map(s => s.trim()).filter(Boolean)) {
        if (!map[t]) map[t] = { count: 0, total: 0 };
        map[t].count++;
        map[t].total += a.sentimentScore ?? 0;
      }
    }
    this.hotTickers = Object.entries(map)
      .map(([ticker, v]) => ({ ticker, count: v.count, avgScore: v.total / v.count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 12);
  }

  get totalArticles(): number { return this.page?.totalElements ?? 0; }

  get avgSentimentClass(): string {
    return this.avgSentiment >= 0.2 ? 'bullish-text' : this.avgSentiment <= -0.2 ? 'bearish-text' : '';
  }

  get pageNumbers(): number[] {
    if (!this.page) return [];
    const total = this.page.totalPages;
    const cur = this.currentPage;
    const start = Math.max(0, cur - 2);
    const end = Math.min(total - 1, cur + 2);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  onSearchChange(val: string): void { this.search$.next(val); }

  reload(): void { this.currentPage = 0; this.loadNews(); }

  clearSearch(): void { this.searchQuery = ''; this.reload(); }

  goPage(p: number): void { this.currentPage = p; this.loadNews(); }

  filterByTicker(ticker: string): void { this.searchQuery = ticker; this.reload(); }

  toggleFilter(f: 'bull' | 'bear'): void {
    if (this.activeFilter === f) {
      this.activeFilter = 'all';
    } else {
      this.activeFilter = f;
    }
    this.reload();
  }

  sentimentClass(score: number): string {
    return score >= 0.2 ? 'bull' : score <= -0.2 ? 'bear' : 'neut';
  }

  cardClass(score: number): string { return score >= 0.2 ? 'pos' : score <= -0.2 ? 'neg' : ''; }
  barClass(score: number): string { return score >= 0.2 ? 'bull' : score <= -0.2 ? 'bear' : 'neut'; }
  scoreClass(score: number): string { return score >= 0.2 ? 'bull' : score <= -0.2 ? 'bear' : 'neut'; }
}
