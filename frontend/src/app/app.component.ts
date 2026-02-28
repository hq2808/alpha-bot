import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="shell">

      <!-- Top Navigation Bar -->
      <nav class="navbar">
        <div class="navbar-brand">
          📊 <span>Alpha Bot</span>
        </div>
        <div class="navbar-links">
          <a routerLink="/dashboard" routerLinkActive="active">
            🏠 Dashboard
          </a>
          <a routerLink="/intelligence" routerLinkActive="active">
            🎯 Intelligence
          </a>
          <a routerLink="/signals" routerLinkActive="active">
            📰 News Insight
          </a>
          <a routerLink="/settings" routerLinkActive="active">
            ⚙️ Settings
          </a>
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
      height: 56px;
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
  `],
})
export class AppComponent { }
