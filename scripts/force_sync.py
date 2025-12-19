import sys
import os
import logging

# Add project root to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# Configure logging to console specifically for this script
logging.basicConfig(level=logging.DEBUG)

from backend.services.sync_mysql import sync_sqlite_to_mysql

print("Forcing sync execution...")
try:
    sync_sqlite_to_mysql()
    print("Sync execution finished.")
except Exception as e:
    print(f"Sync execution failed with exception: {e}")
