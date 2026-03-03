import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortfolioService, PortfolioSummary, PortfolioPosition, PortfolioTransaction } from '../../services/portfolio.service';
import { SignalService, VnStock } from '../../services/signal.service';

@Component({
    selector: 'app-test-trading',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './test-trading.component.ts.html',
    styleUrls: ['./test-trading.component.css']
})
export class TestTradingComponent implements OnInit {
    summary: PortfolioSummary | null = null;
    positions: PortfolioPosition[] = [];
    transactions: PortfolioTransaction[] = [];
    stocks: VnStock[] = [];

    // Form fields
    ticker: string = '';
    quantity: number = 100;
    action: 'BUY' | 'SELL' = 'BUY';
    reason: string = 'Manual trading test';

    loading = false;
    message = '';
    isError = false;

    constructor(
        private portfolioService: PortfolioService,
        private signalService: SignalService
    ) { }

    ngOnInit(): void {
        this.loadData();
        this.signalService.getStocks().subscribe(s => this.stocks = s);
    }

    loadData() {
        this.portfolioService.getManualSummary().subscribe(s => this.summary = s);
        this.portfolioService.getManualPositions().subscribe(p => this.positions = p);
        this.portfolioService.getManualTransactions().subscribe(t => this.transactions = t);
    }

    executeTrade() {
        if (!this.ticker || this.quantity <= 0) {
            this.showMessage('Vui lòng nhập mã chứng khoán và khối lượng > 0', true);
            return;
        }

        if (this.action === 'SELL' && !this.hasPosition(this.ticker)) {
            this.showMessage('Bạn không có mã này trong danh mục để bán', true);
            return;
        }

        this.loading = true;
        this.portfolioService.executeManualTrade(this.ticker, this.quantity, this.action, this.reason)
            .subscribe({
                next: () => {
                    this.showMessage(`Đã thực hiện lệnh ${this.action === 'BUY' ? 'MUA' : 'BÁN'} thành công`);
                    this.loadData();
                    this.loading = false;
                },
                error: (err) => {
                    let errorMsg = 'Không thể thực hiện giao dịch';
                    if (err.error && typeof err.error === 'object' && err.error.error) {
                        errorMsg = this.translateError(err.error.error);
                    } else if (typeof err.error === 'string') {
                        errorMsg = this.translateError(err.error);
                    }
                    this.showMessage('Lỗi: ' + errorMsg, true);
                    this.loading = false;
                }
            });
    }

    translateError(error: string): string {
        const errorMap: { [key: string]: string } = {
            'Insufficient cash balance': 'Không đủ số dư tiền mặt để thực hiện lệnh mua',
            'Insufficient quantity to sell': 'Không đủ khối lượng cổ phiếu để bán',
            'No position found for ticker': 'Không tìm thấy vị thế cho mã này',
            'Cannot execute trade: No valid market price': 'Không có giá thị trường hợp lệ để giao dịch',
            'Manual portfolio not found': 'Không tìm thấy danh mục giao dịch thử'
        };

        for (const key in errorMap) {
            if (error.includes(key)) return errorMap[key];
        }
        return error;
    }

    resetPortfolio() {
        if (confirm('Bạn có chắc chắn muốn reset danh mục? Toàn bộ danh mục và lịch sử giao dịch sẽ bị xóa.')) {
            this.portfolioService.resetManualPortfolio().subscribe(() => {
                this.showMessage('Đã reset danh mục về trạng thái mặc định (100M VND)');
                this.loadData();
            });
        }
    }

    hasPosition(ticker: string): boolean {
        return this.positions.some(p => p.ticker.toUpperCase() === ticker.toUpperCase());
    }

    getPositionQuantity(ticker: string): number {
        const pos = this.positions.find(p => p.ticker.toUpperCase() === ticker.toUpperCase());
        return pos ? pos.quantity : 0;
    }

    showMessage(msg: string, isError = false) {
        this.message = msg;
        this.isError = isError;
        setTimeout(() => this.message = '', 5000);
    }

    selectTicker(t: string) {
        this.ticker = t;
    }
}
