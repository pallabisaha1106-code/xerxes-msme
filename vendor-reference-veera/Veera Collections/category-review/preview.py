"""Local review server; does not publish or change production configuration."""
from http.server import SimpleHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
from functools import partial
import sys
class PreviewHandler(SimpleHTTPRequestHandler):
 extensions_map={**SimpleHTTPRequestHandler.extensions_map,'.webp':'image/webp','.css':'text/css','.js':'text/javascript'}
 def log_message(self,*args):pass
root=Path(__file__).resolve().parents[1]
port=int(sys.argv[1]) if len(sys.argv)>1 else 8766
print(f'Local review: http://127.0.0.1:{port}/more.html',flush=True)
ThreadingHTTPServer(('127.0.0.1',port),partial(PreviewHandler,directory=str(root))).serve_forever()
