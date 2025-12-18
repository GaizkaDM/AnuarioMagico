import os
import logging
from logging.handlers import RotatingFileHandler

def setup_backend_logging():
    # Base directory for logs
    # Assume we are in z:\AnuarioMagico\backend
    # We want z:\AnuarioMagico\logs\backend\
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    log_dir = os.path.join(base_dir, 'logs', 'backend')
    
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)

    # Logger configuration
    logger = logging.getLogger('logger_backend')
    logger.setLevel(logging.DEBUG)  # Set to DEBUG to capture everything, handlers will filter
    
    # Prevent duplicate logging if setup is called multiple times
    if logger.handlers:
        return logger

    # Formatters
    detailed_formatter = logging.Formatter(
        '%(asctime)s - %(levelname)s - %(name)s - %(message)s'
    )

    # Max size and backup count
    max_bytes = 5 * 1024 * 1024  # 5 MB
    backup_count = 5

    # 1. backend-all.log (INFO and above)
    all_log_path = os.path.join(log_dir, 'backend-all.log')
    all_handler = RotatingFileHandler(all_log_path, maxBytes=max_bytes, backupCount=backup_count, encoding='utf-8')
    all_handler.setLevel(logging.DEBUG)
    all_handler.setFormatter(detailed_formatter)

    # 2. backend-error.log (ERROR and above)
    error_log_path = os.path.join(log_dir, 'backend-error.log')
    error_handler = RotatingFileHandler(error_log_path, maxBytes=max_bytes, backupCount=backup_count, encoding='utf-8')
    error_handler.setLevel(logging.ERROR)
    error_handler.setFormatter(detailed_formatter)

    # Console handler (for development)
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(detailed_formatter)

    # Add handlers to the logger
    logger.addHandler(all_handler)
    logger.addHandler(error_handler)
    logger.addHandler(console_handler)

    return logger

# Initialize logger instance
logger_backend = setup_backend_logging()
