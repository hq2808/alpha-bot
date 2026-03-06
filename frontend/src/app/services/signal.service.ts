import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map } from 'rxjs';
import { tap } from 'rxjs/operators';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

// ── Domain Models ─────────────────────────────────────────────────────────────

export interface NewsArticle {
    id: number;
    title: string;
    description: string;
    url: string;
    source: string;
    publishedAt: string;
    crawledAt: string;
    sentimentScore: number;
    mentionedTickers: string;
    aiSummary: string;
    tags: string;
    alertSent: boolean;
}

export interface TickerSignal {
    ticker: string;
    averageSentiment: number;
    mentionCount: number;
    signal: string;
    lastNewsTitle: string;
}

export interface MarketData {
    id?: number;
    ticker: string;
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
}

export interface WatchlistItem {
    id: number;
    ticker: string;
    createdAt: string;
}

export interface NewsPage {
    content: NewsArticle[];
    totalPages: number;
    totalElements: number;
    number: number;   // current page (0-based)
    size: number;
}

export interface VnStock {
    ticker: string;
    companyName: string;
    sector: string;
    exchange: string;
}

// ── Constants ─────────────────────────────────────────────────────────────────

export const SENTIMENT_THRESHOLDS = {
    BULLISH: 0.5,
    BEARISH: -0.5,
    STRONG_BULLISH: 0.7,
};

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class SignalService {
    private readonly stomp = new RxStomp();
    private historicalDataCache: { [ticker: string]: MarketData[] } = {};
    private cacheTimestamp: number = 0;
    private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private authService = inject(AuthService);

    constructor(private http: HttpClient) {
        this.stomp.configure({
            brokerURL: `ws://${window.location.hostname}:8080/ws`,
        });
        this.stomp.activate();
    }

    /** Latest news articles from REST API. */
    getLatestNews(filterByWatchlist = false): Observable<NewsArticle[]> {
        return this.http.get<NewsArticle[]>(`/api/news/latest?filterByWatchlist=${filterByWatchlist}`);
    }

    /** Full VN stock catalog from DB. */
    getStocks(): Observable<VnStock[]> {
        return this.http.get<VnStock[]>('/api/stocks');
    }

    /**
     * Real-time news stream via WebSocket.
     * Properly parses the STOMP message body to NewsArticle.
     */
    getLiveNews(): Observable<NewsArticle> {
        return this.stomp.watch('/topic/news').pipe(
            map(msg => JSON.parse(msg.body) as NewsArticle)
        );
    }

    /** Bullish articles above a given sentiment threshold. */
    getBullishNews(threshold = SENTIMENT_THRESHOLDS.BULLISH, filterByWatchlist = false): Observable<NewsArticle[]> {
        return this.http.get<NewsArticle[]>(`/api/news/bullish?threshold=${threshold}&filterByWatchlist=${filterByWatchlist}`);
    }

    /** Paginated keyword search — used by News Insight page. */
    searchNews(q: string = '', page: number = 0, size: number = 20, filterType: string = 'all', exactTicker: string = '', hours?: number): Observable<NewsPage> {
        let params = `q=${encodeURIComponent(q)}&page=${page}&size=${size}&filterType=${filterType}`;
        if (exactTicker) params += `&ticker=${encodeURIComponent(exactTicker)}`;
        if (hours) params += `&hours=${hours}`;
        return this.http.get<NewsPage>(`/api/news/search?${params}`);
    }

    /** Daily sentiment trend for last 30 days — used by Intelligence page. */
    getSentimentTrend(): Observable<{ date: string; avg_sentiment: number; article_count: number }[]> {
        return this.http.get<any[]>('/api/news/sentiment-trend');
    }

    /** Aggregated market signals for trending tickers. */
    getMarketSignals(): Observable<TickerSignal[]> {
        return this.http.get<TickerSignal[]>('/api/market/signals');
    }


    /** Historical OHLCV data for a single ticker. */
    getHistoricalData(ticker: string): Observable<MarketData[]> {
        return this.http.get<MarketData[]>(`/api/market-data/${ticker}`);
    }

    /**
     * Historical OHLCV data for multiple tickers (Batch API).
     * Implements in-memory TTL caching (5 min).
     */
    getHistoricalDataBatch(tickers: string[]): Observable<{ [ticker: string]: MarketData[] }> {
        if (!tickers || tickers.length === 0) return of({});

        const now = Date.now();
        const cacheValid = now - this.cacheTimestamp < this.CACHE_TTL_MS;
        const allCached = tickers.every(t => this.historicalDataCache[t]);

        if (cacheValid && allCached) {
            const result: { [ticker: string]: MarketData[] } = {};
            tickers.forEach(t => result[t] = this.historicalDataCache[t]);
            return of(result);
        }

        return this.http.post<{ [ticker: string]: MarketData[] }>('/api/market-data/batch', tickers).pipe(
            tap(data => {
                this.historicalDataCache = { ...this.historicalDataCache, ...data };
                this.cacheTimestamp = Date.now();
            })
        );
    }

    // ── Watchlist ──────────────────────────────────────────────────────────────

    getWatchlist(): Observable<WatchlistItem[]> {
        if (!this.authService.isLoggedIn()) {
            return of([]);
        }
        return this.http.get<WatchlistItem[]>('/api/watchlist');
    }

    addToWatchlist(ticker: string): Observable<WatchlistItem> {
        if (!this.authService.isLoggedIn()) {
            return of({ id: 0, ticker, createdAt: new Date().toISOString() } as WatchlistItem);
        }
        return this.http.post<WatchlistItem>(`/api/watchlist/${ticker}`, {});
    }

    removeFromWatchlist(ticker: string): Observable<void> {
        if (!this.authService.isLoggedIn()) {
            return of(undefined);
        }
        return this.http.delete<void>(`/api/watchlist/${ticker}`);
    }

    // ── Chat ───────────────────────────────────────────────────────────────────

    chat(message: string): Observable<{ response: string }> {
        return this.http.post<{ response: string }>('/api/chat', { message });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** CSS class based on sentiment score. */
    getSentimentClass(score: number): string {
        if (score >= SENTIMENT_THRESHOLDS.STRONG_BULLISH) return 'bullish';
        if (score <= SENTIMENT_THRESHOLDS.BEARISH) return 'bearish';
        return 'neutral';
    }

    /** Formatted sentiment label with emoji. */
    formatSentiment(score: number): string {
        if (score >= SENTIMENT_THRESHOLDS.STRONG_BULLISH) return `📈 Bullish (+${score.toFixed(2)})`;
        if (score <= SENTIMENT_THRESHOLDS.BEARISH) return `📉 Bearish (${score.toFixed(2)})`;
        return `➡️ Neutral (${score.toFixed(2)})`;
    }

    /**
     * Shared price formatter for lightweight-charts.
     * Converts raw number (e.g. 92900) to "92,90" VN-style display.
     */
    formatPrice(price: number): string {
        if (!price) return '0';
        return price.toLocaleString('en-US');
    }
}
