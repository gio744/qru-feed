from fastapi import Depends, HTTPException
from .auth import require_user
from .db import connection

def require_roles(*allowed):
    def dependency(user=Depends(require_user)):
        subject=user["sub"]
        with connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""SELECT r.id FROM users u
                    JOIN user_roles ur ON ur.user_id=u.id
                    JOIN roles r ON r.id=ur.role_id
                    WHERE u.external_subject=%s AND u.active=true""",(subject,))
                roles={r[0] for r in cur.fetchall()}
        if not roles.intersection(set(allowed)):
            raise HTTPException(status_code=403,detail="Insufficient role")
        return {"subject":subject,"roles":sorted(roles)}
    return dependency
