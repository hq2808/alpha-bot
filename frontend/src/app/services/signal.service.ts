import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';

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
    getLatestNews(): Observable<NewsArticle[]> {
        return this.latestNews$;
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
    getBullishNews(threshold = 0.5): Observable<NewsArticle[]> {
        return this.http.get<NewsArticle[]>(`/api/news/bullish?threshold=${threshold}`);
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
