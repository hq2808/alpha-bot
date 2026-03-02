import { Component, OnInit, OnDestroy, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { SignalService, NewsArticle, NewsPage } from '../../services/signal.service';

@Component({
  selector: 'app-news-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="news-panel">
      <div class="panel-header">
        <h2 class="panel-title">📡 Live Intelligence Feed
          <span class="badge" [class.pulse]="liveCount > 0" (click)="resetToTop()">
            {{ liveCount }} NEW
          </span>
        </h2>

        <!-- Search bar -->
        <div class="search-bar">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Search by title, source, ticker..."
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearchChange($event)"
            class="search-input"
            id="news-panel-search"
          />
          @if (searchQuery) {
            <button class="clear-btn" (click)="clearSearch()">✕</button>
          }
        </div>
      </div>

      <!-- Results info -->
      @if (newsPage) {
        <div class="results-info">
          {{ newsPage.totalElements }} bài viết
          @if (searchQuery) { — tìm kiếm "<strong>{{ searchQuery }}</strong>" }
        </div>
      }

      <!-- News list (always paginated) -->
      <div class="news-list">
        @if (loading) {
          <div class="loading-state">⏳ Đang tải...</div>
        } @else if (!newsPage?.content?.length) {
          <div class="empty-state">
            @if (searchQuery) { Không tìm thấy bài viết nào cho "{{ searchQuery }}" }
            @else { Crawler đang chạy, chưa có bài viết... }
          </div>
        } @else {
          @for (article of newsPage!.content; track article.id) {
            <div class="news-card" [class]="getSentimentClass(article.sentimentScore)">
              <div class="card-header">
                <span class="source">{{ article.source }}</span>
                <div class="card-meta-right">
                  <span class="pub-date">{{ (article.publishedAt || article.crawledAt) | date:'dd/MM HH:mm' }}</span>
                  <span class="sentiment" [class]="getSentimentClass(article.sentimentScore)">
                    {{ formatSentiment(article.sentimentScore) }}
                  </span>
                </div>
              </div>

              <a [href]="article.url" target="_blank" class="article-title">
                {{ article.title }}
              </a>

              @if (article.aiSummary) {
                <p class="summary">🤖 {{ article.aiSummary }}</p>
              }

              <div class="card-footer">
                @if (article.mentionedTickers) {
                  <div class="tickers">
                    @for (ticker of article.mentionedTickers.split(','); track ticker) {
                      <span class="ticker-chip">{{ ticker.trim() }}</span>
                    }
                  </div>
                }
                @if (article.tags) {
                  <span class="tags">{{ article.tags }}</span>
                }
              </div>
            </div>
          }
        }
      </div>

      <!-- Pagination -->
      @if (newsPage && newsPage.totalPages > 1) {
        <div class="pagination">
          <button class="page-btn" (click)="goToPage(0)" [disabled]="currentPage === 0" title="Trang đầu">«</button>
          <button class="page-btn" (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 0" title="Trang trước">‹</button>

          @for (p of pageNumbers(); track p) {
            <button class="page-btn" [class.active]="p === currentPage" (click)="goToPage(p)">
              {{ p + 1 }}
            </button>
          }

          <button class="page-btn" (click)="goToPage(currentPage + 1)" [disabled]="currentPage >= newsPage.totalPages - 1" title="Trang sau">›</button>
          <button class="page-btn" (click)="goToPage(newsPage.totalPages - 1)" [disabled]="currentPage >= newsPage.totalPages - 1" title="Trang cuối">»</button>

          <span class="page-info">Trang {{ currentPage + 1 }} / {{ newsPage.totalPages }}</span>
        </div>
      }
    </div>
  `,
  styles: [`
    .news-panel { display: flex; flex-direction: column; gap: 12px; }

    .panel-header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px; }
    .panel-title { display: flex; align-items: center; gap: 12px; font-size: 1.1rem; margin: 0; color: #e6edf3; }
    .badge {
      background: #da3633; color: white; padding: 2px 9px; border-radius: 12px;
      font-size: 0.72rem; cursor: pointer; transition: opacity 0.2s;
    }
    .badge.pulse { animation: pulse 1.5s infinite; }
    @keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

    .search-bar {
      display: flex; align-items: center; gap: 6px;
      background: #0d1117; border: 1px solid #30363d; border-radius: 6px;
      padding: 7px 10px; min-width: 240px; flex: 1; max-width: 360px;
      transition: border-color 0.2s;
    }
    .search-bar:focus-within { border-color: #58a6ff; }
    .search-icon { font-size: 0.85rem; }
    .search-input { background: transparent; border: none; outline: none; color: #e6edf3; font-size: 0.85rem; flex: 1; min-width: 0; }
    .clear-btn { background: none; border: none; color: #6e7681; cursor: pointer; font-size: 0.85rem; padding: 0; line-height: 1; }
    .clear-btn:hover { color: #e6edf3; }

    .results-info { font-size: 0.78rem; color: #6e7681; }
    .results-info strong { color: #e6edf3; }

    .news-list { display: flex; flex-direction: column; gap: 10px; }

    .news-card {
      padding: 12px 14px; border-radius: 8px; border-left: 4px solid #30363d;
      background: rgba(255,255,255,0.04); transition: background 0.15s;
    }
    .news-card:hover { background: rgba(255,255,255,0.07); }
    .news-card.bullish { border-left-color: #3fb950; }
    .news-card.bearish { border-left-color: #f85149; }

    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; font-size: 0.78rem; gap: 8px; }
    .source { color: #8b949e; white-space: nowrap; }
    .card-meta-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
    .pub-date { color: #6e7681; font-size: 0.75rem; white-space: nowrap; }
    .sentiment { font-weight: 700; font-size: 0.78rem; }
    .sentiment.bullish { color: #3fb950; }
    .sentiment.bearish { color: #f85149; }
    .sentiment.neutral  { color: #8b949e; }

    .article-title {
      display: block; font-weight: 600; color: #e6edf3; text-decoration: none;
      margin-bottom: 5px; line-height: 1.4; font-size: 0.9rem;
    }
    .article-title:hover { color: #58a6ff; text-decoration: underline; }
    .summary { font-size: 0.82rem; color: #8b949e; margin: 4px 0; font-style: italic; }

    .card-footer { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; align-items: center; }
    .tickers { display: flex; gap: 4px; flex-wrap: wrap; }
    .ticker-chip { background: rgba(63,185,80,0.15); color: #3fb950; padding: 1px 7px; border-radius: 4px; font-size: 0.72rem; font-weight: 700; }
    .tags { font-size: 0.72rem; color: #6e7681; }

    /* Pagination */
    .pagination {
      display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
      padding-top: 12px; border-top: 1px solid #21262d;
    }
    .page-btn {
      background: #161b22; border: 1px solid #30363d; color: #8b949e;
      border-radius: 6px; padding: 5px 10px; cursor: pointer; font-size: 0.82rem;
      transition: all 0.15s; min-width: 32px; text-align: center;
    }
    .page-btn:hover:not(:disabled) { background: #21262d; color: #e6edf3; border-color: #58a6ff; }
    .page-btn.active { background: #1f6feb; border-color: #388bfd; color: #fff; font-weight: 700; }
    .page-btn:disabled { opacity: 0.3; cursor: default; }
    .page-info { font-size: 0.78rem; color: #6e7681; margin-left: 6px; white-space: nowrap; }

    .loading-state, .empty-state { padding: 32px; text-align: center; color: #8b949e; font-size: 0.85rem; }
  `]
})
export class NewsPanelComponent implements OnInit, OnDestroy, OnChanges {
  /** Optional: if parent provides articles, used only as initial seed (page 0 still loads from API). */
  @Input() articles: NewsArticle[] | null = null;

  newsPage: NewsPage | null = null;
  searchQuery = '';
  currentPage = 0;
  readonly pageSize = 20;
  loading = false;
  liveCount = 0;

  private subs: Subscription[] = [];
  private searchSubject = new Subject<string>();

  constructor(public signal: SignalService) { }

  ngOnChanges(_: SimpleChanges): void {
    // Parent articles input is kept for API compatibility but we always load from paginated API
  }

  ngOnInit(): void {
    this.loadPage();

    // Real-time badge via WebSocket — just increment counter, don't mutate page list
    this.subs.push(
      this.signal.getLiveNews().subscribe(() => {
        this.liveCount++;
        // Reload page 0 if user is on page 0 to show latest
        if (this.currentPage === 0) this.loadPage();
      })
    );

    // Debounced search
    this.subs.push(
      this.searchSubject.pipe(debounceTime(400), distinctUntilChanged()).subscribe(() => {
        this.currentPage = 0;
        this.loadPage();
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  onSearchChange(q: string) {
    this.searchSubject.next(q);
  }

  clearSearch() {
    this.searchQuery = '';
    this.currentPage = 0;
    this.loadPage();
  }

  loadPage() {
    this.loading = true;
    this.signal.searchNews(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: page => { this.newsPage = page; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  resetToTop() {
    this.liveCount = 0;
    this.currentPage = 0;
    this.loadPage();
  }

  goToPage(page: number) {
    if (!this.newsPage) return;
    if (page < 0 || page >= this.newsPage.totalPages) return;
    this.currentPage = page;
    this.loadPage();
    // Scroll to top of panel
    const el = document.querySelector('app-news-panel');
    el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  pageNumbers(): number[] {
    if (!this.newsPage) return [];
    const total = this.newsPage.totalPages;
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 3); i <= Math.min(total - 1, cur + 3); i++) {
      range.push(i);
    }
    return range;
  }

  getSentimentClass(score: number): string {
    if (score >= 0.5) return 'bullish';
    if (score <= -0.5) return 'bearish';
    return 'neutral';
  }

  formatSentiment(score: number): string {
    if (score >= 0.5) return `🐂 ${score.toFixed(2)}`;
    if (score <= -0.5) return `🐻 ${score.toFixed(2)}`;
    return `— ${score.toFixed(2)}`;
  }
}
