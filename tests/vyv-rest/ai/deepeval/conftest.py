import sys
from pathlib import Path

# Que pytest encuentre el paquete `src` desde la raíz `deepeval/`
sys.path.insert(0, str(Path(__file__).parent))
