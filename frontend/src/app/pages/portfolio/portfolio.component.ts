import { Component, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import {
  PortfolioService,
  PortfolioSummary,
  PortfolioPosition,
  PortfolioTransaction,
  PortfolioSnapshot
} from '../../services/portfolio.service';
import { createChart, IChartApi, ISeriesApi } from 'lightweight-charts';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  providers: [PortfolioService],
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.css']
})
export class PortfolioComponent implements OnInit, AfterViewInit {
  @ViewChild('chartContainer') chartContainer!: ElementRef;

  summary: PortfolioSummary | null = null;
  positions: PortfolioPosition[] = [];
  transactions: PortfolioTransaction[] = [];
  snapshots: PortfolioSnapshot[] = [];

  private chart!: IChartApi;
  private areaSeries!: ISeriesApi<"Area">;

  constructor(private portfolioService: PortfolioService) { }

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {

  }

  loadData() {
    this.portfolioService.getSummary().subscribe(s => this.summary = s);
    this.portfolioService.getPositions().subscribe(p => this.positions = p);
    this.portfolioService.getTransactions().subscribe(t => this.transactions = t);

    this.portfolioService.getChartData().subscribe(data => {
      this.snapshots = data;
      this.initChart();
    });
  }

  initChart() {
    if (!this.chartContainer) return;

    // Default config styling matched dark theme of app
    this.chart = createChart(this.chartContainer.nativeElement, {
      width: this.chartContainer.nativeElement.clientWidth,
      height: 300,
      layout: {
        background: { color: '#161b22' },
        textColor: '#8b949e',
      },
      grid: {
        vertLines: { color: '#30363d' },
        horzLines: { color: '#30363d' },
      },
      timeScale: {
        borderColor: '#30363d',
        timeVisible: true,
      },
      rightPriceScale: {
        borderColor: '#30363d',
      }
    });

    this.areaSeries = this.chart.addAreaSeries({
      topColor: 'rgba(88, 166, 255, 0.4)',
      bottomColor: 'rgba(88, 166, 255, 0.05)',
      lineColor: '#58a6ff',
      lineWidth: 2,
    });

    if (this.snapshots && this.snapshots.length > 0) {
      // Map format string 'YYYY-MM-DD' => Timestamp
      const chartData = this.snapshots.map(s => {
        return {
          time: s.snapshotDate, // Lightweight charts supports '2019-04-11' format directly
          value: s.totalEquity
        };
      });

      this.areaSeries.setData(chartData);
      this.chart.timeScale().fitContent();
    }
  }
}
