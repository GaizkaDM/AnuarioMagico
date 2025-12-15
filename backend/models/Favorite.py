from backend.extensions import db

class Favorite(db.Model):
    __tablename__ = 'favorites'
    character_id = db.Column(db.String(255), primary_key=True)
    is_favorite = db.Column(db.Boolean, default=False)
