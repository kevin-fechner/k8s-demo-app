import {ChangeDetectionStrategy, Component} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="navbar-brand">
        <span>Demo App</span>
      </div>
      <div class="navbar-links">
        <a routerLink="/products"
           routerLinkActive="active"
           [routerLinkActiveOptions]="{exact: false}">
          Products
        </a>
        <a routerLink="/orders"
           routerLinkActive="active"
           [routerLinkActiveOptions]="{exact: false}">
          Orders
        </a>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex; align-items: center;
      justify-content: space-between;
      padding: 1rem 2rem;
      background: #1a1a2e; color: white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    }
    .navbar-brand span { font-size: 1.4rem; font-weight: bold; }
    .navbar-links { display: flex; gap: 2rem; }
    .navbar-links a {
      color: #ccc; text-decoration: none;
      font-size: 1rem; padding: 0.4rem 0.8rem;
      border-radius: 4px; transition: all 0.2s;
    }
    .navbar-links a:hover, .navbar-links a.active {
      color: white; background: #16213e;
    }
  `]
})
export class NavbarComponent {}
