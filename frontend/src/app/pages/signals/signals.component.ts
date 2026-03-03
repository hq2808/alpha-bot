import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalService, NewsArticle, NewsPage, MarketData, TickerSignal } from '../../services/signal.service';
import { createChart, IChartApi } from 'lightweight-charts';

@Component({
  selector: 'app-signals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="signals-page">
      <div class="page-header">
        <h1>📰 News Insight & Analysis</h1>
        <p class="subtitle">Phân tích chuyên sâu từng bài báo tài chính (Real-time)</p>
        <div class="filter-row">
          <label>Ngưỡng tâm lý:</label>
          <input type="range" min="0" max="1" step="0.05"
                 [(ngModel)]="threshold"
                 (change)="load()" />
          <span class="threshold-val">≥ {{ threshold | number:'1.2-2' }}</span>

          <div class="filter-divider"></div>

          <!-- Search -->
          <div class="search-bar">
            <span>🔍</span>
            <input type="text" placeholder="Tìm theo tiêu đề, mã, nguồn..."
                   [(ngModel)]="searchQuery"
                   (ngModelChange)="onSearchChange()"
                   class="search-input" />
            @if (searchQuery) {
              <button class="clear-btn" (click)="clearSearch()">✕</button>
            }
          </div>

          <div class="filter-divider"></div>

          <label class="switch-block">
            <span class="switch-label">Chỉ hiện tin trong Watchlist</span>
            <div class="switch">
              <input type="checkbox" [(ngModel)]="filterByWatchlist" (change)="toggleWatchlistFilter()">
              <span class="slider round"></span>
            </div>
          </label>
        </div>
      </div>

      <!-- ── Khuyến nghị Mua / Bán ─────────────────────────────── -->
      @if (tickerSignals.length > 0) {
        <div class="rec-section">
          <h2 class="rec-title">📊 Khuyến nghị Mua / Bán theo tin tức</h2>
          <div class="rec-table-wrap">
            <table class="rec-table">
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Khuyến nghị</th>
                  <th>Tâm lý TB</th>
                  <th>Số bài</th>
                  <th>Tin gần nhất</th>
                </tr>
              </thead>
              <tbody>
                @for (sig of tickerSignals; track sig.ticker) {
                  <tr class="rec-row" [class]="recRowClass(sig.signal)" (click)="filterByTicker(sig.ticker)">
                    <td class="rec-ticker">{{ sig.ticker }}</td>
                    <td>
                      <span class="rec-badge" [class]="recBadgeClass(sig.signal)">
                        {{ signalLabel(sig.signal) }}
                      </span>
                    </td>
                    <td class="rec-score" [class]="sig.averageSentiment >= 0.5 ? 'pos' : sig.averageSentiment <= -0.5 ? 'neg' : 'neu'">
                      {{ sig.averageSentiment | number:'1.2-2' }}
                    </td>
                    <td class="rec-count">{{ sig.mentionCount }}</td>
                    <td class="rec-headline">{{ sig.lastNewsTitle }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <p class="rec-note">⚡ Nhấn vào dòng để lọc tin theo mã · Dữ liệu tổng hợp từ AI phân tích báo, chỉ mang tính tham khảo</p>
        </div>
      }


      <!-- Results info -->
      @if (newsPage) {
        <div class="results-info">
          {{ newsPage.totalElements }} bài viết
          @if (searchQuery) { — "<strong>{{ searchQuery }}</strong>" }
        </div>
      }

      <div class="signals-grid">
        @if (loading) {
          <div class="loading-card">⏳ Đang tải...</div>
        } @else {
          @for (article of newsPage?.content ?? []; track article.id) {
            <div class="signal-card">
              <div class="card-top">
                <span class="source-badge">{{ article.source }}</span>
                <span class="score">{{ article.sentimentScore | number:'1.2-2' }}</span>
              </div>
              <a [href]="article.url" target="_blank" class="title">{{ article.title }}</a>
              @if (article.tags) {
                <div class="article-tags">{{ article.tags }}</div>
              }
              @if (article.mentionedTickers) {
                <div class="tickers">
                  @for (t of article.mentionedTickers.split(','); track t) {
            <span class="chip" (click)="filterByTicker(t.trim()); $event.stopPropagation()">{{ t.trim() }}</span>
                  }
                </div>
              }
              <span class="time">{{ (article.publishedAt || article.crawledAt) | date:'dd/MM HH:mm' }}</span>
            </div>
          } @empty {
            <div class="empty">
              @if (filterByWatchlist) {
                <p>Watchlist của bạn đang trống hoặc chưa có tin tức nào nổi bật.</p>
                <button class="add-btn" (click)="filterByWatchlist=false; load()">Xem toàn bộ tin tức</button>
              } @else {
                <p>Không có tin tức nào trên ngưỡng {{ threshold | number:'1.2-2' }}</p>
                <small>Hệ thống đang tiếp tục theo dõi thị trường...</small>
              }
            </div>
          }
        }
      </div>

      <!-- Pagination -->
      @if (newsPage && newsPage.totalPages > 1) {
        <div class="pagination">
          <button class="page-btn" (click)="goToPage(0)" [disabled]="currentPage === 0">«</button>
          <button class="page-btn" (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 0">‹</button>
          @for (p of pageNumbers(); track p) {
            <button class="page-btn" [class.active]="p === currentPage" (click)="goToPage(p)">{{ p + 1 }}</button>
          }
          <button class="page-btn" (click)="goToPage(currentPage + 1)" [disabled]="currentPage >= newsPage.totalPages - 1">›</button>
          <button class="page-btn" (click)="goToPage(newsPage.totalPages - 1)" [disabled]="currentPage >= newsPage.totalPages - 1">»</button>
          <span class="page-info">Trang {{ currentPage + 1 }} / {{ newsPage.totalPages }}</span>
        </div>
      }

    </div>
  `,
  styles: [`
    .signals-page { padding: 20px; }
    .news-panel-section { margin-top: 32px; background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 16px; }
    .page-header { margin-bottom: 24px; }
    h1 { color: #e6edf3; font-size: 1.4rem; margin-bottom: 4px; border-bottom: 1px solid #30363d; padding-bottom: 8px; }
    .subtitle { color: #8b949e; font-size: 0.9rem; margin-top: 0; margin-bottom: 16px; }

    /* News Grid styling */
    .filter-row { display: flex; align-items: center; gap: 12px; font-size: 0.9rem; color: #8b949e; margin-bottom: 8px; flex-wrap: wrap; }
    .filter-divider { width: 1px; height: 16px; background: #30363d; margin: 0 8px; }
    input[type=range] { width: 160px; accent-color: #3fb950; cursor: pointer; }
    .threshold-val { color: #3fb950; font-weight: 700; }

    /* Toggle Switch */
    .switch-block { display: flex; align-items: center; gap: 8px; cursor: pointer; }
    .switch-label { font-weight: 500; }
    .switch { position: relative; display: inline-block; width: 36px; height: 20px; }
    .switch input { opacity: 0; width: 0; height: 0; }
    .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #21262d; transition: .3s; border: 1px solid #30363d;}
    .slider:before { position: absolute; content: ""; height: 14px; width: 14px; left: 2px; bottom: 2px; background-color: #8b949e; transition: .3s; }
    input:checked + .slider { background-color: #1f6feb; border-color: #1f6feb; }
    input:checked + .slider:before { transform: translateX(16px); background-color: white; }
    .slider.round { border-radius: 20px; }
    .slider.round:before { border-radius: 50%; }

    /* ── Recommendation Table ─────────────────────────────── */
    .rec-section { margin-bottom: 24px; }
    .rec-title { font-size: 1rem; color: #e6edf3; margin: 0 0 12px; font-weight: 700; }
    .rec-table-wrap { overflow-x: auto; border: 1px solid #30363d; border-radius: 8px; }
    .rec-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
    .rec-table thead th { background: #161b22; color: #8b949e; text-align: left; padding: 9px 12px; border-bottom: 1px solid #30363d; font-weight: 600; font-size: 0.8rem; white-space: nowrap; }
    .rec-table thead th:nth-child(4) { text-align: center; }
    .rec-row { border-bottom: 1px solid #21262d; cursor: pointer; transition: background 0.12s; }
    .rec-row:hover { background: #161b22; }
    .rec-row.buy-row  { border-left: 3px solid #3fb950; }
    .rec-row.sell-row { border-left: 3px solid #f85149; }
    .rec-row.neu-row  { border-left: 3px solid #30363d; }
    .rec-row td { padding: 9px 12px; vertical-align: middle; }
    .rec-ticker { font-size: 1rem; font-weight: 900; color: #58a6ff; white-space: nowrap; }
    .rec-badge { display: inline-block; padding: 3px 10px; border-radius: 4px; font-weight: 800; font-size: 0.75rem; white-space: nowrap; }
    .rec-badge.buy-b  { background: rgba(35,134,54,0.15); color: #3fb950; border: 1px solid #238636; }
    .rec-badge.sell-b { background: rgba(218,54,51,0.15);  color: #f85149; border: 1px solid #da3633; }
    .rec-badge.neu-b  { background: #21262d; color: #8b949e; border: 1px solid #30363d; }
    .rec-score { font-weight: 700; white-space: nowrap; }
    .rec-score.pos { color: #3fb950; }
    .rec-score.neg { color: #f85149; }
    .rec-score.neu { color: #8b949e; }
    .rec-count { color: #8b949e; text-align: center; }
    .rec-headline { color: #6e7681; font-size: 0.8rem; max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .rec-note { font-size: 0.75rem; color: #6e7681; margin-top: 8px; margin-bottom: 0; }

    /* Mini Charts */

    .mini-charts-section { margin-bottom: 24px; padding-bottom: 24px; border-bottom: 1px solid #30363d; }
    .mini-charts-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .mini-charts-header h3 { font-size: 1.1rem; color: #e6edf3; margin: 0; }
    .mini-charts-count { font-size: 0.8rem; color: #8b949e; }
    .mini-charts-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .mini-chart-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 12px; cursor: pointer; transition: border-color 0.2s; }
    .mini-chart-card:hover { border-color: #58a6ff; }
    .mini-chart-title { font-size: 0.9rem; font-weight: 700; color: #58a6ff; margin-bottom: 8px; }
    .mini-chart-container { width: 100%; height: 200px; }
    
    .add-btn { margin-top: 12px; background: transparent; border: 1px solid #58a6ff; color: #58a6ff; padding: 6px 12px; border-radius: 6px; cursor: pointer; transition: all 0.2s;}
    .add-btn:hover { background: rgba(88, 166, 255, 0.1); }

    .signals-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
    .signal-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 16px; display: flex; flex-direction: column; gap: 12px; border-left: 4px solid #3fb950; }
    .card-top { display: flex; justify-content: space-between; align-items: center; }
    .source-badge { background: #21262d; color: #8b949e; font-size: 0.7rem; padding: 2px 6px; border-radius: 4px; }
    .score { font-size: 1.1rem; font-weight: 800; color: #3fb950; }
    .title { font-size: 0.95rem; font-weight: 600; color: #e6edf3; text-decoration: none; line-height: 1.4; }
    .title:hover { color: #58a6ff; }
    .tickers { display: flex; gap: 6px; flex-wrap: wrap; }
    .chip { background: rgba(88,166,255,0.1); color: #58a6ff; font-size: 0.7rem; font-weight: 700; padding: 1px 6px; border-radius: 3px; border: 1px solid rgba(88,166,255,0.2); cursor: pointer; transition: all 0.2s; }
    .chip:hover { background: rgba(88,166,255,0.2); transform: translateY(-1px); }
    .time { font-size: 0.75rem; color: #6e7681; margin-top: auto; }
    .article-tags { font-size: 0.8rem; color: #d2a8ff; font-weight: 500; }
    .empty { grid-column: 1/-1; text-align: center; padding: 100px; color: #8b949e; }

    .results-info { font-size: 0.78rem; color: #6e7681; margin-bottom: 8px; }
    .results-info strong { color: #e6edf3; }

    .search-bar { display: flex; align-items: center; gap: 6px; background: #161b22; border: 1px solid #30363d; border-radius: 6px; padding: 6px 10px; min-width: 220px; flex: 1; max-width: 360px; transition: border-color 0.2s; }
    .search-bar:focus-within { border-color: #58a6ff; }
    .search-input { background: transparent; border: none; outline: none; color: #e6edf3; font-size: 0.85rem; flex: 1; }
    .clear-btn { background: none; border: none; color: #6e7681; cursor: pointer; font-size: 0.85rem; padding: 0; }
    .clear-btn:hover { color: #e6edf3; }

    .loading-card { grid-column: 1/-1; padding: 40px; text-align: center; color: #8b949e; }

    .pagination { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; padding: 16px 0; }
    .page-btn { background: #161b22; border: 1px solid #30363d; color: #8b949e; border-radius: 6px; padding: 5px 10px; cursor: pointer; font-size: 0.82rem; transition: all 0.15s; min-width: 32px; text-align: center; }
    .page-btn:hover:not(:disabled) { background: #21262d; color: #e6edf3; border-color: #58a6ff; }
    .page-btn.active { background: #1f6feb; border-color: #388bfd; color: #fff; font-weight: 700; }
    .page-btn:disabled { opacity: 0.3; cursor: default; }
    .page-info { font-size: 0.78rem; color: #6e7681; margin-left: 6px; }

    /* Modal styling */

    .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(4px); }
    .modal-content { background: #0d1117; border: 1px solid #30363d; border-radius: 12px; width: 90%; max-width: 800px; padding: 20px; box-shadow: 0 8px 24px rgba(0,0,0,0.5); }
    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #21262d; padding-bottom: 12px; }
    .modal-header h2 { margin: 0; font-size: 1.2rem; color: #e6edf3; }
    .close-btn { background: none; border: none; color: #8b949e; font-size: 1.5rem; cursor: pointer; transition: color 0.2s; }
    .close-btn:hover { color: #f85149; }
    .chart-container { width: 100%; height: 400px; border-radius: 8px; overflow: hidden; }
    .hidden { display: none; }
    
    .loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 400px; color: #8b949e; }
    .spinner { width: 40px; height: 40px; border: 3px solid rgba(88,166,255,0.1); border-top-color: #58a6ff; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 16px; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class SignalsComponent implements OnInit, OnDestroy {
  articles: NewsArticle[] = [];
  tickerSignals: TickerSignal[] = [];
  newsPage: NewsPage | null = null;
  searchQuery = '';
  searchTicker = '';
  searchHours?: number;
  currentPage = 0;
  readonly pageSize = 21;
  loading = false;
  threshold = 0.5;
  filterByWatchlist = false;

  watchlistTickers: string[] = [];
  MAX_MINI_CHARTS = 4;
  miniCharts: { [ticker: string]: IChartApi } = {};

  selectedTicker: string | null = null;
  chartData: MarketData[] = [];
  isLoadingChart = false;

  @ViewChild('chartContainer') chartContainer!: ElementRef;
  private chart: IChartApi | null = null;
  private resizeObservers: ResizeObserver[] = [];

  constructor(private signal: SignalService) { }

  ngOnDestroy(): void {
    this.resizeObservers.forEach(ro => ro.disconnect());
    Object.values(this.miniCharts).forEach(c => c.remove());
    if (this.chart) this.chart.remove();
  }

  ngOnInit(): void {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('filterByWatchlist')) {
      this.filterByWatchlist = urlParams.get('filterByWatchlist') === 'true';
    }
    this.loadWatchlist();
    this.load();
    this.signal.getMarketSignals().subscribe(sigs => this.tickerSignals = sigs);
  }

  loadWatchlist() {
    this.signal.getWatchlist().subscribe(items => {
      this.watchlistTickers = items.map(w => w.ticker);
      // Immediately render mini charts for top tickers
      if (this.watchlistTickers.length > 0) {
        this.renderMiniCharts();
      }
    });
  }

  getDisplayTickers(): string[] {
    return this.watchlistTickers.slice(0, this.MAX_MINI_CHARTS);
  }

  toggleWatchlistFilter() {
    // Update URL without reloading
    const url = new URL(window.location.href);
    url.searchParams.set('filterByWatchlist', this.filterByWatchlist.toString());
    window.history.replaceState({}, '', url);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.load();
  }

  load(): void {
    this.loading = true;

    // Convert numeric threshold to classification filter
    let filterType = 'all';
    if (this.threshold >= 0.5) filterType = 'bull';
    else if (this.threshold <= -0.5) filterType = 'bear';

    this.signal.searchNews(this.searchQuery, this.currentPage, this.pageSize, filterType, this.searchTicker, this.searchHours)
      .subscribe({
        next: page => { this.newsPage = page; this.loading = false; },
        error: () => { this.loading = false; }
      });
  }

  onSearchChange(): void {
    this.searchTicker = '';
    this.searchHours = undefined;
    this.currentPage = 0;
    this.load();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchTicker = '';
    this.searchHours = undefined;
    this.currentPage = 0;
    this.load();
  }

  goToPage(page: number): void {
    if (!this.newsPage) return;
    if (page < 0 || page >= this.newsPage.totalPages) return;
    this.currentPage = page;
    this.load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  pageNumbers(): number[] {
    if (!this.newsPage) return [];
    const total = this.newsPage.totalPages;
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 3); i <= Math.min(total - 1, cur + 3); i++) range.push(i);
    return range;
  }

  filterByTicker(ticker: string): void {
    this.searchTicker = ticker;
    this.searchHours = 24;
    this.searchQuery = '';
    this.currentPage = 0;
    this.load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  signalLabel(signal: string): string {
    const map: Record<string, string> = {
      'STRONG BUY': '🟢🟢 MUA MẠNH',
      'BUY': '🟢 MUA',
      'NEUTRAL': '⏸️ TRUNG LẬP',
      'SELL': '🔴 BÁN',
      'STRONG SELL': '🔴🔴 BÁN MẠNH',
    };
    return map[signal?.toUpperCase()] ?? signal;
  }

  recRowClass(signal: string): string {
    const s = signal?.toUpperCase() ?? '';
    if (s.includes('BUY')) return 'buy-row';
    if (s.includes('SELL')) return 'sell-row';
    return 'neu-row';
  }

  recBadgeClass(signal: string): string {
    const s = signal?.toUpperCase() ?? '';
    if (s.includes('BUY')) return 'buy-b';
    if (s.includes('SELL')) return 'sell-b';
    return 'neu-b';
  }

  scrollToTickerNews(ticker: string) {
    // Advanced UX: Scroll window to first occurrence of ticker
    const el = document.evaluate(`//span[contains(text(), '${ticker}')]`, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue as HTMLElement;
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.style.backgroundColor = 'rgba(88, 166, 255, 0.3)';
      setTimeout(() => el.style.backgroundColor = '', 1500);
    }
  }

  renderMiniCharts() {
    const tickers = this.getDisplayTickers();
    if (tickers.length === 0) return;

    this.signal.getHistoricalDataBatch(tickers).subscribe(batchData => {
      setTimeout(() => { // Wait for Angular to render the empty containers
        tickers.forEach(ticker => {
          const data = batchData[ticker];
          if (!data || data.length === 0) return;

          const containerId = 'mini-chart-' + ticker;
          const container = document.getElementById(containerId);
          if (!container) return; // Prevent errors if scrolled too fast

          // Clean existing chart if any
          if (this.miniCharts[ticker]) {
            this.miniCharts[ticker].remove();
          }

          const chart = createChart(container, {
            width: container.clientWidth,
            height: 200,
            layout: { background: { type: 'solid' as any, color: '#161b22' }, textColor: '#8b949e' },
            grid: { vertLines: { color: '#21262d' }, horzLines: { color: '#21262d' } },
            timeScale: { visible: true, borderColor: '#30363d' },
            rightPriceScale: { visible: true, borderColor: '#30363d' },
            handleScroll: true,
            handleScale: true,
            crosshair: { mode: 1 } // Magnet mode
          } as any);

          const ro = new ResizeObserver(entries => {
            if (entries.length === 0 || entries[0].target !== container) return;
            const newRect = entries[0].contentRect;
            chart.applyOptions({ width: newRect.width, height: newRect.height });
          });
          ro.observe(container);
          this.resizeObservers.push(ro);

          const candleSeries = chart.addCandlestickSeries({
            upColor: '#3fb950', downColor: '#f85149', borderVisible: false, wickUpColor: '#3fb950', wickDownColor: '#f85149',
            priceFormat: { type: 'custom', minMove: 0.01, formatter: (p: number) => this.signal.formatPrice(p) }
          });

          // Format for lightweight-charts
          const formatted = data
            .map(d => ({ time: d.date.split('T')[0], open: d.open || 0, high: d.high || 0, low: d.low || 0, close: d.close || 0 }))
            .filter((v, i, a) => a.findIndex(t => t.time === v.time) === i)
            .sort((a, b) => a.time.localeCompare(b.time));

          candleSeries.setData(formatted);

          // Add simple volume overlay
          const volSeries = chart.addHistogramSeries({ color: '#26a69a', priceFormat: { type: 'volume' }, priceScaleId: '' });
          volSeries.priceScale().applyOptions({ scaleMargins: { top: 0.7, bottom: 0 } });
          const volData = formatted.map(d => {
            const orig = data.find(dt => dt.date.split('T')[0] === d.time);
            return { time: d.time, value: orig?.volume || 0, color: d.close >= d.open ? 'rgba(63, 185, 80, 0.4)' : 'rgba(248, 81, 73, 0.4)' };
          });
          volSeries.setData(volData);

          chart.timeScale().fitContent();
          this.miniCharts[ticker] = chart;
        });
      }, 100);
    });
  }

  openChart(ticker: string) {
    this.selectedTicker = ticker;
    this.isLoadingChart = true;

    this.signal.getHistoricalData(ticker).subscribe({
      next: (data) => {
        // Try to find if we have any signal data to help "current" state
        // (SignalsComponent doesn't have live ticks as often as PriceBoard, but we can try)
        this.chartData = data;
        this.isLoadingChart = false;
        // Wait for DOM to update and ViewChild to be available
        setTimeout(() => this.renderChart(), 0);
      },
      error: (err) => {
        console.error('Lỗi khi lấy dữ liệu chart:', err);
        this.isLoadingChart = false;
        this.selectedTicker = null;
      }
    });
  }

  closeChart() {
    this.selectedTicker = null;
    if (this.chart) {
      this.chart.remove();
      this.chart = null;
    }
  }

  renderChart() {
    if (!this.chartContainer || !this.chartContainer.nativeElement) return;

    if (this.chart) {
      this.chart.remove();
    }

    const container = this.chartContainer.nativeElement;
    this.chart = createChart(container, {
      width: container.clientWidth,
      height: 400,
      layout: {
        background: { type: 'solid' as any, color: '#0d1117' },
        textColor: '#8b949e',
      },
      grid: {
        vertLines: { color: '#21262d' },
        horzLines: { color: '#21262d' },
      },
      crosshair: {
        mode: 1, // Magnet
      },
      timeScale: {
        borderColor: '#30363d',
      },
      rightPriceScale: {
        borderColor: '#30363d',
      }
    } as any);

    const ro = new ResizeObserver(entries => {
      if (entries.length === 0 || entries[0].target !== container) return;
      const newRect = entries[0].contentRect;
      this.chart?.applyOptions({ width: newRect.width, height: newRect.height });
    });
    ro.observe(container);
    this.resizeObservers.push(ro);

    const candlestickSeries = this.chart.addCandlestickSeries({
      upColor: '#3fb950',
      downColor: '#f85149',
      borderVisible: false,
      wickUpColor: '#3fb950',
      wickDownColor: '#f85149',
      priceFormat: { type: 'custom', minMove: 1, formatter: (p: number) => p.toLocaleString('en-US') }
    });

    if (this.chartData && this.chartData.length > 0) {
      // Map and filter unique days (lightweight-charts requires strictly ascending time)
      const formattedData = this.chartData
        .map(d => ({
          time: d.date.split('T')[0],
          open: d.open || 0,
          high: d.high || 0,
          low: d.low || 0,
          close: d.close || 0
        }))
        .filter((v, i, a) => a.findIndex(t => t.time === v.time) === i) // Ensure unique dates
        .sort((a, b) => a.time.localeCompare(b.time)); // Sort by date just in case

      // SMA20 Calculation
      const smaData = [];
      const period = 20;
      for (let i = 0; i < formattedData.length; i++) {
        if (i < period - 1) continue;
        let sum = 0;
        for (let j = 0; j < period; j++) {
          sum += formattedData[i - j].close;
        }
        smaData.push({ time: formattedData[i].time, value: sum / period });
      }

      // Volume Data Mapping
      const volumeData = formattedData.map(d => {
        // Find original volume
        const original = this.chartData.find(dt => dt.date.split('T')[0] === d.time);
        return {
          time: d.time,
          value: original?.volume || 0,
          color: d.close >= d.open ? 'rgba(63, 185, 80, 0.4)' : 'rgba(248, 81, 73, 0.4)'
        };
      });

      candlestickSeries.setData(formattedData);

      // Add Volume Histogram Series
      const volumeSeries = this.chart.addHistogramSeries({
        color: '#26a69a',
        priceFormat: { type: 'volume' },
        priceScaleId: '', // blank sets it as overlay
      });
      volumeSeries.priceScale().applyOptions({
        scaleMargins: { top: 0.8, bottom: 0 },
      });
      volumeSeries.setData(volumeData);

      // Add SMA20 Line Series
      const smaSeries = this.chart.addLineSeries({
        color: '#58a6ff',
        lineWidth: 2,
        title: 'SMA 20'
      });
      smaSeries.setData(smaData);
    }

    this.chart.timeScale().fitContent();
  }
}

