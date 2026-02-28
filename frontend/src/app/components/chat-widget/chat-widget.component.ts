import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalService } from '../../services/signal.service';

@Component({
    selector: 'app-chat-widget',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="chat-wrapper" [class.open]="isOpen">
      <button class="chat-toggle" (click)="toggleChat()" *ngIf="!isOpen">
        <span class="icon">💬</span> Ask AI
      </button>

      <div class="chat-window" *ngIf="isOpen">
        <div class="chat-header">
          <span>AlphaBot Assistant</span>
          <button class="close-btn" (click)="toggleChat()">✕</button>
        </div>
        
        <div class="chat-messages" #scrollMe>
          <div *ngFor="let msg of messages" class="message" [class.user]="msg.isUser">
            <div class="msg-bubble" [innerHTML]="formatMessage(msg.text)"></div>
          </div>
          <div *ngIf="loading" class="message assistant">
            <div class="msg-bubble typing">Thinking...</div>
          </div>
        </div>

        <div class="chat-input">
          <input type="text" [(ngModel)]="currentText" (keyup.enter)="sendMessage()" placeholder="Hỏi AI về mã chứng khoán..." />
          <button (click)="sendMessage()" [disabled]="!currentText.trim() || loading">➔</button>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .chat-wrapper { position: fixed; bottom: 20px; right: 20px; z-index: 1000; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; }
    .chat-toggle { background: #58a6ff; color: #fff; border: none; padding: 12px 20px; border-radius: 30px; font-weight: 600; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,0.4); display: flex; align-items: center; gap: 8px; transition: transform 0.2s; }
    .chat-toggle:hover { transform: scale(1.05); }
    
    .chat-window { width: 350px; height: 500px; background: #0d1117; border: 1px solid #30363d; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.5); }
    .chat-header { background: #161b22; padding: 14px 16px; border-bottom: 1px solid #30363d; display: flex; justify-content: space-between; align-items: center; color: #e6edf3; font-weight: 600; }
    .close-btn { background: none; border: none; color: #8b949e; cursor: pointer; font-size: 1.1rem; }
    .close-btn:hover { color: #f85149; }
    
    .chat-messages { flex: 1; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
    .message { display: flex; flex-direction: column; max-width: 85%; }
    .message.user { align-self: flex-end; }
    .message.assistant { align-self: flex-start; }
    
    .msg-bubble { padding: 10px 14px; border-radius: 12px; font-size: 0.9rem; line-height: 1.4; color: #e6edf3; white-space: pre-wrap; }
    .message.user .msg-bubble { background: #238636; border-bottom-right-radius: 2px; }
    .message.assistant .msg-bubble { background: #21262d; border: 1px solid #30363d; border-bottom-left-radius: 2px; }
    .typing { color: #8b949e; font-style: italic; }
    
    .chat-input { display: flex; padding: 12px; background: #161b22; border-top: 1px solid #30363d; }
    .chat-input input { flex: 1; background: #0d1117; border: 1px solid #30363d; color: #e6edf3; padding: 10px; border-radius: 6px; outline: none; }
    .chat-input input:focus { border-color: #58a6ff; }
    .chat-input button { background: #58a6ff; color: #000; border: none; margin-left: 8px; border-radius: 6px; width: 40px; cursor: pointer; font-weight: bold; }
    .chat-input button[disabled] { opacity: 0.5; cursor: not-allowed; }
  `]
})
export class ChatWidgetComponent {
    isOpen = false;
    currentText = '';
    loading = false;
    messages: { text: string, isUser: boolean }[] = [
        { text: 'Xin chào! Tôi có thể giúp gì cho bạn về theo dõi thị trường?', isUser: false }
    ];

    constructor(private signalService: SignalService) { }

    toggleChat() {
        this.isOpen = !this.isOpen;
    }

    sendMessage() {
        if (!this.currentText.trim()) return;

        const text = this.currentText;
        this.messages.push({ text, isUser: true });
        this.currentText = '';
        this.loading = true;

        // Scroll to bottom manually in real app by selecting the element or setTimeout

        this.signalService.chat(text).subscribe({
            next: (res) => {
                this.messages.push({ text: res.response, isUser: false });
                this.loading = false;
            },
            error: () => {
                this.messages.push({ text: 'Sorry, AI service is unavailable right now.', isUser: false });
                this.loading = false;
            }
        });
    }

    formatMessage(text: string): string {
        // Basic markdown parsing for bold and bullet points
        return text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/- (.*)/g, '<li>$1</li>');
    }
}
