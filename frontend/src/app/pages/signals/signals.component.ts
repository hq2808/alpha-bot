import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalService, NewsArticle, MarketData } from '../../services/signal.service';
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
            @if (article.tags) {
              <div class="article-tags">{{ article.tags }}</div>
            }
            @if (article.mentionedTickers) {
              <div class="tickers">
                @for (t of article.mentionedTickers.split(','); track t) {
                  <span class="chip" (click)="openChart(t.trim())">{{ t.trim() }}</span>
                }
              </div>
            }
            <span class="time">{{ article.crawledAt | date:'HH:mm dd/MM' }}</span>
          </div>
        } @empty {
          <div class="empty">
            <p>Không có tin tức nào trên ngưỡng {{ threshold | number:'1.2-2' }}</p>
            <small>Hệ thống đang tiếp tục theo dõi thị trường...</small>
          </div>
        }
      </div>

      <!-- Chart Modal -->
      @if (selectedTicker) {
        <div class="modal-overlay" (click)="closeChart()">
          <div class="modal-content" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2>Biểu đồ giá {{ selectedTicker }}</h2>
              <button class="close-btn" (click)="closeChart()">✕</button>
            </div>
            
            @if (isLoadingChart) {
              <div class="loading-state">
                <div class="spinner"></div>
                <p>Đang tải dữ liệu...</p>
              </div>
            }
            
            <div class="chart-container" [class.hidden]="isLoadingChart" #chartContainer></div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .signals-page { padding: 20px; }
    .page-header { margin-bottom: 24px; }
    h1 { color: #e6edf3; font-size: 1.4rem; margin-bottom: 4px; border-bottom: 1px solid #30363d; padding-bottom: 8px; }
    .subtitle { color: #8b949e; font-size: 0.9rem; margin-top: 0; margin-bottom: 16px; }

    /* News Grid styling */
    .filter-row { display: flex; align-items: center; gap: 12px; font-size: 0.9rem; color: #8b949e; margin-bottom: 8px; }
    input[type=range] { width: 160px; accent-color: #3fb950; cursor: pointer; }
    .threshold-val { color: #3fb950; font-weight: 700; }

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
export class SignalsComponent implements OnInit {
  articles: NewsArticle[] = [];
  threshold = 0.5;

  selectedTicker: string | null = null;
  chartData: MarketData[] = [];
  isLoadingChart = false;

  @ViewChild('chartContainer') chartContainer!: ElementRef;
  private chart: IChartApi | null = null;

  constructor(private signal: SignalService) { }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.signal.getBullishNews(this.threshold).subscribe(news => this.articles = news);
  }

  openChart(ticker: string) {
    this.selectedTicker = ticker;
    this.isLoadingChart = true;

    this.signal.getHistoricalData(ticker).subscribe({
      next: (data) => {
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

    const candlestickSeries = (this.chart as any).addCandlestickSeries({
      upColor: '#3fb950',
      downColor: '#f85149',
      borderVisible: false,
      wickUpColor: '#3fb950',
      wickDownColor: '#f85149',
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
      const volumeSeries = (this.chart as any).addHistogramSeries({
        color: '#26a69a',
        priceFormat: { type: 'volume' },
        priceScaleId: '', // blank sets it as overlay
      });
      volumeSeries.priceScale().applyOptions({
        scaleMargins: { top: 0.8, bottom: 0 },
      });
      volumeSeries.setData(volumeData);

      // Add SMA20 Line Series
      const smaSeries = (this.chart as any).addLineSeries({
        color: '#58a6ff',
        lineWidth: 2,
        title: 'SMA 20'
      });
      smaSeries.setData(smaData);
    }

    this.chart.timeScale().fitContent();
  }
}

