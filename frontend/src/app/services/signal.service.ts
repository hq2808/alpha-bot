import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay, of } from 'rxjs';
import { tap } from 'rxjs/operators';

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

@Injectable({ providedIn: 'root' })
export class SignalService {
    private readonly stomp = new RxStomp();
    private readonly latestNews$ = this.http.get<NewsArticle[]>('/api/news/latest').pipe(shareReplay(1));
    private historicalDataCache: { [ticker: string]: MarketData[] } = {};
    private cacheTimestamp: number = 0;
    private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    constructor(private http: HttpClient) {
        // Connect WebSocket to Spring Boot backend
        this.stomp.configure({
            brokerURL: `ws://${window.location.hostname}:8080/ws/websocket`,
        });
        this.stomp.activate();
    }

    /**
     * Observable of the latest news articles from REST API (initial load).
     */
    getLatestNews(filterByWatchlist = false): Observable<NewsArticle[]> {
        return this.http.get<NewsArticle[]>(`/api/news/latest?filterByWatchlist=${filterByWatchlist}`);
    }

    /**
     * Observable stream of real-time news articles via WebSocket.
     * Subscribe to receive live updates as the crawler discovers new articles.
     */
    getLiveNews(): Observable<NewsArticle> {
        return this.stomp.watch('/topic/news').pipe(
            // Map the STOMP message body (JSON string) to NewsArticle object
            // Using shareReplay to prevent multiple WebSocket subscriptions
        ) as any;
    }

    /**
     * Get bullish articles above a given sentiment threshold.
     */
    getBullishNews(threshold = 0.5, filterByWatchlist = false): Observable<NewsArticle[]> {
        return this.http.get<NewsArticle[]>(`/api/news/bullish?threshold=${threshold}&filterByWatchlist=${filterByWatchlist}`);
    }

    /**
     * Get aggregated market signals for trending tickers.
     */
    getMarketSignals(): Observable<TickerSignal[]> {
        return this.http.get<TickerSignal[]>('/api/market/signals');
    }

    /**
     * Get historical market data for chart
     */
    getHistoricalData(ticker: string): Observable<MarketData[]> {
        return this.http.get<MarketData[]>(`/api/market-data/${ticker}`);
    }

    /**
     * Get historical market data for multiple tickers (Batch API)
     * Implements session caching strategy.
     */
    getHistoricalDataBatch(tickers: string[]): Observable<{ [ticker: string]: MarketData[] }> {
        if (!tickers || tickers.length === 0) {
            return of({});
        }

        const now = Date.now();
        if (now - this.cacheTimestamp < this.CACHE_TTL_MS && Object.keys(this.historicalDataCache).length > 0) {
            // Check if all requested tickers are in cache
            const allCached = tickers.every(t => this.historicalDataCache[t]);
            if (allCached) {
                const result: { [ticker: string]: MarketData[] } = {};
                tickers.forEach(t => result[t] = this.historicalDataCache[t]);
                return of(result);
            }
        }

        return this.http.post<{ [ticker: string]: MarketData[] }>('/api/market-data/batch', tickers).pipe(
            tap(data => {
                this.historicalDataCache = { ...this.historicalDataCache, ...data };
                this.cacheTimestamp = Date.now();
            })
        );
    }

    /**
     * Watchlist API calls
     */
    getWatchlist(): Observable<any[]> {
        return this.http.get<any[]>('/api/watchlist');
    }

    addToWatchlist(ticker: string): Observable<any> {
        return this.http.post<any>(`/api/watchlist/${ticker}`, {});
    }

    removeFromWatchlist(ticker: string): Observable<any> {
        return this.http.delete<any>(`/api/watchlist/${ticker}`);
    }

    /**
     * Chat API
     */
    chat(message: string): Observable<any> {
        return this.http.post<any>('/api/chat', { message });
    }

    /**
     * Get sentiment color class for Angular Material theming.
     */
    getSentimentClass(score: number): string {
        if (score >= 0.7) return 'bullish';
        if (score <= -0.5) return 'bearish';
        return 'neutral';
    }

    /**
     * Format sentiment score for display.
     */
    formatSentiment(score: number): string {
        if (score >= 0.7) return `📈 Bullish (+${score.toFixed(2)})`;
        if (score <= -0.5) return `📉 Bearish (${score.toFixed(2)})`;
        return `➡️ Neutral (${score.toFixed(2)})`;
    }
}
