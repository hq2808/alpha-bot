import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface AppSettings {
  groqApiKey: string;
  telegramBotToken: string;
  telegramChatId: string;
  sentimentThreshold: number;
  aiProvider: 'groq' | 'ollama';
  ollamaUrl: string;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="settings-page">
      <h1>⚙️ Settings</h1>
      <p class="subtitle">Cấu hình AI provider, Telegram alert và ngưỡng tín hiệu.</p>

      <div class="sections">

        <!-- AI Settings -->
        <section class="section">
          <h2>🤖 AI Provider</h2>
          <div class="field">
            <label>Provider</label>
            <select [(ngModel)]="settings.aiProvider">
              <option value="groq">Groq (Cloud — cần API key)</option>
              <option value="ollama">Ollama (Local — không cần key)</option>
            </select>
          </div>

          @if (settings.aiProvider === 'groq') {
            <div class="field">
              <label>Groq API Key</label>
              <input type="password" [(ngModel)]="settings.groqApiKey"
                     placeholder="gsk_..." autocomplete="off" />
              <small>Lấy tại <a href="https://console.groq.com" target="_blank">console.groq.com</a> (miễn phí)</small>
            </div>
          }

          @if (settings.aiProvider === 'ollama') {
            <div class="field">
              <label>Ollama URL</label>
              <input type="text" [(ngModel)]="settings.ollamaUrl"
                     placeholder="http://localhost:11434" />
              <small>Mặc định nếu bạn chạy Ollama trên máy local.</small>
            </div>
          }
        </section>

        <!-- Telegram Settings -->
        <section class="section">
          <h2>📱 Telegram Alerts</h2>
          <div class="field">
            <label>Bot Token</label>
            <input type="password" [(ngModel)]="settings.telegramBotToken"
                   placeholder="1234567890:AAH..." autocomplete="off" />
            <small>Tạo bot tại <a href="https://t.me/BotFather" target="_blank">&#64;BotFather</a></small>
          </div>
          <div class="field">
            <label>Chat ID</label>
            <input type="text" [(ngModel)]="settings.telegramChatId"
                   placeholder="-100123456789" />
            <small>Dùng <a href="https://t.me/userinfobot" target="_blank">&#64;userinfobot</a> để lấy Chat ID của bạn.</small>
          </div>
        </section>

        <!-- Alert Threshold -->
        <section class="section">
          <h2>🎯 Alert Sensitivity</h2>
          <div class="field">
            <label>Sentiment threshold: <strong>{{ settings.sentimentThreshold | number:'1.2-2' }}</strong></label>
            <input type="range" min="0.3" max="0.95" step="0.05"
                   [(ngModel)]="settings.sentimentThreshold" />
            <small>
              Chỉ gửi Telegram khi điểm AI ≥ ngưỡng này.
              (0.5 = Balanced, 0.7 = Bullish, 0.9 = Very Strong)
            </small>
          </div>
        </section>

      </div>

      <!-- Save -->
      <div class="actions">
        <button class="btn-save" (click)="save()">💾 Lưu cấu hình</button>
        @if (saved) {
          <span class="saved-msg">✅ Đã lưu!</span>
        }
      </div>

    </div>
  `,
  styles: [`
    .settings-page { max-width: 680px; }
    h1 { color: #e6edf3; font-size: 1.4rem; margin-bottom: 4px; }
    .subtitle { color: #8b949e; font-size: 0.9rem; margin-bottom: 32px; }

    .sections { display: flex; flex-direction: column; gap: 24px; }

    .section {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 20px 24px;
    }

    h2 { font-size: 1rem; color: #e6edf3; margin: 0 0 16px; }

    .field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
    .field:last-child { margin-bottom: 0; }
    .field label { font-size: 0.85rem; color: #8b949e; }

    .field input[type=text],
    .field input[type=password],
    .field select {
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 6px;
      color: #e6edf3;
      padding: 8px 12px;
      font-size: 0.9rem;
      outline: none;
      transition: border-color 0.15s;
    }

    .field input:focus,
    .field select:focus { border-color: #58a6ff; }

    .field input[type=range] { accent-color: #3fb950; }

    .field small { font-size: 0.78rem; color: #6e7681; }
    .field small a { color: #58a6ff; }

    .actions { margin-top: 24px; display: flex; align-items: center; gap: 16px; }

    .btn-save {
      background: #238636;
      color: white;
      border: none;
      border-radius: 6px;
      padding: 10px 24px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s;
    }
    .btn-save:hover { background: #2ea043; }

    .saved-msg { color: #3fb950; font-size: 0.9rem; }
  `]
})
export class SettingsComponent {
  settings: AppSettings = {
    groqApiKey: '',
    telegramBotToken: '',
    telegramChatId: '',
    sentimentThreshold: 0.7,
    aiProvider: 'groq',
    ollamaUrl: 'http://localhost:11434',
  };
  saved = false;

  constructor(private http: HttpClient) {
    // Load from localStorage on start
    const stored = localStorage.getItem('alpha-bot-settings');
    if (stored) this.settings = { ...this.settings, ...JSON.parse(stored) };
  }

  save(): void {
    // Save to localStorage (in production: send to backend API)
    localStorage.setItem('alpha-bot-settings', JSON.stringify(this.settings));
    this.saved = true;
    setTimeout(() => this.saved = false, 3000);
  }
}
