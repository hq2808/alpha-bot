import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalService, TickerSignal, MarketData, VnStock } from '../../services/signal.service';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { CdkDragDrop, CdkDropList, CdkDrag, moveItemInArray } from '@angular/cdk/drag-drop';
import { MiniChartComponent } from '../../components/mini-chart/mini-chart.component';

@Component({
  selector: 'app-intelligence',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, MiniChartComponent],
  template: `
    <div class="intel-page">
      <div class="page-header">
        <div>
          <h1>📡 Market Intelligence</h1>
          <p class="subtitle">Phân tích tổng hợp thị trường · Cập nhật tự động</p>
        </div>
        <div class="last-update">🕐 {{ lastUpdate | date:'HH:mm:ss' }}</div>
      </div>

      <!-- ── Stats Row ─────────────────────────────────────────── -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-label">🔥 Trending</div>
          <div class="stat-value">{{ trendingTicker }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">🐂 Market Mood</div>
          <div class="stat-value" [style.color]="overallColor">{{ overallSentiment }}%</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">📡 Tín hiệu</div>
          <div class="stat-value">{{ signals.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">📅 Ngày phân tích</div>
          <div class="stat-value">{{ trendDays }}</div>
        </div>
      </div>

      <!-- ── AI Daily Summary ───────────────────────────────────── -->
      @if (aiSummary) {
        <div class="ai-summary-card">
          <div class="ai-header">
            <span class="ai-icon">🧠</span>
            <span class="ai-title">AI Market Summary hôm nay</span>
            <span class="ai-date">{{ today }}</span>
          </div>
          <p class="ai-text">{{ aiSummary }}</p>
          <div class="ai-signals">
            @for (sig of signals.slice(0, 5); track sig.ticker) {
              <span class="ai-chip" [class]="sentimentClass(sig.averageSentiment)" (click)="navigateToSignals(sig.ticker)">
                {{ sig.ticker }} {{ sig.signal }}
              </span>
            }
          </div>
        </div>
      }

      <!-- ── Two-column layout: Trend + Gainers/Losers ─────────── -->
      <div class="two-col">

        <!-- Sentiment Trend Chart -->
        <div class="panel" id="trend-panel">
          <h2 class="panel-title">📈 Sentiment Trend (30 ngày)</h2>
          @if (trendLoading) {
            <div class="loading">⏳ Đang tải dữ liệu...</div>
          } @else if (trendData.length === 0) {
            <div class="loading">Chưa có đủ dữ liệu (cần ít nhất 2 ngày)</div>
          } @else {
            <canvas #trendChart id="trendChart" height="220"></canvas>
          }
        </div>

        <!-- Top Gainers / Losers -->
        <div class="panel">
          <h2 class="panel-title">🏆 Top Gainers / Losers</h2>
          <div class="gl-table">
            <div class="gl-header">
              <span>Mã</span>
              <span>Tín hiệu</span>
              <span class="right">Tâm lý</span>
              <span class="right">Bài</span>
            </div>
            @for (sig of topGainersLosers; track sig.ticker) {
              <div class="gl-row" [class]="sentimentClass(sig.averageSentiment)" (click)="navigateToSignals(sig.ticker)">
                <span class="gl-ticker">{{ sig.ticker }}</span>
                <span class="gl-signal" [class]="sentimentClass(sig.averageSentiment)">
                  {{ signalEmoji(sig.signal) }} {{ sig.signal }}
                </span>
                <span class="gl-score right" [class]="sentimentClass(sig.averageSentiment)">
                  {{ sig.averageSentiment | number:'1.2-2' }}
                </span>
                <span class="gl-count right">{{ sig.mentionCount }}</span>
              </div>
            } @empty {
              <div class="loading">Không có tín hiệu nào...</div>
            }
          </div>
          <p class="gl-note">⚡ Click vào dòng để xem tin tức của mã đó tại /signals</p>
        </div>
      </div>

      <!-- ── Watchlist Management ─────────────────────────── -->
      <div class="panel wl-panel">
        <h2 class="panel-title">⭐ Watchlist</h2>
        <p class="wl-sub">Chỉ các mã trong danh sách này sẽ nhận Telegram alert. Nếu trống, tất cả tín hiệu tích cực đều được gửi.</p>
        <div class="wl-add-row" (click)="$event.stopPropagation()">
          <div class="custom-dropdown" [class.open]="isDropdownOpen">
            <div class="dropdown-header" (click)="toggleDropdown()">
              <span>-- Bấm để chọn nhiều mã cổ phiếu --</span>
              <span class="arrow" [class.up]="isDropdownOpen">▼</span>
            </div>
            
            @if (isDropdownOpen) {
              <div class="dropdown-list">
                <div class="dropdown-actions">
                  <button class="close-btn" (click)="closeDropdown()">✕ Đóng</button>
                </div>
                
                @for (sector of stockSectors; track sector) {
                  <div class="optgroup-label">{{ sector }}</div>
                  @for (s of stocksBySector(sector); track s.ticker) {
                    <div class="option-item" 
                         [class.disabled]="isInWatchlist(s.ticker)"
                         (click)="addFromCustomDropdown(s.ticker)">
                      <span class="opt-ticker">{{ s.ticker }}</span>
                      <span class="opt-name">{{ s.companyName }}</span>
                      @if (isInWatchlist(s.ticker)) {
                        <span class="opt-added">✓</span>
                      }
                    </div>
                  }
                }
              </div>
            }
          </div>
        </div>
        <div class="wl-chips">
          @for (item of watchlistItems; track item.ticker) {
            <div class="wl-chip">
              <span>{{ item.ticker }}</span>
              <button class="wl-del" (click)="removeTicker(item.ticker)">✕</button>
            </div>
          } @empty {
            <p class="wl-empty">Watchlist trống — nhận tất cả alert.</p>
          }
        </div>
      </div>

      <!-- ── Mini Price Charts (Watchlist) ─────────────────────── -->
      @if (watchlistTickers.length > 0) {
        <div class="panel">
          <h2 class="panel-title">📊 Biểu đồ giá Watchlist</h2>
          <div class="mini-charts-grid" cdkDropList (cdkDropListDropped)="dropWatchlistChart($event)">
            @for (ticker of displayTickers; track ticker) {
              <div cdkDrag class="draggable-wrapper">
                <app-mini-chart 
                  [ticker]="ticker" 
                  [data]="batchData[ticker] || []"
                  [draggable]="true"
                  [showClose]="true"
                  (remove)="removeTicker(ticker)">
                </app-mini-chart>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .intel-page { display: flex; flex-direction: column; gap: 20px; animation: fadeIn 0.4s ease-out; }

    .page-header { display: flex; justify-content: space-between; align-items: flex-end; border-bottom: 2px solid #30363d; padding-bottom: 14px; }
    h1 { margin: 0; font-size: 1.7rem; color: #58a6ff; font-weight: 800; }
    .subtitle { margin: 4px 0 0; color: #8b949e; font-size: 0.9rem; }
    .last-update { font-size: 0.78rem; color: #6e7681; font-family: monospace; }

    /* Stats */
    .stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; }
    .stat-card { background: #161b22; border: 1px solid #30363d; border-radius: 10px; padding: 14px; }
    .stat-label { font-size: 0.75rem; color: #8b949e; text-transform: uppercase; font-weight: 600; margin-bottom: 6px; }
    .stat-value { font-size: 1.4rem; font-weight: 800; color: #e6edf3; }

    /* AI Summary */
    .ai-summary-card { background: linear-gradient(135deg, #161b22 0%, #1a2332 100%); border: 1px solid #1f6feb; border-radius: 12px; padding: 18px 20px; }
    .ai-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
    .ai-icon { font-size: 1.2rem; }
    .ai-title { font-weight: 700; color: #79c0ff; font-size: 0.95rem; flex: 1; }
    .ai-date { font-size: 0.75rem; color: #6e7681; }
    .ai-text { color: #c9d1d9; font-size: 0.9rem; line-height: 1.6; margin: 0 0 14px; }
    .ai-signals { display: flex; flex-wrap: wrap; gap: 6px; }
    .ai-chip { padding: 3px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: opacity 0.15s; }
    .ai-chip:hover { opacity: 0.8; }
    .ai-chip.bullish { background: rgba(63,185,80,0.15); color: #3fb950; border: 1px solid #238636; }
    .ai-chip.bearish { background: rgba(248,81,73,0.15); color: #f85149; border: 1px solid #da3633; }
    .ai-chip.neutral  { background: #21262d; color: #8b949e; border: 1px solid #30363d; }

    /* Two columns */
    .two-col { display: grid; grid-template-columns: 1fr 340px; gap: 16px; }
    @media (max-width: 900px) { .two-col { grid-template-columns: 1fr; } }

    /* Panel */
    .panel { background: #161b22; border: 1px solid #30363d; border-radius: 10px; padding: 18px; }
    .panel-title { font-size: 1rem; font-weight: 700; color: #e6edf3; margin: 0 0 14px; }

    /* Trend chart */
    canvas { display: block; width: 100%; }
    .loading { color: #8b949e; font-size: 0.85rem; padding: 30px; text-align: center; }

    /* Gainers / Losers table */
    .gl-table { display: flex; flex-direction: column; gap: 0; }
    .gl-header { display: grid; grid-template-columns: 60px 1fr 60px 40px; padding: 6px 8px; font-size: 0.72rem; font-weight: 600; color: #6e7681; text-transform: uppercase; border-bottom: 1px solid #21262d; }
    .gl-row { display: grid; grid-template-columns: 60px 1fr 60px 40px; padding: 9px 8px; border-bottom: 1px solid #21262d; cursor: pointer; transition: background 0.12s; border-left: 3px solid transparent; }
    .gl-row:hover { background: #21262d; }
    .gl-row.bullish { border-left-color: #3fb950; }
    .gl-row.bearish { border-left-color: #f85149; }
    .gl-ticker { font-weight: 800; font-size: 0.9rem; color: #58a6ff; }
    .gl-signal { font-size: 0.72rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; align-self: center; }
    .gl-signal.bullish { color: #3fb950; }
    .gl-signal.bearish { color: #f85149; }
    .gl-signal.neutral  { color: #8b949e; }
    .gl-score { font-weight: 700; font-size: 0.85rem; align-self: center; }
    .gl-score.bullish { color: #3fb950; }
    .gl-score.bearish { color: #f85149; }
    .gl-score.neutral  { color: #8b949e; }
    .gl-count { color: #6e7681; font-size: 0.8rem; align-self: center; }
    .right { text-align: right; }
    .gl-note { font-size: 0.72rem; color: #6e7681; margin: 8px 0 0; }

    /* Mini charts */
    .mini-charts-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .draggable-wrapper {
      cursor: grab;
    }
    .draggable-wrapper:active { cursor: grabbing; }
    .cdk-drag-preview {
      box-sizing: border-box;
      border-radius: 8px;
      box-shadow: 0 5px 15px -3px rgba(0, 0, 0, 0.5),
                  0 8px 20px 1px rgba(0, 0, 0, 0.4);
      opacity: 0.9;
    }
    .cdk-drag-placeholder { opacity: 0; }
    .cdk-drag-animating { transition: transform 250ms cubic-bezier(0, 0, 0.2, 1); }
    .mini-charts-grid.cdk-drop-list-dragging .draggable-wrapper:not(.cdk-drag-placeholder) {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    /* Custom Dropdown Watchlist panel */
    .wl-panel { overflow: visible; }
    .wl-sub { font-size: 0.8rem; color: #6e7681; margin: -8px 0 14px; }
    
    .wl-add-row { position: relative; width: 100%; margin-bottom: 16px; z-index: 100; }
    .custom-dropdown { position: relative; width: 100%; }
    .dropdown-header {
      padding: 10px 14px; background: #21262d; border: 1px solid #30363d;
      border-radius: 6px; color: #e6edf3; cursor: pointer; display: flex;
      justify-content: space-between; align-items: center; font-size: 0.95rem;
      transition: border-color 0.15s;
    }
    .custom-dropdown.open .dropdown-header { border-color: #58a6ff; }
    .arrow { font-size: 0.7rem; color: #8b949e; transition: transform 0.2s; }
    .arrow.up { transform: rotate(180deg); }
    
    .dropdown-list {
      position: absolute; top: 100%; left: 0; right: 0; margin-top: 4px;
      background: #161b22; border: 1px solid #30363d; border-radius: 8px;
      max-height: 350px; overflow-y: auto; box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    }
    .dropdown-actions {
      padding: 8px 12px; border-bottom: 1px solid #30363d; display: flex; justify-content: flex-end;
      position: sticky; top: 0; background: #161b22; z-index: 2;
    }
    .close-btn {
      background: #21262d; border: 1px solid #30363d; color: #e6edf3; border-radius: 4px;
      padding: 4px 10px; font-size: 0.75rem; cursor: pointer; transition: all 0.15s;
    }
    .close-btn:hover { background: #f85149; border-color: #f85149; color: #fff; }
    
    .optgroup-label {
      padding: 8px 12px; color: #8b949e; font-weight: 700; font-size: 0.75rem;
      background: #0d1117; text-transform: uppercase; letter-spacing: 0.5px;
    }
    .option-item {
      padding: 8px 12px 8px 20px; color: #e6edf3; display: flex; align-items: center; gap: 8px;
      cursor: pointer; transition: background 0.1s; border-bottom: 1px solid #21262d;
    }
    .option-item:hover:not(.disabled) { background: #1f6feb33; }
    .option-item.disabled { opacity: 0.5; cursor: default; }
    
    .opt-ticker { font-weight: 700; width: 45px; color: #58a6ff; }
    .opt-name { font-size: 0.85rem; color: #c9d1d9; flex: 1; }
    .opt-added { color: #3fb950; font-weight: bold; }

    .wl-chips { display: flex; flex-wrap: wrap; gap: 8px; min-height: 42px; }
    .wl-chip { display: flex; align-items: center; gap: 6px; background: #21262d; border: 1px solid #30363d; border-radius: 6px; padding: 5px 10px; font-weight: 700; color: #58a6ff; font-size: 0.88rem; }
    .wl-del { background: none; border: none; color: #8b949e; cursor: pointer; font-size: 0.85rem; padding: 0; line-height: 1; }
    .wl-del:hover { color: #f85149; }
    .wl-empty { color: #6e7681; font-size: 0.82rem; font-style: italic; margin: 0; }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class IntelligenceComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('trendChart') trendChartCanvas?: ElementRef<HTMLCanvasElement>;

  signals: TickerSignal[] = [];
  lastUpdate = new Date();
  trendingTicker = '—';
  overallSentiment = 0;
  overallColor = '#8b949e';
  today = new Date().toLocaleDateString('vi-VN');
  aiSummary = '';

  trendData: { date: string; avg_sentiment: number; article_count: number }[] = [];
  trendDays = 0;
  trendLoading = true;

  watchlistTickers: string[] = [];
  watchlistItems: { ticker: string; id?: number }[] = [];
  isDropdownOpen = false;
  stockCatalog: VnStock[] = [];
  readonly MAX_MINI = 20;

  batchData: { [t: string]: MarketData[] } = {};

  private sub?: Subscription;
  private trendChartInstance: any = null; // Chart.js instance

  constructor(private signalService: SignalService, private router: Router) { }

  get displayTickers() { return this.watchlistTickers.slice(0, this.MAX_MINI); }
  get topGainersLosers() {
    return [...this.signals].sort((a, b) => b.averageSentiment - a.averageSentiment);
  }

  ngOnInit() {
    // Load signals
    this.signalService.getMarketSignals().subscribe(sigs => {
      this.signals = sigs;
      this.lastUpdate = new Date();
      if (sigs.length > 0) {
        this.trendingTicker = sigs[0].ticker;
        const avg = sigs.reduce((a, b) => a + b.averageSentiment, 0) / sigs.length;
        this.overallSentiment = Math.round(avg * 100);
        this.overallColor = avg >= 0.3 ? '#3fb950' : avg <= -0.3 ? '#f85149' : '#8b949e';
        this.buildAiSummary(sigs);
      }
    });

    // Load sentiment trend
    this.signalService.getSentimentTrend().subscribe({
      next: data => {
        this.trendData = data;
        this.trendDays = data.length;
        this.trendLoading = false;
        setTimeout(() => this.renderTrendChart(), 50);
      },
      error: () => { this.trendLoading = false; }
    });

    // Load watchlist for mini price charts + panel
    this.loadWatchlistItems();

    // Load VN stock catalog for dropdown
    this.signalService.getStocks().subscribe(stocks => this.stockCatalog = stocks);
  }

  loadWatchlistItems() {
    this.signalService.getWatchlist().subscribe(items => {
      this.watchlistItems = items.map(w => ({ ticker: w.ticker, id: w.id }));
      this.watchlistTickers = items.map(w => w.ticker);
      if (this.watchlistTickers.length > 0) {
        this.loadMiniChartsData();
      }
    });
  }

  dropWatchlistChart(event: CdkDragDrop<string[]>) {
    moveItemInArray(this.watchlistTickers, event.previousIndex, event.currentIndex);
    // Optionally: save new order to backend if DB supports it.
  }

  private clickListener = this.onDocumentClick.bind(this);

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeDropdown() {
    this.isDropdownOpen = false;
  }

  addFromCustomDropdown(t: string) {
    if (!t || this.isInWatchlist(t)) return;

    this.signalService.addToWatchlist(t).subscribe({
      next: () => {
        this.loadWatchlistItems();
        // Do NOT close dropdown so user can add more
      }
    });
  }

  get stockSectors(): string[] {
    const sectors = new Set(this.stockCatalog.map(s => s.sector));
    return Array.from(sectors).sort();
  }

  stocksBySector(sector: string): VnStock[] {
    return this.stockCatalog.filter(s => s.sector === sector);
  }

  isInWatchlist(ticker: string): boolean {
    return this.watchlistTickers.includes(ticker);
  }

  removeTicker(ticker: string) {
    this.signalService.removeFromWatchlist(ticker).subscribe(() => {
      this.loadWatchlistItems();
    });
  }

  ngAfterViewInit() {
    // Close dropdown strictly when clicking entirely outside
    document.addEventListener('click', this.clickListener);
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.trendChartInstance) this.trendChartInstance.destroy();

    document.removeEventListener('click', this.clickListener);
  }

  onDocumentClick() {
    // If clicked outside the wl-add-row wrapper (handled by stopPropagation in template)
    if (this.isDropdownOpen) {
      this.closeDropdown();
    }
  }

  loadTrendData() { }

  buildAiSummary(sigs: TickerSignal[]) {
    const bullish = sigs.filter(s => s.averageSentiment >= 0.5);
    const bearish = sigs.filter(s => s.averageSentiment <= -0.5);
    const neutral = sigs.filter(s => s.averageSentiment > -0.5 && s.averageSentiment < 0.5);
    const topBuy = bullish.slice(0, 3).map(s => s.ticker).join(', ');
    const topSell = bearish.slice(0, 2).map(s => s.ticker).join(', ');

    let summary = `Thị trường hiện có ${sigs.length} mã được theo dõi. `;
    if (bullish.length > 0) summary += `${bullish.length} mã đang có tín hiệu tích cực${topBuy ? ' (' + topBuy + ')' : ''}. `;
    if (bearish.length > 0) summary += `${bearish.length} mã có tín hiệu tiêu cực${topSell ? ' (' + topSell + ')' : ''}. `;
    if (neutral.length > 0) summary += `${neutral.length} mã trung lập. `;
    if (bullish.length > bearish.length) {
      summary += `Tâm lý chung nghiêng về phía lạc quan.`;
    } else if (bearish.length > bullish.length) {
      summary += `Tâm lý chung nghiêng về phía tiêu cực, cần thận trọng.`;
    } else {
      summary += `Thị trường đang phân kỳ, không có hướng rõ ràng.`;
    }
    this.aiSummary = summary;
  }

  renderTrendChart() {
    const canvas = this.trendChartCanvas?.nativeElement;
    if (!canvas || !this.trendData.length) return;

    // Destroy existing Chart.js instance
    if (this.trendChartInstance) { this.trendChartInstance.destroy(); this.trendChartInstance = null; }

    const labels = this.trendData.map(d => d.date.slice(5)); // MM-DD
    const sentimentValues = this.trendData.map(d => Number(d.avg_sentiment));
    const countValues = this.trendData.map(d => Number(d.article_count));

    // Dynamically load Chart.js if not already loaded
    const render = (Chart: any) => {
      this.trendChartInstance = new Chart(canvas, {
        type: 'line',
        data: {
          labels,
          datasets: [
            {
              label: 'Avg Sentiment',
              data: sentimentValues,
              borderColor: '#58a6ff',
              backgroundColor: 'rgba(88,166,255,0.12)',
              borderWidth: 2,
              pointRadius: 3,
              fill: true,
              tension: 0.3,
              yAxisID: 'ySentiment',
            },
            {
              label: 'Số bài viết',
              data: countValues,
              borderColor: '#3fb950',
              backgroundColor: 'rgba(63,185,80,0.06)',
              borderWidth: 1.5,
              pointRadius: 2,
              fill: false,
              tension: 0.3,
              yAxisID: 'yCount',
            }
          ]
        },
        options: {
          responsive: true,
          animation: { duration: 600 },
          plugins: {
            legend: { labels: { color: '#8b949e', font: { size: 11 } } },
            tooltip: {
              callbacks: {
                label: (ctx: any) => {
                  if (ctx.datasetIndex === 0) return ` Sentiment: ${ctx.raw.toFixed(3)}`;
                  return ` Bài viết: ${ctx.raw}`;
                }
              }
            }
          },
          scales: {
            x: { ticks: { color: '#6e7681', font: { size: 10 } }, grid: { color: '#21262d' } },
            ySentiment: {
              position: 'left', ticks: { color: '#58a6ff', font: { size: 10 } },
              grid: { color: '#21262d' },
              min: -1, max: 1,
            },
            yCount: {
              position: 'right', ticks: { color: '#3fb950', font: { size: 10 } },
              grid: { drawOnChartArea: false },
            }
          }
        }
      });
    };

    // Use dynamically imported Chart.js (must be in package.json)
    import('chart.js/auto').then(m => render(m.default || m)).catch(() => {
      console.warn('chart.js not available, install with: npm install chart.js');
    });
  }

  loadMiniChartsData() {
    const tickers = this.displayTickers;
    if (!tickers.length) return;

    this.signalService.getHistoricalDataBatch(tickers).subscribe(batch => {
      this.batchData = batch;
    });
  }

  navigateToSignals(ticker: string) {
    this.router.navigate(['/signals'], { queryParams: { q: ticker } });
  }

  sentimentClass(score: number): string {
    if (score >= 0.5) return 'bullish';
    if (score <= -0.5) return 'bearish';
    return 'neutral';
  }

  signalEmoji(signal: string): string {
    const s = signal?.toUpperCase() ?? '';
    if (s === 'STRONG BUY') return '🟢🟢';
    if (s === 'BUY') return '🟢';
    if (s === 'SELL') return '🔴';
    if (s === 'STRONG SELL') return '🔴🔴';
    return '⏸️';
  }
}
