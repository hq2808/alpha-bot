import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Subscription, interval } from 'rxjs';
import { SignalService, MarketData } from '../../services/signal.service';
import { MiniChartComponent } from '../../components/mini-chart/mini-chart.component';

/**
 * Interface for VNDirect price board data (simplified)
 */
export interface VndPriceData {
  code: string;           // Mã CP
  basicPrice: number;     // Giá tham chiếu (TC)
  ceilingPrice: number;   // Trần
  floorPrice: number;     // Sàn

  matchPrice: number;     // Khớp lệnh - Giá
  matchQtty: number;      // Khớp lệnh - KL

  buyPrice1: number; buyQtty1: number;
  buyPrice2: number; buyQtty2: number;
  buyPrice3: number; buyQtty3: number;

  sellPrice1: number; sellQtty1: number;
  sellPrice2: number; sellQtty2: number;
  sellPrice3: number; sellQtty3: number;

  totalMatchQtty: number; // Tổng KL match
}

@Component({
  selector: 'app-price-board',
  standalone: true,
  imports: [CommonModule, HttpClientModule, MiniChartComponent],
  template: `
    <div class="price-board-page">
      <div class="page-header">
        <div>
          <h1>📈 Bảng Giá Chứng Khoán</h1>
          <p class="subtitle">Dữ liệu thị trường (Real-time từ VNDirect) · Tự động cập nhật mỗi 5s</p>
        </div>
      </div>
      
      <div class="board-container">
        <table class="board-table">
          <thead>
            <tr>
              <th rowspan="2" class="col-ticker">Mã CK</th>
              <th rowspan="2" class="col-ref">TC</th>
              <th rowspan="2" class="col-ceil">Trần</th>
              <th rowspan="2" class="col-floor">Sàn</th>
              
              <th colspan="6" class="group-buy">BÊN MUA</th>
              <th colspan="3" class="group-match">KHỚP LỆNH</th>
              <th colspan="6" class="group-sell">BÊN BÁN</th>
              
              <th rowspan="2" class="col-total">Tổng KL</th>
            </tr>
            <tr>
              <!-- Mua -->
              <th class="col-price">Giá 3</th><th class="col-vol">KL 3</th>
              <th class="col-price">Giá 2</th><th class="col-vol">KL 2</th>
              <th class="col-price">Giá 1</th><th class="col-vol">KL 1</th>
              
              <!-- Khớp -->
              <th class="col-price">Giá</th><th class="col-vol">KL</th><th class="col-change">+/-</th>
              
              <!-- Bán -->
              <th class="col-price">Giá 1</th><th class="col-vol">KL 1</th>
              <th class="col-price">Giá 2</th><th class="col-vol">KL 2</th>
              <th class="col-price">Giá 3</th><th class="col-vol">KL 3</th>
            </tr>
          </thead>
          <tbody>
            @for (stock of displayData; track stock.code) {
              <tr class="stock-row" (click)="openDetail(stock.code)">
                <td class="col-ticker" [class]="getPriceColor(stock.matchPrice, stock)">{{ stock.code }}</td>
                <td class="col-ref c-ref">{{ stock.basicPrice | number:'1.2-2' }}</td>
                <td class="col-ceil c-ceil">{{ stock.ceilingPrice | number:'1.2-2' }}</td>
                <td class="col-floor c-floor">{{ stock.floorPrice | number:'1.2-2' }}</td>
                
                <!-- Bên Mua -->
                <td class="col-price" [class]="getPriceColor(stock.buyPrice3, stock)">{{ formatPrice(stock.buyPrice3) }}</td>
                <td class="col-vol" [class]="getPriceColor(stock.buyPrice3, stock)">{{ formatVol(stock.buyQtty3) }}</td>
                <td class="col-price" [class]="getPriceColor(stock.buyPrice2, stock)">{{ formatPrice(stock.buyPrice2) }}</td>
                <td class="col-vol" [class]="getPriceColor(stock.buyPrice2, stock)">{{ formatVol(stock.buyQtty2) }}</td>
                <td class="col-price" [class]="getPriceColor(stock.buyPrice1, stock)">{{ formatPrice(stock.buyPrice1) }}</td>
                <td class="col-vol" [class]="getPriceColor(stock.buyPrice1, stock)">{{ formatVol(stock.buyQtty1) }}</td>
                
                <!-- Khớp Lệnh -->
                <td class="col-price highlight-match" [class]="getPriceColor(stock.matchPrice, stock)">{{ formatPrice(stock.matchPrice) }}</td>
                <td class="col-vol highlight-match" [class]="getPriceColor(stock.matchPrice, stock)">{{ formatVol(stock.matchQtty) }}</td>
                <td class="col-change highlight-match" [class]="getPriceColor(stock.matchPrice, stock)">
                  {{ getPriceChange(stock) }}
                </td>
                
                <!-- Bên Bán -->
                <td class="col-price" [class]="getPriceColor(stock.sellPrice1, stock)">{{ formatPrice(stock.sellPrice1) }}</td>
                <td class="col-vol" [class]="getPriceColor(stock.sellPrice1, stock)">{{ formatVol(stock.sellQtty1) }}</td>
                <td class="col-price" [class]="getPriceColor(stock.sellPrice2, stock)">{{ formatPrice(stock.sellPrice2) }}</td>
                <td class="col-vol" [class]="getPriceColor(stock.sellPrice2, stock)">{{ formatVol(stock.sellQtty2) }}</td>
                <td class="col-price" [class]="getPriceColor(stock.sellPrice3, stock)">{{ formatPrice(stock.sellPrice3) }}</td>
                <td class="col-vol" [class]="getPriceColor(stock.sellPrice3, stock)">{{ formatVol(stock.sellQtty3) }}</td>
                
                <!-- Tổng KL -->
                <td class="col-total">{{ formatVol(stock.totalMatchQtty) }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="23" style="text-align: center; padding: 30px; color: #8b949e;">Đang tải dữ liệu bảng giá...</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      
      <!-- Overlay Detail Chart -->
      @if (selectedTicker) {
        <div class="overlay" (click)="closeDetail()">
          <div class="detail-modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2>Chi tiết mã {{ selectedTicker }}</h2>
              <button class="close-btn" (click)="closeDetail()">✕</button>
            </div>
            <div class="modal-body">
              @if (loadingChart) {
                <p>Đang tải dữ liệu biểu đồ nhiều năm...</p>
              } @else {
                <div class="chart-wrapper">
                  <app-mini-chart 
                    [ticker]="selectedTicker" 
                    [data]="selectedTickerData"
                    [showClose]="false"
                    [draggable]="false">
                  </app-mini-chart>
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .price-board-page {
      padding: 0 20px 20px 20px;
      animation: fadeIn 0.4s ease-out;
      height: calc(100vh - 80px); /* Fill remaining height */
      display: flex;
      flex-direction: column;
    }
    .page-header {
      margin-bottom: 16px;
      border-bottom: 2px solid #30363d;
      padding-bottom: 8px;
    }
    h1 { margin: 0; font-size: 1.5rem; color: #58a6ff; font-weight: 800; }
    .subtitle { margin: 4px 0 0; color: #8b949e; font-size: 0.85rem; }
    
    .board-container {
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 8px;
      overflow: auto;
      flex: 1;
    }
    
    .board-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.82rem;
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      white-space: nowrap;
    }
    
    .board-table thead {
      position: sticky;
      top: 0;
      z-index: 10;
      background: #161b22;
    }
    
    .board-table th, .board-table td {
      border: 1px solid #30363d;
      padding: 6px 4px;
      text-align: right;
    }
    
    .board-table th {
      color: #8b949e;
      font-weight: 600;
      font-size: 0.75rem;
      text-align: center;
    }
    
    .col-ticker { text-align: left !important; font-weight: 700; position: sticky; left: 0; background: #0d1117; z-index: 5; }
    .board-table thead .col-ticker { background: #161b22; z-index: 15; }
    .stock-row:hover .col-ticker { background: #1f242c; }
    
    .group-buy { background: rgba(35, 134, 54, 0.1); }
    .group-sell { background: rgba(248, 81, 73, 0.1); }
    .group-match { background: rgba(88, 166, 255, 0.1); }
    .highlight-match { background: rgba(88, 166, 255, 0.05); font-weight: 500;}
    
    .stock-row {
      cursor: pointer;
      transition: background 0.1s;
    }
    .stock-row:hover {
      background: #1f242c;
    }
    
    /* COLORS */
    .c-ceil { color: #d73aee !important; } /* Tím */
    .c-floor { color: #00e0ff !important; } /* Xanh lơ */
    .c-up { color: #3fb950 !important; } /* Xanh lá */
    .c-down { color: #f85149 !important; } /* Đỏ */
    .c-ref { color: #d2a8ff !important; } /* Vàng TC */
    
    /* Overlay Modal */
    .overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center;
      z-index: 1000; animation: fadeIn 0.2s;
    }
    .detail-modal {
      background: #0d1117; border: 1px solid #30363d; border-radius: 12px;
      width: 90%; max-width: 800px; max-height: 90vh; overflow-y: auto;
      box-shadow: 0 10px 30px rgba(0,0,0,0.5);
    }
    .modal-header {
      padding: 16px 20px; border-bottom: 1px solid #30363d;
      display: flex; justify-content: space-between; align-items: center;
    }
    .modal-header h2 { margin: 0; font-size: 1.2rem; color: #58a6ff; }
    .close-btn {
      background: none; border: none; color: #8b949e; font-size: 1.2rem;
      cursor: pointer; padding: 4px; transition: color 0.15s;
    }
    .close-btn:hover { color: #f85149; }
    .modal-body { padding: 20px; }
    .chart-wrapper { height: 400px; }
  `]
})
export class PriceBoardComponent implements OnInit, OnDestroy {
  // Using a predefined list of popular VN30 and large cap stocks for the board
  private trackList = 'SSI,VND,HCM,VCI,HPG,HSG,NKG,VHM,VIC,VRE,NVL,DIG,DXG,TCB,MBB,VPB,STB,CTG,VCB,BID,FPT,MWG,PNJ,GAS,PLX,POW,VNM,MSN,SAB,VJC,HVN,GVR,DGC,DPM,DCM,KBC,IDC,VGC';

  displayData: VndPriceData[] = [];
  selectedTicker: string | null = null;
  selectedTickerData: MarketData[] = [];
  loadingChart = false;

  private sub?: Subscription;
  private chartSub?: Subscription;

  constructor(private http: HttpClient, private signalService: SignalService) { }

  ngOnInit() {
    this.fetchData();
    // Auto refresh every 5 seconds
    this.sub = interval(5000).subscribe(() => this.fetchData());
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
    this.chartSub?.unsubscribe();
  }

  fetchData() {
    // Calling our Spring Boot proxy endpoint to bypass CORS and load cached DB data
    const url = `/api/market-data/vndirect/quotes`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        if (res && res.data) {
          this.displayData = res.data.map((d: any) => ({
            code: d.ticker || d.code,
            basicPrice: d.basicPrice,
            ceilingPrice: d.ceilingPrice,
            floorPrice: d.floorPrice,

            matchPrice: d.matchPrice,
            matchQtty: d.matchQtty,

            buyPrice1: d.buyPrice1, buyQtty1: d.buyQtty1,
            buyPrice2: d.buyPrice2, buyQtty2: d.buyQtty2,
            buyPrice3: d.buyPrice3, buyQtty3: d.buyQtty3,

            sellPrice1: d.sellPrice1, sellQtty1: d.sellQtty1,
            sellPrice2: d.sellPrice2, sellQtty2: d.sellQtty2,
            sellPrice3: d.sellPrice3, sellQtty3: d.sellQtty3,

            totalMatchQtty: d.totalMatchQtty || 0
          }));

          // Sort alphabetically by default
          this.displayData.sort((a, b) => a.code.localeCompare(b.code));
        }
      },
      error: (err) => {
        console.error('Error fetching price board data', err);
      }
    });
  }

  getPriceColor(price: number, stock: VndPriceData): string {
    if (!price || price === 0) return ''; // Empty/unmatched
    if (price >= stock.ceilingPrice) return 'c-ceil';
    if (price <= stock.floorPrice) return 'c-floor';
    if (price > stock.basicPrice) return 'c-up';
    if (price < stock.basicPrice) return 'c-down';
    return 'c-ref'; // Equal to TC
  }

  getPriceChange(stock: VndPriceData): string {
    if (!stock.matchPrice || stock.matchPrice === 0) return '';
    const diff = stock.matchPrice - stock.basicPrice;
    if (diff === 0) return '0.00';
    const sign = diff > 0 ? '+' : '';
    return `${sign}${diff.toFixed(2)}`;
  }

  formatPrice(p: number): string {
    if (!p || p === 0) return '-';
    return p.toFixed(2);
  }

  formatVol(v: number): string {
    if (!v || v === 0) return '-';
    // Format volume to K (e.g. 15400 -> 154.0) usually VN boards divide by 10 for prices, 10 for volumes
    // We just use standard locale formatting for real volume
    return (v * 10).toLocaleString('en-US'); // VNDirect typically sends Vol/10
  }

  openDetail(ticker: string) {
    this.selectedTicker = ticker;
    this.loadingChart = true;

    // Fetch historical data for the chart using the existing SignalService methods
    this.chartSub?.unsubscribe();
    this.chartSub = this.signalService.getHistoricalDataBatch([ticker]).subscribe({
      next: (batch) => {
        this.selectedTickerData = batch[ticker] || [];
        this.loadingChart = false;
      },
      error: (err) => {
        console.error('Failed to load chart data', err);
        this.loadingChart = false;
      }
    });
  }

  closeDetail() {
    this.selectedTicker = null;
    this.selectedTickerData = [];
    this.chartSub?.unsubscribe();
  }
}
