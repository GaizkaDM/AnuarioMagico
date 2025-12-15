import requests
import time
import sys

print("Checking backend health...")
for i in range(10):
    try:
        response = requests.get('http://localhost:8000/health', timeout=2)
        if response.status_code == 200:
            print(f"Backend is UP: {response.json()}")
            sys.exit(0)
    except requests.exceptions.ConnectionError:
        print(f"Waiting for backend... ({i+1}/10)")
        time.sleep(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

print("Backend timed out.")
sys.exit(1)
