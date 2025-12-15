from flask import Blueprint, jsonify
import threading
from backend.services.ImageService import ImageService
from backend.services.sync_mysql import sync_sqlite_to_mysql, sync_mysql_to_sqlite

admin_bp = Blueprint('admin', __name__)

@admin_bp.route('/admin/sync-images', methods=['POST'])
def trigger_image_sync():
    thread = threading.Thread(target=ImageService.cache_all_images_background)
    thread.daemon = True
    thread.start()
    return jsonify({"message": "Background image sync started", "status": "started"})

@admin_bp.route('/admin/sync-mysql', methods=['POST'])
def sync_mysql_push():
    try:
        sync_sqlite_to_mysql()
        return jsonify({"success": True, "message": "Push to MySQL completed sucessfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@admin_bp.route('/admin/sync-pull', methods=['POST'])
def sync_mysql_pull():
    try:
        sync_mysql_to_sqlite()
        return jsonify({"success": True, "message": "Pull from MySQL completed successfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500
