import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChatWidgetComponent } from './components/chat-widget/chat-widget.component';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ChatWidgetComponent],
  template: `
    <div class="shell">
 
       <!-- Top Navigation Bar -->
       <nav class="navbar">
         <div class="navbar-brand">
           📊 <span>Alpha Bot</span>
         </div>
         <div class="navbar-links">
           <a routerLink="/dashboard" routerLinkActive="active" *ngIf="authService.isLoggedIn()">
             📊 Tổng quan
           </a>
           <a routerLink="/price-board" routerLinkActive="active">
             📈 Bảng giá
           </a>
           <a routerLink="/intelligence" routerLinkActive="active">
             🧠 Phân tích
           </a>
           <a routerLink="/signals" routerLinkActive="active">
             📰 Tin tức
           </a>
           <a routerLink="/portfolio" routerLinkActive="active" *ngIf="authService.isLoggedIn()">
             💰 Paper Trading
           </a>
           <a routerLink="/test-trading" routerLinkActive="active" *ngIf="authService.isLoggedIn()">
             🛡️ Test Trading
           </a>
         </div>
         <div class="navbar-auth">
            <button *ngIf="!authService.isLoggedIn()" (click)="login()" class="auth-btn login">Login</button>
            <button *ngIf="authService.isLoggedIn()" (click)="logout()" class="auth-btn logout">Logout</button>
         </div>
         <div class="navbar-status">
           <span class="status-dot" [class.connected]="true"></span>
           Live
         </div>
       </nav>
 
       <!-- Page Content -->
       <main class="content">
         <router-outlet />
       </main>
 
       <app-chat-widget></app-chat-widget>
     </div>
   `,
  styles: [`
     :host {
       display: block;
       min-height: 100vh;
       background: #0d1117;
       color: #e6edf3;
       font-family: 'Inter', -apple-system, sans-serif;
     }
 
     .shell {
       display: flex;
       flex-direction: column;
       min-height: 100vh;
     }
 
     /* ---- Navbar ---- */
     .navbar {
       display: flex;
       align-items: center;
       gap: 24px;
       padding: 0 24px;
       height: 64px;
       background: #161b22;
       border-bottom: 1px solid #30363d;
       position: sticky;
       top: 0;
       z-index: 100;
     }
 
     .navbar-brand {
       font-size: 1.1rem;
       font-weight: 700;
       color: #58a6ff;
       display: flex;
       align-items: center;
       gap: 8px;
       min-width: 140px;
     }
 
     .navbar-links {
       display: flex;
       gap: 4px;
       flex: 1;
     }
 
     .navbar-links a {
       padding: 6px 14px;
       border-radius: 6px;
       text-decoration: none;
       color: #8b949e;
       font-size: 0.9rem;
       font-weight: 500;
       transition: all 0.15s ease;
     }
 
     .navbar-links a:hover {
       color: #e6edf3;
       background: #21262d;
     }
 
     .navbar-links a.active {
       color: #e6edf3;
       background: #21262d;
     }

     .navbar-auth {
        display: flex;
        gap: 8px;
     }

     .auth-btn {
        padding: 6px 16px;
        border-radius: 6px;
        border: 1px solid #30363d;
        background: #21262d;
        color: #e6edf3;
        font-size: 0.85rem;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
     }

     .auth-btn:hover {
        background: #30363d;
        border-color: #8b949e;
     }

     .auth-btn.login {
        background: #238636;
        border-color: rgba(240,246,252,0.1);
     }
     .auth-btn.login:hover {
        background: #2ea043;
     }
 
     .navbar-status {
       display: flex;
       align-items: center;
       gap: 6px;
       font-size: 0.8rem;
       color: #8b949e;
     }
 
     .status-dot {
       width: 8px;
       height: 8px;
       border-radius: 50%;
       background: #6e7681;
     }
 
     .status-dot.connected {
       background: #3fb950;
       box-shadow: 0 0 6px #3fb950;
       animation: pulse 2s infinite;
     }
 
     @keyframes pulse {
       0%, 100% { opacity: 1; }
       50% { opacity: 0.5; }
     }
 
     /* ---- Content ---- */
     .content {
       flex: 1;
       padding: 24px;
       max-width: 1400px;
       width: 100%;
       margin: 0 auto;
     }
 
     /* ---- Responsive Design ---- */
     @media (max-width: 1024px) {
       .navbar {
         flex-wrap: wrap;
         height: auto;
         padding: 12px 16px;
         gap: 12px;
       }
       .navbar-brand {
         flex: 1 1 auto;
       }
       .navbar-links {
         order: 3;
         width: 100%;
         overflow-x: auto;
         padding-bottom: 4px;
         -webkit-overflow-scrolling: touch;
       }
       .navbar-auth {
         order: 2;
       }
       .navbar-status {
         order: 4;
         width: 100%;
         justify-content: flex-end;
         margin-top: -8px;
       }
       .content {
         padding: 16px;
       }
     }
   `],
})
export class AppComponent implements OnInit {
  authService = inject(AuthService);
  private http = inject(HttpClient);
  // private route = inject(ActivatedRoute); // Not needed
  // private router = inject(Router); // Not needed

  ngOnInit() {
    // Robustly extract token before Angular Router processes it
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (token) {
      this.authService.login(token); // Synchronously save to localStorage
      // Remove token from browser history completely
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    this.checkSession();
  }

  checkSession() {
    // Check if we have a session/token on load
    if (!this.authService.getToken()) {
      this.authService.isLoggedIn.set(false);
      return;
    }

    this.http.get('/api/user/me').subscribe({
      next: (user: any) => {
        if (user) {
          this.authService.isLoggedIn.set(true);
        }
      },
      error: () => {
        // Token is invalid/expired
        this.authService.logout();
        this.authService.isLoggedIn.set(false);
      }
    });
  }

  login() {
    // Redirect to backend OAuth2 authorization endpoint using relative path to inherit current host/port
    window.location.href = '/oauth2/authorization/google';
  }

  logout() {
    this.authService.logout();
  }
}
