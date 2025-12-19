import sys
import os
import pymysql

# Add project root to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from backend.config import MYSQL_CONFIG

print(f"Testing connection to MySQL at {MYSQL_CONFIG['host']}:{MYSQL_CONFIG['port']}...")

try:
    conn = pymysql.connect(**MYSQL_CONFIG)
    print("Connection SUCCESSFUL!")
    
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM characters")
    count = cursor.fetchone()[0]
    print(f"Character count in MySQL: {count}")
    
    conn.close()
except Exception as e:
    print(f"Connection FAILED: {e}")
