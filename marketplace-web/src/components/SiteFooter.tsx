import { Link } from "react-router-dom";

export function SiteFooter({ listBusinessLabel }: { listBusinessLabel: string }) {
  return (
    <footer>
      <div>
        <Link className="brand" to="/">
          neighbourly<span className="brand-dot">.</span>
        </Link>
        <p>Your neighbourhood, all in one place.</p>
      </div>
      <div className="footer-links">
        <Link to="/shops">Explore Shops</Link>
        <Link to="/wholesalers">Explore Wholesaler</Link>
        <Link to="/pricing">Pricing</Link>
        <Link to="/partners/apply">Become a local partner</Link>
        <a href="#list-business">{listBusinessLabel}</a>
        <span>© {new Date().getFullYear()} Neighbourly</span>
      </div>
    </footer>
  );
}
