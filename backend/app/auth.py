from datetime import datetime, timedelta, timezone
import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from .config import settings

bearer = HTTPBearer(auto_error=False)

def create_dev_token(subject: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {"sub": subject, "iss": settings.jwt_issuer, "iat": now, "exp": now + timedelta(hours=1)}
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")

def require_user(credentials: HTTPAuthorizationCredentials = Depends(bearer)):
    if credentials is None:
        raise HTTPException(status_code=401, detail="Bearer token required")
    try:
        return jwt.decode(
            credentials.credentials,
            settings.jwt_secret,
            algorithms=["HS256"],
            issuer=settings.jwt_issuer,
        )
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
