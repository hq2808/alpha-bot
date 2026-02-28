import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SignalService, TickerSignal } from '../../services/signal.service';
import { interval, Subscription } from 'rxjs';

@Component({
    selector: 'app-intelligence',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="intelligence-page">
      <div class="header">
        <div class="header-main">
          <h1>🗞️ Market Intelligence Terminal</h1>
          <p>Aggregated sentiment analysis from 150+ financial news sources</p>
        </div>
        <div class="last-update">
          Last update: {{ lastUpdate | date:'HH:mm:ss' }}
        </div>
      </div>

      <!-- Sentiment Overview Cards -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">🔥 Trending Today</div>
          <div class="stat-value">{{ trendingTicker }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">🐂 Market Sentiment</div>
          <div class="stat-value" [style.color]="overallColor">{{ overallSentiment }}%</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">📡 Active Signals</div>
          <div class="stat-value">{{ signals.length }}</div>
        </div>
      </div>

      <div class="dashboard-main">
        <!-- Visual Heatmap / Grid -->
        <div class="heatmap-section">
          <h2>🎯 Live Signal Heatmap</h2>
          <div class="signals-grid">
            @for (sig of signals; track sig.ticker) {
              <div class="signal-box" [class]="sig.signal.toLowerCase().replace(' ', '-')">
                <div class="ticker">{{ sig.ticker }}</div>
                <div class="badge">{{ sig.signal }}</div>
                <div class="sentiment-bar">
                  <div class="fill" [style.width.%]="(sig.averageSentiment + 1) * 50"></div>
                </div>
                <div class="meta">
                  <span>{{ sig.mentionCount }} stories</span>
                  <span>{{ sig.averageSentiment | number:'1.2-2' }} score</span>
                </div>
                <div class="last-title">{{ sig.lastNewsTitle }}</div>
              </div>
            } @empty {
               <div class="empty">Calculating market signals...</div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .intelligence-page { display: flex; flex-direction: column; gap: 24px; animation: fadeIn 0.4s ease-out; }
    
    .header { display: flex; justify-content: space-between; align-items: end; border-bottom: 2px solid #30363d; padding-bottom: 16px; }
    h1 { margin: 0; font-size: 1.8rem; color: #58a6ff; font-weight: 800; letter-spacing: -0.5px; }
    p { margin: 4px 0 0; color: #8b949e; font-size: 0.95rem; }
    .last-update { font-size: 0.8rem; color: #6e7681; font-family: 'JetBrains Mono', monospace; }

    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
    .stat-card { background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 16px; }
    .stat-label { font-size: 0.8rem; color: #8b949e; margin-bottom: 8px; font-weight: 600; text-transform: uppercase; }
    .stat-value { font-size: 1.5rem; font-weight: 800; color: #e6edf3; }

    .heatmap-section h2 { font-size: 1.1rem; margin-bottom: 16px; color: #e6edf3; }
    
    .signals-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }

    .signal-box {
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 16px;
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      gap: 12px;
      transition: all 0.2s;
    }
    .signal-box:hover { border-color: #8b949e; transform: translateY(-2px); }

    .ticker { font-size: 1.4rem; font-weight: 900; color: #58a6ff; }
    .badge { position: absolute; top: 16px; right: 16px; font-size: 0.7rem; font-weight: 800; padding: 4px 8px; border-radius: 4px; border: 1px solid transparent; }
    
    /* Signal Colors */
    .strong-buy .badge { background: rgba(35,134,54,0.1); color: #3fb950; border-color: #238636; }
    .strong-buy { border-top: 4px solid #238636; }
    .buy .badge { background: rgba(35,134,54,0.1); color: #3fb950; border-color: #238636; }
    .buy { border-top: 4px solid #2ea043; }
    
    .sell .badge { background: rgba(218,54,51,0.1); color: #f85149; border-color: #da3633; }
    .sell { border-top: 4px solid #da3633; }
    .strong-sell .badge { background: rgba(218,54,51,0.1); color: #f85149; border-color: #da3633; }
    .strong-sell { border-top: 4px solid #f85149; }
    
    .neutral .badge { background: #21262d; color: #8b949e; border-color: #30363d; }
    .neutral { border-top: 4px solid #30363d; }

    .sentiment-bar { height: 6px; background: #21262d; border-radius: 3px; overflow: hidden; }
    .sentiment-bar .fill { height: 100%; background: #58a6ff; transition: width 1s ease-in-out; }
    .strong-buy .fill, .buy .fill { background: #3fb950; }
    .strong-sell .fill, .sell .fill { background: #f85149; }

    .meta { display: flex; justify-content: space-between; font-size: 0.8rem; color: #8b949e; }
    .last-title { font-size: 0.75rem; color: #6e7681; font-style: italic; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding-top: 8px; border-top: 1px solid #21262d; }

    .empty { grid-column: 1/-1; padding: 40px; text-align: center; color: #8b949e; background: #161b22; border: 1px dashed #30363d; border-radius: 8px; }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class IntelligenceComponent implements OnInit, OnDestroy {
    signals: TickerSignal[] = [];
    lastUpdate = new Date();
    trendingTicker = '—';
    overallSentiment = 0;
    overallColor = '#8b949e';
    private sub?: Subscription;

    constructor(private signalService: SignalService) { }

    ngOnInit() {
        this.refresh();
        // Auto refresh every 30 seconds
        this.sub = interval(30000).subscribe(() => this.refresh());
    }

    ngOnDestroy() {
        this.sub?.unsubscribe();
    }

    refresh() {
        this.signalService.getMarketSignals().subscribe(sigs => {
            this.signals = sigs;
            this.lastUpdate = new Date();
            if (sigs.length > 0) {
                this.trendingTicker = sigs[0].ticker;
                const avg = sigs.reduce((a, b) => a + b.averageSentiment, 0) / sigs.length;
                this.overallSentiment = Math.round(avg * 100);
                this.overallColor = avg >= 0.3 ? '#3fb950' : avg <= -0.3 ? '#f85149' : '#8b949e';
            }
        });
    }
}
