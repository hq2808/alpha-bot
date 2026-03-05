import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly TOKEN_KEY = 'auth_token';

    // Use a signal for reactive state management
    isLoggedIn = signal<boolean>(this.hasToken());

    private hasToken(): boolean {
        return !!localStorage.getItem(this.TOKEN_KEY);
    }

    getToken(): string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    login(token: string) {
        localStorage.setItem(this.TOKEN_KEY, token);
        this.isLoggedIn.set(true);
    }

    logout() {
        localStorage.removeItem(this.TOKEN_KEY);
        this.isLoggedIn.set(false);
    }
}
