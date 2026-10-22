"""Create a recoverable PostgreSQL snapshot before applying imported migrations.

Credentials stay in the subprocess environment; never print or store them in logs.
"""
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'backend'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
import django
django.setup()
from django.conf import settings

db = settings.DATABASES['default']
if db['ENGINE'] != 'django.db.backends.postgresql':
    raise SystemExit('Expected PostgreSQL; no database was changed.')
target = ROOT / '.integration-backups' / ('database-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '.backup')
target.parent.mkdir(exist_ok=True)
env = os.environ.copy()
env['PGPASSWORD'] = db['PASSWORD']
subprocess.run([
    r'C:\Program Files\PostgreSQL\18\bin\pg_dump.exe', '--format=custom',
    '--host', db['HOST'], '--port', str(db['PORT']), '--username', db['USER'],
    '--file', str(target), '--no-password', db['NAME'],
], env=env, check=True)
print('Database backup:', target)
print('Bytes:', target.stat().st_size)
