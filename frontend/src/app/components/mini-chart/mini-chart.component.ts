import { Component, Input, Output, EventEmitter, OnChanges, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { createChart, IChartApi } from 'lightweight-charts';
import { MarketData, SignalService } from '../../services/signal.service';

@Component({
    selector: 'app-mini-chart',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="mini-chart-card">
      <div class="mini-chart-header">
        <span class="mini-chart-drag-handle" *ngIf="draggable">⋮⋮</span>
        <div class="mini-chart-title">{{ ticker }}</div>
        <button class="mini-chart-close" *ngIf="showClose" (click)="remove.emit()" title="Xóa khỏi Watchlist">✕</button>
      </div>
      <div class="mini-chart-container" #chartContainer></div>
    </div>
  `,
    styles: [`
    .mini-chart-card {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      transition: border-color 0.2s;
      box-sizing: border-box;
      background-clip: padding-box;
    }
    .mini-chart-card:hover {
      border-color: #58a6ff;
    }
    .mini-chart-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .mini-chart-drag-handle {
      color: #6e7681;
      cursor: grab;
      font-weight: bold;
      letter-spacing: -2px;
      user-select: none;
    }
    .mini-chart-drag-handle:active {
      cursor: grabbing;
    }
    .mini-chart-title {
      font-size: 0.9rem;
      font-weight: 700;
      color: #58a6ff;
      flex: 1;
      margin: 0;
    }
    .mini-chart-close {
      background: none;
      border: none;
      color: #8b949e;
      cursor: pointer;
      font-size: 0.9rem;
      padding: 0 4px;
      line-height: 1;
      transition: color 0.15s;
    }
    .mini-chart-close:hover {
      color: #f85149;
    }
    .mini-chart-container {
      width: 100%;
      height: 200px;
    }
  `]
})
export class MiniChartComponent implements OnChanges, AfterViewInit, OnDestroy {
    @Input() ticker!: string;
    @Input() data: MarketData[] = [];
    @Input() showClose = false;
    @Input() draggable = false;

    @Output() remove = new EventEmitter<void>();

    @ViewChild('chartContainer') container!: ElementRef<HTMLDivElement>;

    private chart?: IChartApi;
    private resizeObserver?: ResizeObserver;
    private series: any;

    constructor(private signalService: SignalService) { }

    ngAfterViewInit() {
        this.initChart();
    }

    ngOnChanges() {
        if (this.chart && this.data && this.data.length > 0) {
            this.updateData();
        }
    }

    ngOnDestroy() {
        this.resizeObserver?.disconnect();
        this.chart?.remove();
    }

    private initChart() {
        const el = this.container.nativeElement;

        this.chart = createChart(el, {
            width: el.clientWidth,
            height: 200,
            layout: { background: { type: 'solid' as any, color: '#161b22' }, textColor: '#8b949e' },
            grid: { vertLines: { color: '#21262d' }, horzLines: { color: '#21262d' } },
            timeScale: { visible: true, borderColor: '#30363d' },
            rightPriceScale: { visible: true, borderColor: '#30363d' },
            handleScroll: true,
            handleScale: true,
            crosshair: { mode: 1 } // Magnet mode
        } as any);

        this.resizeObserver = new ResizeObserver(entries => {
            if (entries.length === 0 || entries[0].target !== el) return;
            const newRect = entries[0].contentRect;
            this.chart?.applyOptions({ width: newRect.width, height: newRect.height });
        });
        this.resizeObserver.observe(el);

        this.series = this.chart.addCandlestickSeries({
            upColor: '#3fb950', downColor: '#f85149', borderVisible: false, wickUpColor: '#3fb950', wickDownColor: '#f85149',
            priceFormat: { type: 'custom', minMove: 0.01, formatter: (p: number) => this.signalService.formatPrice(p) }
        });

        if (this.data && this.data.length > 0) {
            this.updateData();
        }
    }

    private updateData() {
        if (!this.series || !this.data) return;

        // Sort array in ascending order (oldest to newest) to avoid lightweight-charts error
        const sortedData = [...this.data].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

        const formatted = sortedData.map(d => ({
            time: d.date.split('T')[0],
            open: d.open,
            high: d.high,
            low: d.low,
            close: d.close
        })) as any[];

        // Filter out duplicate timestamps which crash lightweight-charts
        const uniqueFormatted = formatted.filter((v, i, a) => a.findIndex(t => (t.time === v.time)) === i);

        this.series.setData(uniqueFormatted);
        this.chart?.timeScale().fitContent();
    }
}
