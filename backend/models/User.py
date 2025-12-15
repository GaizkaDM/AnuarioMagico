from backend.extensions import db

class User(db.Model):
    __tablename__ = 'users'
    username = db.Column(db.String(255), primary_key=True)
    password_hash = db.Column(db.String(255))
    created_at = db.Column(db.String(100))
