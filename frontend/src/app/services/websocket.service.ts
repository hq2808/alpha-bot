import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { VndPriceData } from '../pages/price-board/price-board.component';

@Injectable({
    providedIn: 'root'
})
export class WebsocketService implements OnDestroy {
    private rxStomp: RxStomp;

    constructor() {
        this.rxStomp = new RxStomp();

        const rxStompConfig: RxStompConfig = {
            brokerURL: `ws://${window.location.hostname}:8080/ws`,

            // Headers
            // connectHeaders: {
            //   login: 'guest',
            //   passcode: 'guest',
            // },

            // How often to heartbeat?
            heartbeatIncoming: 0, // Typical value 0 - disabled
            heartbeatOutgoing: 20000, // Typical value 20000 - every 20 seconds

            // Wait in milliseconds before attempting auto reconnect
            reconnectDelay: 5000,

            // Will log diagnostics on console
            debug: (msg: string): void => {
                // console.log(new Date(), msg);
            },
        };

        this.rxStomp.configure(rxStompConfig);
        this.rxStomp.activate();
    }

    /**
     * Listen to market ticks from the backend
     * Returns an Observable of array of VndPriceData (mapped from StockQuote)
     */
    public getMarketTicks(): Observable<VndPriceData[]> {
        return this.rxStomp.watch('/topic/market-ticks').pipe(
            map(message => {
                // Parse the JSON array
                const dataArr = JSON.parse(message.body);

                // Map backend StockQuote structure to frontend VndPriceData structure
                return dataArr.map((d: any) => ({
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
            })
        );
    }

    ngOnDestroy() {
        this.rxStomp.deactivate();
    }
}
