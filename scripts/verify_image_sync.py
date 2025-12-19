import sys
import os
import time
import threading

# Add parent dir to path to find backend modules
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from backend.services.ImageService import ImageService
from backend.config import DB_FILE
from backend.app import create_app, db
from backend.models.Character import Character

def monitor_sync():
    print("Monitor started...")
    while ImageService.sync_status["running"]:
        status = ImageService.sync_status
        print(f"Status: {status['current']}/{status['total']} (Errors: {status['errors']})")
        time.sleep(1)
    print("Monitor finished.")

if __name__ == "__main__":
    print(f"Testing parallel image sync using DB: {DB_FILE}")
    
    app = create_app()
    with app.app_context():
        # Ensure DB is created
        db.create_all()
        
        # Check if we have characters, if not add a dummy one
        if Character.query.count() == 0:
            print("Adding dummy character for testing...")
            dummy = Character(
                id='harry-potter-test',
                name='Harry Potter Test',
                image='https://ik.imagekit.io/hpapi/harry.jpg', # Example valid URL
                slug='harry-potter-test'
            )
            db.session.add(dummy)
            db.session.commit()
            print("Dummy character added.")
        else:
            print(f"Found {Character.query.count()} characters in DB.")
            # Ensure at least one has no blob but has an image URL
            # Reset blob for testing if needed? 
            # Let's just reset the first one to force download
            first = Character.query.filter(Character.image != None).first()
            if first:
                first.image_blob = None
                db.session.commit()
                print(f"Reset image blob for {first.name} to force download.")

    # Reset status manually just in case
    ImageService.sync_status["running"] = True # Hack to let monitor start
    
    # Start monitor thread
    t_mon = threading.Thread(target=monitor_sync)
    t_mon.start()
    
    # Run sync (blocking in this script context, but originally intended for background)
    start_time = time.time()
    ImageService.cache_all_images_background()
    end_time = time.time()
    
    duration = end_time - start_time
    print(f"\nSync took {duration:.2f} seconds.")
    
    # Ensure monitor stops
    t_mon.join()
